package com.bot.inori.bot.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class Entity {

    private Integer code;

    private String msg;

    private String data;

    public static Entity success(String data) {
        return new Entity(1, "操作成功", data);
    }

    public static Entity error(Integer code, String msg) {
        return new Entity(code, msg, null);
    }
}
