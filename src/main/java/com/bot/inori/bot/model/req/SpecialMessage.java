package com.bot.inori.bot.model.req;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 特殊消息 篮球 新猜拳 新骰子 能收不能发
 */
@Data
@AllArgsConstructor
public class SpecialMessage {

    private String type;

    private Special data;

    public static SpecialMessage basketball(Long id) {
        return new SpecialMessage("basketball", new Special(id));
    }

    public static SpecialMessage new_rps(Long id) {
        return new SpecialMessage("new_rps", new Special(id));
    }

    public static SpecialMessage new_dice(Long id) {
        return new SpecialMessage("new_dice", new Special(id));
    }
}

@Data
@AllArgsConstructor
class Special {

    //篮球 5 没中, 4 擦边没中, 3 卡框, 2 擦边中, 1 正中
    //猜拳 锤 3 剪 2 布 1
    //骰子 点数 ID
    private Long id;
}