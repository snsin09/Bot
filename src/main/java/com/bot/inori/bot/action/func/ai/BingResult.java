package com.bot.inori.bot.action.func.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BingResult {

    private Integer code;

    private Boolean status;

    private String model;

    private String message;

    private String original;
}
