package com.gupaoedu.vip.work4;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;

public class Demo1 {
    public static void main(String[] args) {

//        用例一，使用内存池分配器创建直接内存缓冲区：
        final byte[] CONTENT = new byte[1024];
        int loop = 1800000;
        long startTime = System.currentTimeMillis();
        ByteBuf poolBuffer = null;
        for (int i = 0; i < loop; i++) {
            poolBuffer = PooledByteBufAllocator.DEFAULT.directBuffer(1024);
            poolBuffer.writeBytes(CONTENT);
            poolBuffer.release();
        }
        long endTime = System.currentTimeMillis();
        System.out.println("内存池分配缓冲区耗时" + (endTime - startTime) + "ms.");

//      用例二，使用非堆内存分配器创建的直接内存缓冲区：
        long startTime2 = System.currentTimeMillis();
        ByteBuf buffer = null;
        for (int i = 0; i < loop; i++) {
            buffer = Unpooled.directBuffer(1024);
            buffer.writeBytes(CONTENT);
        }
        endTime = System.currentTimeMillis();
        System.out.println("非内存池分配缓冲区耗时" + (endTime - startTime2) + "ms.");
    }
}
