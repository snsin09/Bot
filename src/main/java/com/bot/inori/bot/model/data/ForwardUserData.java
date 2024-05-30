package com.bot.inori.bot.model.data;

import com.bot.inori.bot.utils.HttpUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Random;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ForwardUserData {

    private Long uin;

    private String name;

    public static ForwardUserData newInstance() {
        Random random = new Random();
        String res = HttpUtils.getResp("http://apii.gq/api/shiju.php");
        return new ForwardUserData(random.nextLong(1000000000L) + 100000000, res);
    }
}
