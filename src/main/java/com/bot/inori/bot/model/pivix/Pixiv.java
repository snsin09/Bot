package com.bot.inori.bot.model.pivix;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class Pixiv {

    private String alt;

    private String description;

    private String title;

    private String userName;

    private Long userId;

    //缩略图
    private String url;

    private Urls urls;

    private Object tags;

    private Integer width;

    private Integer height;

    private String id;

    //头像
    private String profileImageUrl;

    private Date updateDate;

    private Date createDate;

    private Integer pageCount;

    private Map<Long, Object> userIllusts;

    private Integer likeCount;

    private Integer bookmarkCount;

    private Integer viewCount;

    public String getTagsToString() {
        String result = "";
        String space = "     ";
        if (tags instanceof ArrayList) {
            List<String> list = (ArrayList) tags;
            for (String str : list) {
                result += str + space;
            }
        } else if (tags instanceof Map) {
            Map<String, Object> map = (Map) tags;
            if (map.get("tags") != null) {
                List<Map<String, Object>> mapList = (List<Map<String, Object>>) map.get("tags");
                for (Map<String, Object> m : mapList) {
                    result += m.get("tag");
                    if (m.get("translation") != null) {
                        Map<String, Object> tran = (Map<String, Object>) m.get("translation");
                        if (tran.get("en") != null)
                            result += "(" + tran.get("en") + ")";
                    }
                    result += "     ";
                }
            }
        }
        if (result.length() > 1) result = result.substring(0,
                result.length() - space.length());
        return result;
    }
}
