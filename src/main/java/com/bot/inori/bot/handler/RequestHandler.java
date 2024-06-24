package com.bot.inori.bot.handler;

import com.alibaba.fastjson2.JSONObject;
import com.bot.inori.bot.config.BaseConfig;
import com.bot.inori.bot.model.req.TextMessage;
import com.bot.inori.bot.utils.SimpleMessageUtils;
import jakarta.websocket.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class RequestHandler {

    private static final Logger logger = LoggerFactory.getLogger(RequestHandler.class);

    public static void handleRequestMessage(Session session, JSONObject json) {
        try {
            logger.info("请求消息：" + json.toJSONString());
            List<Object> messages = new ArrayList<>();
            switch (json.getString("request_type")) {
                case "friend" -> {
                    if (BaseConfig.friendInfo) {
                        messages.add(new TextMessage(String.format("用户%s请求加好友，验证信息：%s",
                                json.getLong("user_id"), json.getString("comment"))));
                    }
                    SimpleMessageUtils.sendMaster(messages);
                }
                case "group" -> {
                    logger.info(json.toJSONString());
                    if (BaseConfig.groupInfo || "invite".equalsIgnoreCase(json.getString("sub_type"))) {
                        messages.add(new TextMessage(String.format("收到加群%s\n群号：%s\n请求人：%s\n验证信息：%s",
                                "add".equalsIgnoreCase(json.getString("sub_type")) ? "请求" : "邀请",
                                json.getLong("group_id"),
                                json.getLong("user_id"),
                                json.getString("comment"))));
                        SimpleMessageUtils.sendMaster(messages);
                    }
                    if (!BotHandler.isMaster(json.getLong("user_id"))
                            && "invite".equals(json.getString("sub_type"))) {
                        SimpleMessageUtils.sendPrivateMessage(session, json.getLong("user_id"), new TextMessage("邀请入群请加群：789692753"));
                    }
                }
                default -> logger.info("不支持的请求消息 {} {}", session.getId(), json.toJSONString());
            }
        } catch (Exception e) {
            logger.error("处理请求消息报错 {}", e.getMessage());
        }
    }
}
