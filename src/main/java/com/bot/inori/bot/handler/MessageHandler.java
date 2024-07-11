package com.bot.inori.bot.handler;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.bot.inori.bot.action.BiliAction;
import com.bot.inori.bot.model.data.EmojiLikeData;
import com.bot.inori.bot.model.data.GroupData;
import com.bot.inori.bot.model.data.GroupMemberData;
import com.bot.inori.bot.model.data.ImageCacheData;
import com.bot.inori.bot.model.req.EmojiLikeMessage;
import com.bot.inori.bot.model.req.TextMessage;
import com.bot.inori.bot.model.res.*;
import com.bot.inori.bot.utils.SimpleMessageUtils;
import com.bot.inori.bot.utils.StringUtil;
import jakarta.websocket.Session;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.SetParams;

import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class MessageHandler {

    @Getter
    private static final Logger logger = LoggerFactory.getLogger(MessageHandler.class);

    private static final Random random = new Random();

    public static void handleResponseMessage(Session session, JSONObject json) {
        try {
            if ("ok".equalsIgnoreCase(json.getString("status"))) {
                logger.info("ws响应数据：{}", json.toJSONString());
                switch (json.getString("echo")) {
                    case "获取登录号信息" -> {
                        JSONObject data = json.getJSONObject("data");
                        BotHandler.setBOT(data.getLong("user_id"));
                        logger.info("{}({}) 连接成功", data.getString("nickname"), data.getLong("user_id"));
                    }
                    case "获取群列表" -> {
                        logger.info("缓存群列表缓存完成");
                        List<GroupData> list = json.getList("data", GroupData.class);
                        BotHandler.setGROUPS(list);
                        list.forEach(group -> {
                            JSONObject res = SimpleMessageUtils.getGroupMemberListByGet(group.getGroup_id(), "获取群成员列表");
                            List<GroupMemberData> data = res.getList("data", GroupMemberData.class);
                            if (!data.isEmpty()) {
                                logger.info("{} 缓存群成员完成！共缓存成员{}名", BotHandler.getGroupText(group.getGroup_id()), data.size());
                                BotHandler.putMembers(data);
                            }
                        });
                    }
                    case "群聊合并转发", "撤回" -> {
                        Long message_id = json.getJSONObject("data").getLong("message_id");
                        new Timer().schedule(new TimerTask() {
                            @Override
                            public void run() {
                                SimpleMessageUtils.deleteMsg(session, message_id);
                            }
                        }, 60_000);
                    }
                    case "获取群系统消息" -> {
                        if (json.getJSONObject("data") != null) {
                            //邀请信息处理
                            JSONArray array = json.getJSONObject("data").getJSONArray("invited_requests");
                            if (array == null || array.isEmpty()) {
                                session.getBasicRemote().sendText("暂无邀请信息");
                                return;
                            }
                            StringBuilder builder = new StringBuilder();
                            for (int i = 0; i < array.size(); i++) {
                                JSONObject obj = array.getJSONObject(i);
                                if (obj.getBoolean("checked")) continue;
                                if (!builder.isEmpty()) builder.append("\n\n");
                                builder.append("邀请人：").append(obj.get("invitor_uin")).append("\n")
                                        .append("邀请者昵称：").append(obj.get("invitor_nick")).append("\n")
                                        .append("群号：").append(obj.get("group_id")).append("\n")
                                        .append("群名：").append(obj.get("group_name")).append("\n")
                                        .append("请求ID：").append(obj.get("request_id")).append("\n")
                                        .append("flag：").append(obj.get("flag"));
                            }
                            if (BotHandler.getChain() != null) {
                                if (builder.isEmpty()) BotHandler.getChain().sendReplyMsg(new TextMessage("暂无邀请信息！"));
                                else BotHandler.getChain().sendReplyMsg(new TextMessage(builder.toString()));
                                BotHandler.setChain(null);
                            }
                        }
                    }
                    case "设置群组专属头衔" -> {
                        BotHandler.getChain().sendReplyMsg(new TextMessage("设置群组专属头衔成功"));
                        BotHandler.setChain(null);
                    }
                    case "处理加群请求／邀请" -> {
                        BotHandler.getChain().sendReplyMsg(new TextMessage("处理加群请求／邀请成功"));
                        BotHandler.setChain(null);
                    }
                    case "退出群组" -> {
                        BotHandler.getChain().sendReplyMsg(new TextMessage("退出群组成功"));
                        BotHandler.setChain(null);
                    }
                    case "点赞" -> {
                        String msg = json.getString("message");
                        if (StringUtil.isBlank(msg)) msg = "今日已赞！";
                        BotHandler.getChain().sendReplyMsg(new TextMessage(msg));
                        BotHandler.setChain(null);
                    }
                    case "发送群聊消息" -> {
                        logger.info("发送群聊消息成功，消息id： {}", json.getJSONObject("data").getLong("message_id"));
                        if (random.nextInt(4) == 1) {
                            EmojiLikeMessage emoji = new EmojiLikeMessage();
                            emoji.setMessage_id(json.getJSONObject("data").getString("message_id"));
                            emoji.setEmoji_id(EmojiLikeData.emojiLikeData.get(random.nextInt(EmojiLikeData.emojiLikeData.size())).getCode());
                            SimpleMessageUtils.setMsgEmojiLike(session, emoji);
                        }
                    }
                    case "发送私聊消息" ->
                        logger.info("发送私聊消息成功，消息id： {}", json.getJSONObject("data").getLong("message_id"));
                    default -> logger.info("暂不处理");
                }
            } else {
                if (BotHandler.getChain() != null) {
                    BotHandler.getChain().sendReplyMsg(new TextMessage(json.getString("message")));
                    BotHandler.setChain(null);
                }
            }
        } catch (Exception e) {
            logger.error("处理响应消息报错 {}", e.getMessage());
        }
    }

    public static void handleReceiveMessage(Session session, JSONObject json) {
        try (Jedis jedis = new Jedis()) {
            Sender sender = json.getObject("sender", Sender.class);
            List<MetadataMsg> list = json.getList("message", MetadataMsg.class);
            MetadataChain chain = new MetadataChain(session, list, sender);
            chain.setMessage_id(json.getLong("message_id"));
            FileMsg firstImage = chain.getFirstImage();
            if (firstImage != null) ImageCacheData.putData(chain.getMessage_id(), firstImage.getUrl());
            switch (json.getString("message_type")) {
                case "group" -> {
                    logger.info("群消息 {}：{} {}", BotHandler.getGroupText(json.getLong("group_id")),
                            sender.getFormatUser(), json.getString("raw_message"));
                    chain.setGroup_id(json.getLong("group_id"));
                    if (BotHandler.isBot(chain.getSender().getUser_id())) {
                        if (random.nextInt(4) == 1) {
                            EmojiLikeMessage emoji = new EmojiLikeMessage();
                            emoji.setMessage_id(String.valueOf(chain.getMessage_id()));
                            emoji.setEmoji_id(EmojiLikeData.emojiLikeData.get(random.nextInt(EmojiLikeData.emojiLikeData.size())).getCode());
                            SimpleMessageUtils.setMsgEmojiLike(session, emoji);
                        }
                    }
                    JsonMsg msg = chain.getFirstJson();
                    if (msg != null) {
                        if (msg.getData().contains("com.tencent.miniapp") && msg.getData().contains("b23.tv")) {
                            JSONObject data = JSON.parseObject(msg.getData());
                            new Thread(() -> BiliAction.analysisRedirectB23Url(chain, data.getJSONObject("meta")
                                    .getJSONObject("detail_1").getString("qqdocurl"), true)).start();
                        }
                    }
                }
                case "private" -> {
                    String sub_type = json.getString("sub_type");
                    String msg = "好友消息：{} {}";
                    if (!"friend".equals(sub_type)) msg = sub_type + "消息：{} {}";
                    logger.info(msg, sender.getFormatUser(), json.get("raw_message"));
                }
                default -> logger.info("不支持的消息类型{}", json.toJSONString());
            }
//            if (!BotHandler.isMaster(chain.getSender().getUser_id())) return;
            //记录消息收发
            if (BotHandler.isBot(chain.getSender().getUser_id())) {
                String sendKey = String.format("Yz:count:send:msg:bot:%s:total", BotHandler.getBOT());
                int value = jedis.get(sendKey) == null ? 0 : Integer.parseInt(jedis.get(sendKey));
                jedis.set(sendKey, String.valueOf(++value), new SetParams().ex(86400));
                if (chain.getFirstImage() != null) {
                    String imageKey = String.format("Yz:count:send:image:bot:%s:total", BotHandler.getBOT());
                    value = jedis.get(imageKey) == null ? 0 : Integer.parseInt(jedis.get(imageKey));
                    value += chain.getImages().size();
                    jedis.set(imageKey, String.valueOf(value), new SetParams().ex(86400));
                }
            } else {
                String recKey = String.format("Yz:count:receive:msg:bot:%s:total", BotHandler.getBOT());
                int value = jedis.get(recKey) == null ? 0 : Integer.parseInt(jedis.get(recKey));
                jedis.set(recKey, String.valueOf(++value), new SetParams().ex(86400));
                new InvokeHandler(chain).start();
            }
        } catch (Exception e) {
            logger.error("处理接收消息报错 {}", e.getMessage());
        }
    }

    public static void handleNoticeMessage(Session session, JSONObject json) {
        NoticeHandler.handleNoticeMessage(session, json);
    }

    public static void handleRequestMessage(Session session, JSONObject json) {
        RequestHandler.handleRequestMessage(session, json);
    }
}
