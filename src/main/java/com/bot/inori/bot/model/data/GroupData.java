package com.bot.inori.bot.model.data;

import lombok.Data;

import java.util.List;

@Data
public class GroupData {

    private Long group_id;

    private String group_name;

    private String group_remark;

    private Long group_uin;

    private List<Long> admins;

    private String class_text;

    private Boolean is_frozen;

    private Long max_member;

    private Long member_num;

    private Long member_count;

    private Long max_member_count;
}
