package com.gupaoedu.vip.registry;

import com.gupaoedu.vip.protocol.InvokerProtocol;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RegistryHandler extends ChannelInboundHandlerAdapter {

    private List<String> classNames = new ArrayList<>();

    private Map<String, Object> registryMap = new ConcurrentHashMap<>();

    /**
     * 思路
     * 1、根据一个包名，将所有符合条件的class全部扫描出来，放到一个容器中
     * 2、给每一个对应的class取一个唯一的名字，作为服务名称，保存的容器中
     * 3、当有客户端过来时，就会获取协议内容InvokerProtocol的对象
     * 4、去注册好的容器中找到符合条件的服务
     * 5、通过远程调用provider的到返回结果，并回复给客户端
     */

    public RegistryHandler() {
        scannerClass("com.gupaoedu.vip.provider");

        doRegistry();
    }

    /**
     * 1、根据一个包名，将所有符合条件的class全部扫描出来，放到一个容器中
     *
     * @param packageName
     */
    private void scannerClass(String packageName) {
        URL url = this.getClass().getClassLoader().getResource(packageName.replaceAll("\\.", "/"));
        File classPath = new File(url.getFile());
        for (File file : classPath.listFiles()) {

            if (file.isDirectory()) {
                scannerClass(packageName + "." + file.getName());
            } else {
                classNames.add(packageName + "." + file.getName().replaceAll(".class", ""));
            }
        }


    }

    /**
     * 2、给每一个对应的class取一个唯一的名字，作为服务名称，保存的容器中
     */
    private void doRegistry() {
        if (classNames.isEmpty()) {
            return;
        }
        for (String className : classNames) {
            try {

                Class<?> clazz = Class.forName(className);
                Class<?> i = clazz.getInterfaces()[0];
                String serviceName = i.getName();
                registryMap.put(serviceName,clazz.newInstance());

            } catch (Exception e) {
                e.printStackTrace();
            }

        }


    }

    /**
     * 有客户端连接的时候回调
     *
     * @param ctx
     * @param msg
     * @throws Exception
     */
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Object result = new Object();
        InvokerProtocol request = (InvokerProtocol) msg;

        if(registryMap.containsKey(request.getClassName())){
            Object service = registryMap.get(request.getClassName());
            Method method = service.getClass().getMethod(request.getMethodName(),request.getParams());

            result = method.invoke(service,request.getValues());

        }

        ctx.write(result);
        ctx.flush();
        ctx.close();

    }

    /**
     * 连接发送异常的时候，自动回调
     *
     * @param ctx
     * @param cause
     * @throws Exception
     */
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
    }
}
