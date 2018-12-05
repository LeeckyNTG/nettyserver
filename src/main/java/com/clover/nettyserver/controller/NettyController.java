package com.clover.nettyserver.controller;

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


}
