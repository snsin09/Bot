package com.bot.inori.bot.model.reverse;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class ReserveRes {

    private String action;

    private Params params;

    private String echo;
}
