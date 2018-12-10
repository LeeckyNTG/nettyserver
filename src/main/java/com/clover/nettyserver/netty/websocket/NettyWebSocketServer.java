package com.clover.nettyserver.netty.websocket;

import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;

import com.alibaba.fastjson.JSONObject;
import com.clover.nettyserver.model.BaseMsg;
import com.clover.nettyserver.model.SocketUserInfo;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.springframework.beans.factory.annotation.Value;


public class NettyWebSocketServer {

    @Value("${netty.websocket_port}")
    private static int websocket_port = 8081;

    private static int webSocketPort;

    private static Channel channel;

    private static ChannelHandlerContext channelHandlerContext;

    private static List<SocketUserInfo> socketUserInfoList = new ArrayList<>();

    public static boolean run() {

        webSocketPort = websocket_port;
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast("http-codec", new HttpServerCodec())
                                .addLast("aggregator", new HttpObjectAggregator(65535))
                                .addLast("http-chunked", new ChunkedWriteHandler())
                                .addLast("handler", new WebSocketServerHandler());
                    }
                });
        try {
            ChannelFuture channelFuture = bootstrap.bind(webSocketPort).sync().channel().closeFuture().sync();
            channel = channelFuture.channel();
            return true;
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    private static class WebSocketServerHandler extends SimpleChannelInboundHandler<Object> {
        private WebSocketServerHandshaker handshaker;

        private void handleWebsocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
            if (frame instanceof CloseWebSocketFrame) {//关闭
                handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
            } else if (frame instanceof PingWebSocketFrame) {//ping消息
                ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
            } else if (frame instanceof TextWebSocketFrame) {//文本消息
                String request = ((TextWebSocketFrame) frame).text();
                System.out.println(request);
                BaseMsg baseMsg = JSONObject.parseObject(request, BaseMsg.class);
                if (baseMsg.getType().equals("connect")) {
                    SocketUserInfo socketUserInfo = JSONObject.parseObject(baseMsg.getData(), SocketUserInfo.class);
                    boolean bol = false;
                    int position = -1;
                    for (int i = 0; i < socketUserInfoList.size(); i++) {
                        if (socketUserInfo.getUserId() == socketUserInfoList.get(i).getUserId()) {
                            bol = true;
                            position = i;
                            break;
                        }
                    }
                    if (bol) {
                        socketUserInfoList.remove(position);
                    }
                    socketUserInfo.setChannelHandlerContext(ctx);
                    socketUserInfoList.add(socketUserInfo);
                }
            }
        }

        private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest request) {
            if (request.getDecoderResult().isSuccess() && "websocket".equals(request.headers().get("Upgrade"))) {
                System.out.println("create WebSocket connection");
                WebSocketServerHandshakerFactory factory = new WebSocketServerHandshakerFactory("ws://localhost:" + webSocketPort + "/websocket", null, false);
                handshaker = factory.newHandshaker(request);//通过创建请求生成一个握手对象
                if (handshaker != null) {
                    handshaker.handshake(ctx.channel(), request);
                }
            }
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
            ctx.flush();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            cause.printStackTrace();
            ctx.close();
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {

            channelHandlerContext = ctx;
            //发送心跳包
            new java.util.Timer().schedule(new TimerTask() {

                @Override
                public void run() {
                    if (handshaker != null) {
                        BaseMsg baseMsg = new BaseMsg();
                        baseMsg.setType("HeartBeat");
                        baseMsg.setData("我是服务器发给客户端的心跳包！");
                        ctx.channel().write(new TextWebSocketFrame(JSONObject.toJSONString(baseMsg)));
                        ctx.flush();
                    }
                }
            }, 1000, 1000);
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {

            System.out.println(msg.toString());
            if (msg instanceof FullHttpRequest) {//建立连接的请求
                handleHttpRequest(ctx, (FullHttpRequest) msg);
                System.out.println(msg.toString());
            } else if (msg instanceof WebSocketFrame) {//WebSocket
                handleWebsocketFrame(ctx, (WebSocketFrame) msg);
            }
        }
    }

    public static Channel getChannel() {
        return channel;
    }

    public static List<SocketUserInfo> getSocketUserInfoList() {
        return socketUserInfoList;
    }
}