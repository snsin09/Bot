package com.bot.inori.bot.model.req;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class WeatherMessage {

    private String type;

    private Weather data;

    public WeatherMessage(String city) {
        this.type = "city";
        this.data = new Weather(city);
    }
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class Weather {

    /**
     * 城市名称
     */
    private String city;

    /**
     * 城市代码
     */
    private String code;

    public Weather(String city) {
        this.city = city;
    }
}