package com.gupaoedu.vip.consumer;

import com.gupaoedu.vip.api.IRpcHelloService;
import com.gupaoedu.vip.api.IRpcService;
import com.gupaoedu.vip.provider.RpcHelloServiceImpl;
import com.gupaoedu.vip.provider.RpcServiceImpl;

public class RpcConsumer {

    public static void main(String[] args) {
        IRpcHelloService iRpcHelloService = RpcProxy.create(IRpcHelloService.class);
        System.out.println(iRpcHelloService.hello("Tom"));

        IRpcService service =  RpcProxy.create(IRpcService.class);
        System.out.println("8 + 2 =" + service.add(8,2));
        System.out.println("8 - 2 =" + service.sub(8,2));
        System.out.println("8 * 2 =" + service.mult(8,2));
        System.out.println("8 / 2 =" + service.div(8,2));
    }

}
