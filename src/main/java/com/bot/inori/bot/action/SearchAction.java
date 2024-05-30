package com.bot.inori.bot.action;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.bot.inori.bot.model.req.TextMessage;
import com.bot.inori.bot.utils.HttpUtils;
import com.bot.inori.bot.utils.StringUtil;
import com.bot.inori.bot.utils.annotation.BotCommand;
import com.bot.inori.bot.action.func.search.SearchCenter;
import com.bot.inori.bot.model.data.ImageCacheData;
import com.bot.inori.bot.model.res.FileMsg;
import com.bot.inori.bot.model.res.MetadataChain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class SearchAction {

    @BotCommand(cmd = "搜", alias = "搜番", description = "搜图（sauceNao, Ascii2d），搜番（Animate）", permit = false)
    public void search(MetadataChain chain) {
        FileMsg image = chain.getFirstImage();
        String url = null;
        if (image != null) url = image.getUrl();
        else if (chain.getReply() != null) url = ImageCacheData.gutData(chain.getReply().getId());
        SearchCenter.search(chain, chain.getBasicCommand(), url);
    }

    @BotCommand(cmd = "磁力搜索", isMaster = true, description = "在线搜磁力资源")
    public void magnet(MetadataChain chain) {
        String cmd = chain.getBasicCommand().substring(4).trim();
        if (cmd.isEmpty()) return;
        String[] params = cmd.split(" ");
        int page = 0;
        if (params.length > 1 && StringUtil.isNumeric(params[1]))
            page = Integer.parseInt(params[1]);
        JSONObject obj = HttpUtils.sendGet(String.format("https://hk-01.wdc2.info/request/?request_type=esearch_v11&keywords=%S&device_id=9b4681e3-9784-bb27-0550-a3692b61acfb&page=%s",
                params[0], page), false);
        if (obj != null && !obj.isEmpty()) {
            List<Object> list = new ArrayList<>();
            JSONArray array = obj.getJSONArray("videos");
            if (!array.isEmpty()) {
                list.add(new TextMessage("磁力搜索结果如下：\n"));
                StringBuilder builder = new StringBuilder();
                for (int i = 0; i < array.size(); i++) {
                    JSONObject data = array.getJSONObject(i);
                    if (!builder.isEmpty()) builder.append("\n\n");
                    builder.append("资源名称：").append(data.get("filename")).append("\n")
                            .append("资源大小：").append(data.get("filename")).append("\n")
                            .append("文件数量：").append(data.getBigDecimal("file_size")
                                    .divide(new BigDecimal(1048576), 2, RoundingMode.UP)).append("m").append("\n")
                            .append("磁力地址：magnet:?xt=urn:btih:").append(data.get("moc")).append("\n")
                            .append("发布时间：").append(data.get("create_time"));
                }
                list.add(new TextMessage(builder.toString()));
                chain.sendListForwardMsg(list);
                return;
            }
        }
        chain.sendReplyMsg(new TextMessage("未找到资源"));
    }
}
