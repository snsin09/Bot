package com.bot.inori.bot.model.req;

import lombok.Data;

/**
 * 位置
 */
@Data
public class LocationMessage {

    private String type;

    private Location data;

    public LocationMessage(Float lat, Float lon) {
        this.type = "location";
        this.data = new Location(lat, lon);
    }
}

@Data
class Location {

    /**
     * 纬度
     */
    private Float lat;

    /**
     * 经度
     */
    private Float lon;

    /**
     * 标题
     */
    private String title;

    /**
     * 内容
     */
    private String content;

    public Location(Float lat, Float lon) {
        this.lat = lat;
        this.lon = lon;
    }
}