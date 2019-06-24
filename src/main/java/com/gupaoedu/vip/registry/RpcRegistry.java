package com.gupaoedu.vip.registry;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;


public class RpcRegistry {
    private int port;

    public RpcRegistry(int port) {
        this.port = port;
    }

    public void start(){
//        初始化一个主线程及一个字线程
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {


            ServerBootstrap server = new ServerBootstrap();
            server.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)//由它来轮询bossGroup 中所有的key
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
//                在Netty中把所有的业务逻辑处理全部归总到了一个队列中
//                这个队列中包含了各种各样的处理逻辑，对这些处理逻辑在Netty中封装了一个对象
//                而，该对象就是，一个无锁化穿行任务队列——pipline
                            ChannelPipeline pipeline = ch.pipeline();
//                即对我们处理逻辑的封装
//                对我们自定义的内容进行编、解码
                            pipeline.addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));
                            pipeline.addLast(new LengthFieldPrepender(4));
//                实参的处理
                            pipeline.addLast("encoder", new ObjectEncoder());
                            pipeline.addLast("decoder", new ObjectDecoder(Integer.MAX_VALUE,
                                    ClassResolvers.cacheDisabled(null)));

//                执行属于自己的逻辑
                            pipeline.addLast(new RegistryHandler());
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);//保证每个子线程在线程池中可以回收利用的

//          正式启动服务，相当于用一个死循环开始轮询
            ChannelFuture future = server.bind(port).sync();//        future ,返回一个线程的异步回调的东西
            System.out.println("GP  RPC Registry start listen at "+this.port);
            future.channel().closeFuture().sync();
        }catch (Exception e){
            bossGroup.shutdownGracefully();
            e.printStackTrace();
        }


    }

    public static void main(String[] args) {
        new RpcRegistry(8080).start();
    }
}
