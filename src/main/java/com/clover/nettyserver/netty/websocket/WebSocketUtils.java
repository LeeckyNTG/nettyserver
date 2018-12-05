package com.clover.nettyserver.netty.websocket;


import com.clover.nettyserver.model.SocketUserInfo;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.util.List;

public class WebSocketUtils {


    public static boolean sendMsg(String data, int userId) {

        List<SocketUserInfo> socketUserInfoList = NettyWebSocketServer.getSocketUserInfoList();

        SocketUserInfo userInfo = null;
        for (SocketUserInfo socketUserInfo:socketUserInfoList){
            if (socketUserInfo.getUserId() == userId){
                userInfo = socketUserInfo;
            }
        }

        if (userInfo == null){
            return false;
        }
        ChannelHandlerContext ctx = userInfo.getChannelHandlerContext();
        if (ctx == null) {
            return false;
        }
        ctx.channel().write(new TextWebSocketFrame(data));
        ctx.flush();
        return true;
    }

    public static boolean run() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                NettyWebSocketServer.run();
            }
        }).start();
        return true;
    }

}
