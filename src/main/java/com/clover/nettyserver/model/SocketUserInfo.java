package com.clover.nettyserver.model;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

public class SocketUserInfo {

    private int userId;

    private String ip;

    private ChannelHandlerContext channelHandlerContext;

    private Channel channel;


    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public ChannelHandlerContext getChannelHandlerContext() {
        return channelHandlerContext;
    }

    public void setChannelHandlerContext(ChannelHandlerContext channelHandlerContext) {
        this.channelHandlerContext = channelHandlerContext;
    }
}
