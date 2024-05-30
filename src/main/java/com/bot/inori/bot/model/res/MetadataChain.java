package com.bot.inori.bot.model.res;

import com.alibaba.fastjson2.JSONObject;
import com.bot.inori.bot.model.req.ForwardGroupMessage;
import com.bot.inori.bot.model.req.ForwardPrivateMessage;
import com.bot.inori.bot.model.req.ReplyMessage;
import com.bot.inori.bot.utils.SimpleMessageUtils;
import com.bot.inori.bot.handler.BotHandler;
import jakarta.websocket.Session;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MetadataChain {

    /**
     * 消息元数据
     */
    List<MetadataMsg> list = new ArrayList<>();

    /**
     * 群号
     */
    private Long group_id;

    /**
     * 讨论组ID
     */
    private Long discuss_id;

    /**
     * 消息发送人
     */
    private Sender sender;

    /**
     * 会话Session
     */
    Session session;

    /**
     * 消息ID
     */
    private Long message_id;

    public MetadataChain(Session session, List<MetadataMsg> list, Sender sender) {
        this.session = session;
        this.list = list;
        this.sender = sender;
    }

    public void sendMsg(Object msg) {
        if (msg == null) return;
        if (this.group_id != null) SimpleMessageUtils.sendGroupMessage(this.session, this.group_id, msg);
        else SimpleMessageUtils.sendPrivateMessage(this.session, this.sender.getUser_id(), msg);
    }

    public void sendMsg(Object msg, String echo) {
        if (msg == null) return;
        if (this.group_id != null) SimpleMessageUtils.sendGroupMessage(this.session, this.group_id, msg, echo);
        else SimpleMessageUtils.sendPrivateMessage(this.session, this.sender.getUser_id(), msg, echo);
    }

    public void sendReplyMsg(Object msg) {
        if (msg == null) return;
        List<Object> list = new ArrayList<>();
        list.add(new ReplyMessage(this.getMessage_id()));
        if (msg instanceof List<?>) {
            list.addAll((List<?>) msg);
        } else list.add(msg);
        if (this.group_id != null) SimpleMessageUtils.sendGroupMessage(this.session, this.group_id, list);
        else SimpleMessageUtils.sendPrivateMessage(this.session, this.sender.getUser_id(), list);
    }

    public void sendListForwardMsg(List<Object> list) {
        if (list == null || list.isEmpty()) return;
        if (this.group_id != null) {
            ForwardGroupMessage fgm = new ForwardGroupMessage(this.group_id);
            list.forEach(fgm::appendNode);
            if (fgm.size() > 0) SimpleMessageUtils.sendGroupForwardMessage(this.session, fgm);
        } else {
            ForwardPrivateMessage fpm = new ForwardPrivateMessage(this.getSender().getUser_id());
            list.forEach(fpm::appendNode);
            if (fpm.size() > 0) SimpleMessageUtils.sendPrivateForwardMessage(this.session, fpm);
        }
    }

    public String getBasicCommand() {
        if (!this.list.isEmpty()) {
            StringBuilder builder = new StringBuilder();
            for (MetadataMsg msg : this.list) {
                if ("text".equalsIgnoreCase(msg.getType())) {
                    builder.append((JSONObject.parseObject(msg.getData(), TextMsg.class)).getText().trim());
                }
            }
            return builder.toString().trim();
        }
        return null;
    }

    public List<FileMsg> getImages() {
        if (!this.list.isEmpty()) {
            List<FileMsg> fileMsgList = new ArrayList<>();
            for (MetadataMsg msg : this.list) {
                if ("image".equalsIgnoreCase(msg.getType())) {
                    fileMsgList.add(JSONObject.parseObject(msg.getData(), FileMsg.class));
                }
            }
            return fileMsgList;
        }
        return Collections.emptyList();
    }

    public FileMsg getFirstImage() {
        List<FileMsg> fileMsgList = getImages();
        if (fileMsgList.isEmpty()) return null;
        return fileMsgList.get(0);
    }

    public List<AtMsg> getAtMsg() {
        if (!this.list.isEmpty()) {
            List<AtMsg> atMsgList = new ArrayList<>();
            for (MetadataMsg msg : this.list) {
                if ("at".equalsIgnoreCase(msg.getType())) {
                    atMsgList.add(JSONObject.parseObject(msg.getData(), AtMsg.class));
                }
            }
            return atMsgList;
        }
        return Collections.emptyList();
    }

    public Boolean isAtBot() {
        if (!this.list.isEmpty()) {
            for (MetadataMsg msg : this.list) {
                if ("at".equalsIgnoreCase(msg.getType())) {
                    AtMsg atMsg = JSONObject.parseObject(msg.getData(), AtMsg.class);
                    if (BotHandler.isBot(atMsg.getQq())) return true;
                }
            }
        }
        return false;
    }

    public AtMsg getFirstAt() {
        List<AtMsg> atMsgList = getAtMsg();
        if (atMsgList.isEmpty()) return null;
        return atMsgList.get(0);
    }

    public List<ReplyMsg> getReplyMsg() {
        if (!this.list.isEmpty()) {
            List<ReplyMsg> replyMsgList = new ArrayList<>();
            for (MetadataMsg msg : this.list) {
                if ("reply".equalsIgnoreCase(msg.getType())) {
                    replyMsgList.add(JSONObject.parseObject(msg.getData(), ReplyMsg.class));
                }
            }
            return replyMsgList;
        }
        return Collections.emptyList();
    }

    public ReplyMsg getReply() {
        List<ReplyMsg> replyMsgList = getReplyMsg();
        if (replyMsgList.isEmpty()) return null;
        return replyMsgList.get(0);
    }

    public List<JsonMsg> getJsonMsg() {
        if (!this.list.isEmpty()) {
            List<JsonMsg> jsonMsgList = new ArrayList<>();
            for (MetadataMsg msg : this.list) {
                if ("json".equalsIgnoreCase(msg.getType())) {
                    jsonMsgList.add(JSONObject.parseObject(msg.getData(), JsonMsg.class));
                }
            }
            return jsonMsgList;
        }
        return Collections.emptyList();
    }

    public JsonMsg getFirstJson() {
        List<JsonMsg> jsonMsgList = getJsonMsg();
        if (jsonMsgList.isEmpty()) return null;
        return jsonMsgList.get(0);
    }
}
