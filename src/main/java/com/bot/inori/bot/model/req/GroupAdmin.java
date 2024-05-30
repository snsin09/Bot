package com.bot.inori.bot.model.req;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class GroupAdmin {

    private Long group_id;

    private Long user_id;

    /**
     * 有 user_id true 为设置，false 为取消
     * 无 user_id 是否禁言
     */
    private Boolean enable;

    public static GroupAdmin groupWholeBan(Long group_id, Boolean enable) {
        return new GroupAdmin(group_id, null, enable);
    }

    public static GroupAdmin groupAdmin(Long group_id, Long user_id, Boolean enable) {
        return new GroupAdmin(group_id, user_id, enable);
    }
}
