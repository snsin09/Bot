package com.bot.inori.bot.model.entity;

import lombok.Data;

@Data
public class RequestApi {

    private String method;

    private String echo;

    private RequestApi(String method, String echo) {
        this.method = method;
        this.echo = echo;
    }

    public RequestApi echo(String echo) {
        this.setEcho(echo);
        return this;
    }

    public static final RequestApi GET_GROUP_LIST = new RequestApi("get_group_list", "获取群列表");
    public static final RequestApi GET_GROUP_INFO = new RequestApi("get_group_info", "获取群信息");
    public static final RequestApi GET_GROUP_MEMBER_INFO = new RequestApi("get_group_member_info", "获取群成员信息");
    public static final RequestApi GET_GROUP_MEMBER_LIST = new RequestApi("get_group_member_list", "获取群成员列表");
    public static final RequestApi GET_FRIEND_LIST = new RequestApi("get_friend_list", "获取群列表");
    public static final RequestApi SEND_GROUP_MSG = new RequestApi("send_group_msg", "发送群聊消息");
    public static final RequestApi SEND_GROUP_FORWARD_MSG = new RequestApi("send_group_forward_msg", "群聊合并转发");
    public static final RequestApi SEND_PRIVATE_MSG = new RequestApi("send_private_msg", "发送私聊消息");
    public static final RequestApi SEND_PRIVATE_FORWARD_MSG = new RequestApi("send_private_forward_msg", "发送私聊消息");
    public static final RequestApi GET_GROUP_SYSTEM_MSG = new RequestApi("get_group_system_msg", "获取群系统消息");
    public static final RequestApi SET_GROUP_ADD_REQUEST = new RequestApi("set_group_add_request", "处理加群请求／邀请");
    public static final RequestApi SET_GROUP_BAN = new RequestApi("set_group_ban", "群单人禁言");
    public static final RequestApi SET_GROUP_SPECIAL_TITLE = new RequestApi("set_group_special_title", "设置群组专属头衔");
    public static final RequestApi SET_GROUP_LEAVE = new RequestApi("set_group_leave", "退出群组");
    public static final RequestApi SEND_LIKE = new RequestApi("send_like", "点赞");
    public static final RequestApi DELETE_MSG = new RequestApi("delete_msg", "撤回消息");
}
