package com.bot.inori.bot.model.data;

import lombok.Data;

import java.util.List;

@Data
public class GroupMemberData {

    //QQ 号
    private Long user_id;

    //群号
    private Long group_id;

    //昵称
    private String user_name;

    //性别
    private String sex;

    //年龄
    private Integer age;

    //专属头衔
    private String title;

    //专属头衔过期时间戳
    private Long title_expire_time;

    //群昵称
    private String nickname;

    //显示名
    private String user_displayname;

    private String card;

    //距离
    private Integer distance;

    //群荣誉
    private List<Integer> honor;

    //入群时间
    private Long join_time;

    //最后发言时间
    private Long last_active_time;

    //最后发言时间
    private Long last_sent_time;

    //群头衔
    private String unique_name;

    //地区
    private String area;

    //成员等级
    private String level;

    //角色
    private String role;

    //是否不良记录成员
    private Boolean unfriendly;

    //是否允许修改群名片
    private Boolean card_changeable;

    private Long shut_up_timestamp;
}
