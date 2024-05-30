package com.bot.inori.bot.utils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.bot.inori.bot.config.BaseConfig;
import com.bot.inori.bot.handler.BotHandler;
import com.bot.inori.bot.handler.MessageHandler;
import com.bot.inori.bot.model.req.*;
import jakarta.websocket.Session;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.util.concurrent.TimeUnit;

@SuppressWarnings("unused")
public class SimpleMessageUtils {

    public static void getLoginInfo(Session session) {
        getLoginInfo(session, "获取登录号信息");
    }

    public static void getLoginInfo(Session session, String echo) {
        try {
            Req req = new Req();
            req.setAction("get_login_info");
            req.setEcho(echo);
            session.getBasicRemote().sendText(JSON.toJSONString(req));
        } catch (Exception e) {
            MessageHandler.getLogger().error("获取登录号信息报错：{}", e.getMessage());
        }
    }

    public static void getGroupList(Session session) {
        getGroupList(session, "获取群列表");
    }

    public static void getGroupList(Session session, String echo) {
        try {
            Req req = new Req();
            req.setAction("get_group_list");
            req.setEcho(echo);
            session.getBasicRemote().sendText(JSON.toJSONString(req));
        } catch (Exception e) {
            MessageHandler.getLogger().error("获取群信息报错：{}", e.getMessage());
        }
    }

    public static void getGroupInfo(Session session) {
        getGroupInfo(session, "获取群信息");
    }

    public static void getGroupInfo(Session session, String echo) {
        try {
            Req req = new Req();
            req.setAction("get_group_info");
            req.setEcho(echo);
            session.getBasicRemote().sendText(JSON.toJSONString(req));
        } catch (Exception e) {
            MessageHandler.getLogger().error("获取群信息报错：{}", e.getMessage());
        }
    }

    public static void getGroupMemberInfo(Session session, Long group_id, Long user_id) {
        getGroupMemberInfo(session, group_id, user_id, "获取群成员信息");
    }

    public static void getGroupMemberInfo(Session session, Long group_id, Long user_id, String echo) {
        try {
            Req req = new Req();
            req.setAction("get_group_member_info");
            req.setEcho(echo);
            req.setParams(new GroupMemberInfo(group_id, user_id));
            session.getBasicRemote().sendText(JSON.toJSONString(req));
        } catch (Exception e) {
            MessageHandler.getLogger().error("获取群信息报错：{}", e.getMessage());
        }
    }

    public static JSONObject getMessageByGet(Long message_id) {
        return sendGet("get_msg", String.format("message_id=%s&echo=%s", message_id, "获取消息"));
    }

    public static JSONObject getHistoryMessageByGet(Long message_seq, Long group_id) {
        return sendGet("get_group_msg_history", String.format("message_seq=%s&group_id=%s&echo=%s", message_seq, group_id, "获取历史消息"));
    }

    public static JSONObject getGroupInfoByGet(Long group_id, String echo) {
        return sendGet("get_group_info", String.format("group_id=%s&echo=%s", group_id, echo));
    }

    public static JSONObject getGroupMemberListByGet(Long group_id, String echo) {
        return sendGet("get_group_member_list", String.format("group_id=%s&echo=%s", group_id, echo));
    }

    /**
     * 模拟器需先开启端口转发才能使用 adb forward tcp:5700 tcp:5700
     */
    private static JSONObject sendGet(String method, String params) {
        JSONObject result = null;
        try {
            if (BaseConfig.httpHost != null) {
                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(10, TimeUnit.SECONDS)
                        .build();
                Request request = new Request.Builder()
                        .url(String.format("%s/%s?access_token=%s&%s", BaseConfig.httpHost, method, BaseConfig.AccessToken, params))
                        .addHeader("Authorization", "Bearer " + BaseConfig.AccessToken)
                        .build();
                Call call = client.newCall(request);
                Response response = call.execute();
                if (response.body() != null) result = JSON.parseObject(response.body().string());
                response.close();
            }
        } catch (Exception e) {
            MessageHandler.getLogger().error("http请求报错 {}", e.getMessage());
        }
        return result;
    }

    public static void getGroupMemberList(Session session, Long group_id) {
        getGroupMemberList(session, group_id, "获取群成员列表");
    }

    public static void getGroupMemberList(Session session, Long group_id, String echo) {
        try {
            Req req = new Req();
            req.setAction("get_group_member_list");
            req.setEcho(echo);
            req.setParams(new GroupMemberInfo(group_id, null));
            session.getBasicRemote().sendText(JSON.toJSONString(req));
        } catch (Exception e) {
            MessageHandler.getLogger().error("获取群信息报错：{}", e.getMessage());
        }
    }

