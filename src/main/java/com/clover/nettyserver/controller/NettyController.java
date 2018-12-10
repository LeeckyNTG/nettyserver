package com.clover.nettyserver.controller;

import com.clover.nettyserver.netty.tcp.NettyTcpServer;
import com.clover.nettyserver.netty.tcp.TcpUtils;
import com.clover.nettyserver.netty.websocket.WebSocketUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;

@Controller
public class NettyController {


    @ResponseBody
    @RequestMapping("/sendMsg")
    public String sendMsg(HttpServletRequest request) {

        int userId = Integer.parseInt((String) request.getParameter("userId"));
        String msg = request.getParameter("msg");

        boolean bol = WebSocketUtils.sendMsg(msg, userId);

        if (bol)
            return "success";
        else
            return "error";
    }


    @ResponseBody
    @RequestMapping("/startNettyWebsocket")
    public String startNettyWebsocket() {


        boolean bol = WebSocketUtils.run();
        if (bol)
            return "success";
        else
            return "error";

    }

    @ResponseBody
    @RequestMapping("/startNettyTcp")
    public String startNettyTcp() {

        boolean bol = TcpUtils.run();
        if (bol)
            return "success";
        else
            return "error";
    }

    @ResponseBody
    @RequestMapping("/sendTcpMsg")
    public String sendTcpMsg(HttpServletRequest request) {

        int userId = Integer.parseInt((String) request.getParameter("userId"));
        String msg = request.getParameter("msg");

        boolean bol = TcpUtils.sendTcpMsg(msg, userId);

        if (bol)
            return "success";
        else
            return "error";
    }
}
