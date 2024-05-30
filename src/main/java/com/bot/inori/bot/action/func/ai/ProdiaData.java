package com.bot.inori.bot.action.func.ai;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ProdiaData {

    private String model;

    private Integer cfg_scale = 7;

    //v1
    private Integer steps = 30;

    private String sampler = "DPM++ 2M Karras";

    private String negative_prompt = "EasyNegative, nsfw, lowres, bad anatomy, bad hands, text, error, missing fingers, extra digit, fewer digits, cropped, worst quality, lowquality, normal quality, jpeg artifacts, signature, watermark, usern";

    //下面sd和rd
    private String sampling_method = "DPM++ 2M Karras";

    private Integer sampling_steps = 30;

    private Integer width = 768;

    private Integer height = 1024;

    private String prompt_negative = "EasyNegative, nsfw, lowres, bad anatomy, bad hands, text, error, missing fingers, extra digit, fewer digits, cropped, worst quality, lowquality, normal quality, jpeg artifacts, signature, watermark, usern";

    public ProdiaData(String model) {
        this.model = model;
    }
}