    public static void getFriendList(Session session) {
        getFriendList(session, "获取好友列表");
    }

    public static void getFriendList(Session session, String echo) {
        try {
            Req req = new Req();
            req.setAction("get_friend_list");
            req.setEcho(echo);
            session.getBasicRemote().sendText(JSON.toJSONString(req));
        } catch (Exception e) {
            MessageHandler.getLogger().error("获取群信息报错：{}", e.getMessage());
        }
    }

    public static void sendGroupMessage(Session session, Long group_id, Object message) {
        sendGroupMessage(session, group_id, message, "发送群聊消息");
    }

    public static void sendGroupMessage(Session session, Long group_id, Object message, String echo) {
        try {
            Req req = new Req();
            req.setAction("send_group_msg");
            req.setEcho(echo);
            if (message instanceof String) {
                if (((String) message).length() > 4000) message = "文本消息过长";
            }
            req.setParams(new GroupMessage(group_id, message));
            session.getBasicRemote().sendText(JSON.toJSONString(req));
        } catch (Exception e) {
            MessageHandler.getLogger().error("发送群消息报错：{}", e.getMessage());
        }
    }

    public static void sendGroupForwardMessage(Session session, ForwardGroupMessage message) {
        sendGroupForwardMessage(session, message, "群聊合并转发");
    }

    public static void sendGroupForwardMessage(Session session, ForwardGroupMessage message, String echo) {
        try {
            if (message.size() == 0) return;
            Req req = new Req();
            req.setAction("send_group_forward_msg");
            req.setEcho(echo);
            req.setParams(message);
            session.getBasicRemote().sendText(JSON.toJSONString(req));
        } catch (Exception e) {
            MessageHandler.getLogger().error("发送群消息报错：{}", e.getMessage());
        }
    }

    public static void sendPrivateMessage(Session session, Long user_id, Object message) {
        sendPrivateMessage(session, user_id, message, "发送私聊消息");
    }

    public static void sendPrivateMessage(Session session, Long user_id, Object message, String echo) {
        try {
            Req req = new Req();
            req.setAction("send_private_msg");
            req.setEcho(echo);
            req.setParams(new PrivateMessage(user_id, message));
            session.getBasicRemote().sendText(JSON.toJSONString(req));
        } catch (Exception e) {
            MessageHandler.getLogger().error("发送群消息报错：{}", e.getMessage());
        }
    }

    public static void sendPrivateForwardMessage(Session session, ForwardPrivateMessage message) {
        sendPrivateForwardMessage(session, message, "私聊合并转发");
    }

    public static void sendPrivateForwardMessage(Session session, ForwardPrivateMessage message, String echo) {
        try {
            if (message.size() == 0) return;
            Req req = new Req();
            req.setAction("send_private_forward_msg");
            req.setEcho(echo);
            req.setParams(message);
            session.getBasicRemote().sendText(JSON.toJSONString(req));
        } catch (Exception e) {
            MessageHandler.getLogger().error("私聊合并转发报错：{}", e.getMessage());
        }
    }

    public static void getGroupSystemMsg(Session session) {
        getGroupSystemMsg(session, "获取群系统消息");
    }

    public static void getGroupSystemMsg(Session session, String echo) {
        try {
            Req req = new Req();
            req.setAction("get_group_system_msg");
            req.setEcho(echo);
            session.getBasicRemote().sendText(JSON.toJSONString(req));
        } catch (Exception e) {
            MessageHandler.getLogger().error("获取群系统消息报错：{}", e.getMessage());
        }
    }

    public static void setGroupAddRequest(Session session, GroupAddMessage msg) {
        setGroupAddRequest(session, msg, "处理加群请求／邀请");
    }

    public static void setGroupAddRequest(Session session, GroupAddMessage msg, String echo) {
        try {
            Req req = new Req();
            req.setAction("set_group_add_request");
            req.setEcho(echo);
            req.setParams(msg);
            session.getBasicRemote().sendText(JSON.toJSONString(req));
        } catch (Exception e) {
            MessageHandler.getLogger().error("处理加群请求／邀请报错：{}", e.getMessage());
        }
    }

    public static void setMsgEmojiLike(Session session, EmojiLikeMessage msg) {
        try {
            Req req = new Req();
            req.setAction("set_msg_emoji_like");
            req.setEcho("发送表情回应");
            req.setParams(msg);
            session.getBasicRemote().sendText(JSON.toJSONString(req));
        } catch (Exception e) {
            MessageHandler.getLogger().error("处理加群请求／邀请报错：{}", e.getMessage());
        }
    }

