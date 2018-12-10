package com.clover.nettyserver.netty.tcp;


import com.clover.nettyserver.model.SocketUserInfo;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.util.List;

public class TcpUtils {


    public static boolean sendTcpMsg(String data, int userId) {

        List<SocketUserInfo> socketUserInfoList = NettyTcpServer.getSocketUserInfoList();

        SocketUserInfo userInfo = null;
        for (SocketUserInfo socketUserInfo:socketUserInfoList){
            if (socketUserInfo.getUserId() == userId){
                userInfo = socketUserInfo;
            }
        }

        if (userInfo == null){
            return false;
        }
        Channel channel = userInfo.getChannelHandlerContext().channel();
        if (channel == null) {
            return false;
        }
        byte [] udpcommand = data.getBytes();

//        ByteBuf byteBuf = Unpooled.copiedBuffer(udpcommand);
        try {
//            channel.write(byteBuf).sync();
//            channel.flush();
            channel.writeAndFlush(udpcommand).sync();
            byte [] end = new byte[]{0x7e};
            channel.writeAndFlush(end).sync();
            return true;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return true;
    }

    public static boolean run() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                NettyTcpServer.run();
            }
        }).start();
        return true;
    }

}
