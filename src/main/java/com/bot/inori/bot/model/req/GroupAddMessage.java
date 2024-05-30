package com.bot.inori.bot.model.req;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupAddMessage {

    private String flag;

    private String sub_type;

    private String type;

    private Boolean approve;

    //拒绝理由
    private String reason;

    public static GroupAddMessage groupAdd(String flag, Boolean approve, String reason) {
        return new GroupAddMessage(flag, "add", "add", approve, reason);
    }

    public static GroupAddMessage groupInvite(String flag, Boolean approve, String reason) {
        return new GroupAddMessage(flag, "invite", "invite", approve, reason);
    }
}
