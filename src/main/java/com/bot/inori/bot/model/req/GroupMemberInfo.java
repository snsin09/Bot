package com.bot.inori.bot.model.req;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GroupMemberInfo {

    private Long group_id;

    private Long user_id;
}
