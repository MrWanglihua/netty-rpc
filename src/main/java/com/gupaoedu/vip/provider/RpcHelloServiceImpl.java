package com.gupaoedu.vip.provider;

import com.gupaoedu.vip.api.IRpcHelloService;

public class RpcHelloServiceImpl implements IRpcHelloService {
    @Override
    public String hello(String name) {
        return "Hello"+name+"!";
    }
}
