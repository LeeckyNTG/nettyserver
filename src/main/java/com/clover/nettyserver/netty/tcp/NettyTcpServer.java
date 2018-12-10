package com.clover.nettyserver.netty.tcp;

import com.alibaba.fastjson.JSONObject;
import com.clover.nettyserver.model.BaseMsg;
import com.clover.nettyserver.model.SocketUserInfo;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;


public class NettyTcpServer {

    private static int tcp_port = 8082;

    private static Channel channel;
    private static SocketChannel socketChannel;
    private static ChannelFuture channelFuture;

    private static ChannelHandlerContext channelHandlerContext;

    private static List<SocketUserInfo> socketUserInfoList = new ArrayList<>();

    public static boolean run() {

        //服务器运行状态
        boolean isRunning = false;
        //处理Accept连接事件的线程，这里线程数设置为1即可，netty处理链接事件默认为单线程，过度设置反而浪费cpu资源
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        //处理hadnler的工作线程，其实也就是处理IO读写 。线程数据默认为 CPU 核心数乘以2
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        //创建ServerBootstrap实例
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        //初始化ServerBootstrap的线程组
        serverBootstrap.group(workerGroup, workerGroup);//
        //设置将要被实例化的ServerChannel类
        serverBootstrap.channel(NioServerSocketChannel.class);//

        try {
            //在ServerChannelInitializer中初始化ChannelPipeline责任链，并添加到serverBootstrap中
            serverBootstrap.childHandler(new ServerChannelInitializer());
            //标识当服务器请求处理线程全满时，用于临时存放已完成三次握手的请求的队列的最大长度
            serverBootstrap.option(ChannelOption.SO_BACKLOG, 1024);
            // 是否启用心跳保活机机制
            serverBootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
            //绑定端口后，开启监听
            channelFuture = serverBootstrap.bind(tcp_port).sync().channel().closeFuture().sync();
            channel = channelFuture.channel();
            return true;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    @ChannelHandler.Sharable
    public static class ServerChannelInitializer extends ChannelInitializer<SocketChannel> {
        final EventExecutorGroup group = new DefaultEventExecutorGroup(2);

        public ServerChannelInitializer() throws InterruptedException {
        }

        @Override
        protected void initChannel(SocketChannel socketChannel) throws Exception {

            System.out.println(socketChannel.remoteAddress());


            channel = socketChannel;

            ChannelPipeline pipeline = socketChannel.pipeline();
            //IdleStateHandler心跳机制,如果超时触发Handle中userEventTrigger()方法
            pipeline.addLast("idleStateHandler",
                    new IdleStateHandler(15, 0, 0, TimeUnit.MINUTES));
            // netty基于分割符的自带解码器，根据提供的分隔符解析报文，这里是0x7e;1024表示单条消息的最大长度，解码器在查找分隔符的时候，达到该长度还没找到的话会抛异常
            pipeline.addLast(
                    new DelimiterBasedFrameDecoder(1024, Unpooled.copiedBuffer(new byte[]{0x7e}),
                            Unpooled.copiedBuffer(new byte[]{0x7e})));
            //自定义编解码器
            pipeline.addLast(
                    new MessagePacketDecoder(),
                    new MessagePacketEncoder()
            );

            //自定义Hadler
            pipeline.addLast("handler", new TCPServerHandler());

        }
    }

    public static class TCPServerHandler extends ChannelInboundHandlerAdapter {


        public TCPServerHandler() {
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            //拿到传过来的msg数据，开始处理
            byte[] alldata = (byte [])msg;
            String request = new String(alldata, "UTF-8");
            System.out.println(request);
            BaseMsg baseMsg = JSONObject.parseObject(request, BaseMsg.class);
            if (baseMsg.getType().equals("connect")) {
                SocketUserInfo socketUserInfo = JSONObject.parseObject(baseMsg.getData(), SocketUserInfo.class);;
                boolean bol = false;
                int position = -1;
                for (int i = 0; i < socketUserInfoList.size(); i++) {
                    if (socketUserInfo.getUserId() == socketUserInfoList.get(i).getUserId()) {
                        bol = true;
                        position = i;;
                        break;
                    }
                }
                if (bol) {
                    socketUserInfoList.remove(position);
                }
                socketUserInfo.setChannelHandlerContext(ctx);

                SocketAddress socketAddress = ctx.channel().remoteAddress();

                String ip = socketAddress.toString().split(":")[0].substring(1);
                socketUserInfo.setIp(ip);
                socketUserInfoList.add(socketUserInfo);

                //发送心跳包
                new java.util.Timer().schedule(new TimerTask() {

                    @Override
                    public void run() {
                        if (ctx != null) {
                            BaseMsg baseMsg = new BaseMsg();
                            baseMsg.setType("HeartBeat");
                            baseMsg.setData("服务器发送的心跳包！");
                            byte[] udpcommand = JSONObject.toJSONString(baseMsg).getBytes();
                            try {
                                channel.writeAndFlush(udpcommand).sync();
                                byte [] end = new byte[]{0x7e};;
                                channel.writeAndFlush(end).sync();


                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }, 3000, 3000);
            }
        }
        //检测到空闲连接,触发
        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            //这里可做一些断开连接的处理
        }
    }


    public static Channel getChannel() {
        return channel;
    }

    public static List<SocketUserInfo> getSocketUserInfoList() {
        return socketUserInfoList;
    }
}