    public static void setGroupBan(Session session, GroupBanMessage msg) {
        setGroupBan(session, msg, "群单人禁言");
    }

    public static void setGroupBan(Session session, GroupBanMessage msg, String echo) {
        try {
            Req req = new Req();
            req.setAction("set_group_ban");
            req.setEcho(echo);
            req.setParams(msg);
            session.getBasicRemote().sendText(JSON.toJSONString(req));
        } catch (Exception e) {
            MessageHandler.getLogger().error("群单人禁言报错：{}", e.getMessage());
        }
    }

    public static void setGroupWholeBan(Session session, GroupAdmin msg) {
        setGroupWholeBan(session, msg, "群组全员禁言");
    }

    public static void setGroupWholeBan(Session session, GroupAdmin msg, String echo) {
        try {
            Req req = new Req();
            req.setAction("set_group_whole_ban");
            req.setEcho(echo);
            req.setParams(msg);
            session.getBasicRemote().sendText(JSON.toJSONString(req));
        } catch (Exception e) {
            MessageHandler.getLogger().error("群组全员禁言报错：{}", e.getMessage());
        }
    }

    public static void setGroupAdmin(Session session, GroupAdmin msg) {
        setGroupAdmin(session, msg, "群组设置管理员");
    }

    public static void setGroupAdmin(Session session, GroupAdmin msg, String echo) {
        try {
            Req req = new Req();
            req.setAction("set_group_admin");
            req.setEcho(echo);
            req.setParams(msg);
            session.getBasicRemote().sendText(JSON.toJSONString(req));
        } catch (Exception e) {
            MessageHandler.getLogger().error("群组设置管理员报错：{}", e.getMessage());
        }
    }

    public static void setGroupSpecialTitle(Session session, GroupSpecialTitleMessage msg) {
        setGroupSpecialTitle(session, msg, "设置群组专属头衔");
    }

    public static void setGroupSpecialTitle(Session session, GroupSpecialTitleMessage msg, String echo) {
        try {
            Req req = new Req();
            req.setAction("set_group_special_title");
            req.setEcho(echo);
            req.setParams(msg);
            session.getBasicRemote().sendText(JSON.toJSONString(req));
        } catch (Exception e) {
            MessageHandler.getLogger().error("处理加群请求／邀请报错：{}", e.getMessage());
        }
    }

    public static void setGroupLeave(Session session, GroupLeaveMessage msg) {
        setGroupLeave(session, msg, "退出群组");
    }

    public static void setGroupLeave(Session session, GroupLeaveMessage msg, String echo) {
        try {
            Req req = new Req();
            req.setAction("set_group_leave");
            req.setEcho(echo);
            req.setParams(msg);
            session.getBasicRemote().sendText(JSON.toJSONString(req));
        } catch (Exception e) {
            MessageHandler.getLogger().error("退出群组报错：{}", e.getMessage());
        }
    }

    public static void sendMaster(Object message) {
        sendMaster(message, "发送给主人");
    }

    public static void sendMaster(Object message, String echo) {
        try {
            Req req = new Req();
            req.setAction("send_private_msg");
            req.setEcho(echo);
            req.setParams(new PrivateMessage(BaseConfig.master, message));
            BotHandler.getSession().getBasicRemote().sendText(JSON.toJSONString(req));
        } catch (Exception e) {
            MessageHandler.getLogger().error("发送群消息报错：{}", e.getMessage());
        }
    }

    public static void sendThumbUp(Long user_id) {
        try {
            Req req = new Req();
            req.setAction("send_like");
            req.setEcho("点赞");
            req.setParams(new ThumbMessage(user_id, 10));
            BotHandler.getSession().getBasicRemote().sendText(JSON.toJSONString(req));
        } catch (Exception e) {
            MessageHandler.getLogger().error("点赞报错：{}", e.getMessage());
        }
    }

    public static void deleteMsg(Session session, Long message_id) {
        deleteMsg(session, message_id, "撤回消息");
    }

    public static void deleteMsg(Session session, Long message_id, String echo) {
        try {
            Req req = new Req();
            req.setAction("delete_msg");
            req.setEcho(echo);
            req.setParams(new RecallMessage(message_id));
            session.getBasicRemote().sendText(JSON.toJSONString(req));
        } catch (Exception e) {
            MessageHandler.getLogger().error("撤回消息报错：{}", e.getMessage());
        }
    }
}
