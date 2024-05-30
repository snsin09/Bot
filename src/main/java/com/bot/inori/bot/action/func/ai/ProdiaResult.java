package com.bot.inori.bot.action.func.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProdiaResult {

    private Integer code;

    private Boolean status;

    private String prompt;

    private String model;

    private ProdiaData data;

    private List<String> images;
}
