package com.bot.inori.bot.model.entity;

import com.alibaba.fastjson2.util.DateUtils;
import com.bot.inori.bot.utils.StringUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BVideo {

    //标题
    private String title;

    //动态
    private String dynamic;

    //up名字
    private String name;

    //up主id
    private Long mid;

    //类型
    private String tname;

    //封面
    private String pic;

    //简介
    private String desc;

    //发布日期
    private Date pubdate;

    //观看数
    private Long view;

    //视频1P cid
    private Long cid;

    //bv号
    private String bvid;

    //弹幕数
    private Integer danmaku;

    //评论数
    private Integer reply;

    //收藏数
    private Integer favorite;

    //硬币数
    private Integer coin;

    //分享数
    private Integer share;

    //点赞数
    private Integer like;

    @Override
    public String toString() {
        return "标题：'" + title + '\'' +
                (StringUtil.isBlank(dynamic) ? "" : "\n动态：'" + dynamic + '\'') +
                "\n类型：'" + tname + '\'' +
                " | UP：'" + name + '\'' +
                " | 日期：" + DateUtils.format(pubdate, "yyyy-MM-dd HH:mm:ss") +
                "\n播放：" + view +
                " | 弹幕：" + danmaku +
                " | 收藏：" + favorite +
                " | 评论：" + reply +
                "\n硬币：" + coin +
                " | 分享：" + share +
                " | 点赞：" + like +
                "\n简介：'" + desc;
    }
}
