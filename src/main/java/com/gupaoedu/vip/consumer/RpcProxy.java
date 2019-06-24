package com.gupaoedu.vip.consumer;

import com.gupaoedu.vip.protocol.InvokerProtocol;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.AbstractNioByteChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class RpcProxy {

    public static <T>T create(Class<?> clazz){

        MethodProxy methodProxy = new MethodProxy(clazz);
        T result = (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, methodProxy);

        return result;
    }

    private static class MethodProxy implements InvocationHandler{

        private Class<?> clazz;
        public MethodProxy(Class<?> clazz) {
            this.clazz = clazz;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if(Object.class.equals(method.getDeclaringClass())){
                return method.invoke(this,args);
            }else {
                return rpcInvoke(proxy,method,args);
            }
        }

        private Object rpcInvoke(Object proxy, Method method, Object[] args) {

//            1、先构造一个协议内容，即消息
            InvokerProtocol msg = new InvokerProtocol();
            msg.setClassName(this.clazz.getName());
            msg.setMethodName(method.getName());
            msg.setParams(method.getParameterTypes());
            msg.setValues(args);

           final RpcProxyHandler handler =  new RpcProxyHandler();

//            发起网络请求
            EventLoopGroup workGroup = new NioEventLoopGroup();
            try {
                Bootstrap client = new Bootstrap();
                client.group(workGroup)
                        .channel(NioSocketChannel.class)
                        .option(ChannelOption.TCP_NODELAY, true)
                        .handler(new ChannelInitializer<SocketChannel>() {

                            @Override
                            protected void initChannel(SocketChannel ch) throws Exception {

                                ChannelPipeline pipeline = ch.pipeline();
//                即对我们处理逻辑的封装
//                对我们自定义的内容进行编、解码
                                pipeline.addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));
                                pipeline.addLast(new LengthFieldPrepender(4));
//                实参的处理
                                pipeline.addLast("encoder", new ObjectEncoder());
                                pipeline.addLast("decoder", new ObjectDecoder(Integer.MAX_VALUE, ClassResolvers.cacheDisabled(null)));

//                执行属于自己的逻辑
                                pipeline.addLast(handler);
                            }
                        });

                ChannelFuture future = client.connect("localhost", 8080).sync();
                future.channel().writeAndFlush(msg).sync();
                future.channel().closeFuture().sync();

            }catch (Exception e){
                e.printStackTrace();
            }finally {
                workGroup.shutdownGracefully();
            }
            return handler.getResponse();
        }
    }

}
