package com.bot.inori.bot.action;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.bot.inori.bot.action.func.ai.*;
import com.bot.inori.bot.config.BaseConfig;
import com.bot.inori.bot.handler.BotHandler;
import com.bot.inori.bot.handler.MessageHandler;
import com.bot.inori.bot.handler.PermissionHandler;
import com.bot.inori.bot.model.req.MediaMessage;
import com.bot.inori.bot.model.req.TextMessage;
import com.bot.inori.bot.model.res.MetadataChain;
import com.bot.inori.bot.utils.BaiduUtils;
import com.bot.inori.bot.utils.HttpUtils;
import com.bot.inori.bot.utils.SimpleMessageUtils;
import com.bot.inori.bot.utils.StringUtil;
import com.bot.inori.bot.utils.annotation.BotCommand;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <a href="https://github.com/snsin09/gpti-js/blob/main/README.md">...</a>
 */
@SuppressWarnings("unused")
public class AIAction {

    @BotCommand(cmd = "bing", alias = "BING,Bing", description = "使用bing对话功能", permit = false)
    public void bing(MetadataChain chain) {
        try {
            String cmd = chain.getBasicCommand();
            cmd = cmd.substring(4).trim();
            JSONObject params = new JSONObject();
            params.put("messages", BingData.putSendModels(chain.getSender().getUser_id(), cmd));
            params.put("conversation_style", "Balanced");
            params.put("markdown", false);
            params.put("model", "bing");
            params.put("stream", false);
            String result = HttpUtils.sendJSONPost("https://nexra.aryahcr.cc/api/chat/complements", params, true);
            if (!StringUtil.isBlank(result)) {
                if (!result.contains("{")) return;
                result = result.substring(result.indexOf("{"));
                BingResult br = JSON.parseObject(result, BingResult.class);
                if (br.getCode() == 200) {
                    BingData.putReplyModels(chain.getSender().getUser_id(), br.getMessage());
                    chain.sendReplyMsg(new TextMessage(br.getMessage()));
                } else {
                    MessageHandler.getLogger().warn("bing对话警告 {}", result);
                    BingData.clearModels(chain.getSender().getUser_id());
                    chain.sendMsg(new TextMessage("Bing对话出错，已重置对话"));
                }
            }
        } catch (Exception e) {
            MessageHandler.getLogger().error("bing对话报错 {}", e.getMessage());
        }
    }

    @BotCommand(cmd = "回复", description = "@bot 使用AI回复")
    public void at_reply(MetadataChain chain) {
        try {
            String cmd = chain.getBasicCommand().trim();
            if (cmd.startsWith("回复")) cmd = cmd.substring(2).trim();
            List<ChatModel> list = new ArrayList<>();
            list.add(new ChatModel("system", BaseConfig.CharacterPrompt));
            list.addAll(GptData.getModels(chain.getSender().getUser_id()));
            list.add(new ChatModel("user", cmd));
            JSONObject result = HttpUtils.sendGptGet(list);
            if (result != null) {
                JSONArray choices = result.getJSONArray("choices");
                if (choices != null && !choices.isEmpty()) {
                    String msg = choices.getJSONObject(0).getJSONObject("message").getString("content");
                    if (!StringUtil.isBlank(msg)) {
                        chain.sendReplyMsg(new TextMessage(msg));
                        GptData.putSendModels(chain.getSender().getUser_id(), cmd);
                        GptData.putReplyModels(chain.getSender().getUser_id(), msg);
                        if (BotHandler.isMaster(chain.getSender().getUser_id()) || PermissionHandler.checkPermission("语音回复", chain.getGroup_id())) {
                            String url = String.format(BaseConfig.VitsUrl + "voice/vits?id=%s&text=%s", BotHandler.SPEAKER_ID, URLEncoder.encode(msg, StandardCharsets.UTF_8));
                            if (HttpUtils.isUrlOk(url)) chain.sendMsg(MediaMessage.audioMedia(url));
                        }
                    }
                } else {
                    GptData.clearModels(chain.getSender().getUser_id());
                    chain.sendReplyMsg(new TextMessage("对话过长，已重置"));
                }
            } else {
                List<Object> data = new ArrayList<>();
                data.add(new TextMessage("猫脑过载，躺平"));
                data.add(MediaMessage.imageMedia(BotHandler.getFile("working/过载.jpg")));
                chain.sendReplyMsg(data);
            }
        } catch (Exception e) {
            MessageHandler.getLogger().error("AI回复报错 {}", e.getMessage());
        }
    }

    @BotCommand(cmd = "语音回复", description = "使用AI语音回复")
    public void voiceReply(MetadataChain chain) {
        String msg = chain.getBasicCommand().substring(4).trim();
        if (StringUtil.isBlank(msg)) return;
        String url = String.format(BaseConfig.VitsUrl + "voice/vits?id=%s&text=%s", BotHandler.SPEAKER_ID, URLEncoder.encode(msg, StandardCharsets.UTF_8));
        if (HttpUtils.isUrlOk(url)) chain.sendMsg(MediaMessage.audioMedia(url));
    }

    @BotCommand(cmd = "发言人", description = "查看回复语言发言人", isMaster = true)
    public void speaker(MetadataChain chain) {
        String url = BaseConfig.VitsUrl + "voice/speakers";
        if (HttpUtils.isUrlOk(url)) {
            JSONObject json = HttpUtils.sendGet(url, false);
            if (json != null) {
                JSONArray arr = json.getJSONArray("VITS");
                StringBuilder sb = new StringBuilder();
                sb.append("id  发言人");
                for (int i = 0; i < arr.size(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    sb.append("\n");
                    if (i < 10) sb.append(" ");
                    sb.append(obj.get("id")).append("  ").append(obj.get("name"));
                }
                chain.sendMsg(new TextMessage(sb.toString()));
            }
        }
    }

    @BotCommand(cmd = "设置发言人", description = "查看回复语言发言人", isMaster = true)
    public void setSpeaker(MetadataChain chain) {
        String cmd = chain.getBasicCommand().substring(5).trim();
        if (StringUtil.isBlank(cmd) || !StringUtil.isNumeric(cmd)) return;
        String url = BaseConfig.VitsUrl + "voice/speakers";
        if (HttpUtils.isUrlOk(url)) {
            JSONObject json = HttpUtils.sendGet(url, false);
            if (json != null) {
                JSONArray arr = json.getJSONArray("VITS");
                int index = Integer.parseInt(cmd);
                if (arr.size() > index) {
                    BotHandler.SPEAKER_ID = index;
                    chain.sendReplyMsg(new TextMessage(String.format("设置发言人%s成功", arr.getJSONObject(index).getString("name"))));
                    return;
                }
            }
        }
        chain.sendReplyMsg(new TextMessage("设置发言人失败"));
    }

    @BotCommand(cmd = "gpt", alias = "GPT,Gpt", description = "使用gpt对话功能", permit = false)
    public void gpt(MetadataChain chain) {
        try {
            String cmd = chain.getBasicCommand();
            cmd = cmd.substring(3).trim();
            JSONObject params = new JSONObject();
            params.put("messages", GptData.getModels(chain.getSender().getUser_id()));
            params.put("prompt", cmd);
            params.put("markdown", false);
            params.put("model", AIConfig.GPT);
            String result = HttpUtils.sendJSONPost("https://nexra.aryahcr.cc/api/chat/gpt", params, true);
            if (!StringUtil.isBlank(result)) {
                if (!result.contains("{")) return;
                result = result.substring(result.indexOf("{"));
                GptResult gr = JSON.parseObject(result, GptResult.class);
                if (gr.getCode() == 200) {
                    GptData.putSendModels(chain.getSender().getUser_id(), cmd);
                    GptData.putReplyModels(chain.getSender().getUser_id(), gr.getGpt());
                    chain.sendReplyMsg(new TextMessage(gr.getGpt()));
                    return;
                }
            }
            MessageHandler.getLogger().warn("gpt对话警告 {}", result);
            GptData.clearModels(chain.getSender().getUser_id());
            chain.sendMsg(new TextMessage("gpt对话出错，已重置对话"));
        } catch (Exception e) {
            MessageHandler.getLogger().error("gpt对话报错 {}", e.getMessage());
        }
    }

    @BotCommand(cmd = "pv1", description = "使用 Prodia v1 绘图")
    public void pv1(MetadataChain chain) {
        try {
            String cmd = chain.getBasicCommand();
            cmd = "masterpiece, best quality, " + cmd.substring(3).trim();
            JSONObject params = new JSONObject();
            params.put("prompt", cmd);
            params.put("model", "prodia");
            params.put("data", new ProdiaData(AIConfig.V1));
            chain.sendReplyMsg(new TextMessage("使用 Prodia v1 生成图片，请稍后..."));
            String result = HttpUtils.sendJSONPost("https://nexra.aryahcr.cc/api/image/complements", params, true);
            if (!StringUtil.isBlank(result)) {
                if (!result.contains("{")) return;
                result = result.substring(result.indexOf("{"));
                ProdiaResult pr = JSON.parseObject(result, ProdiaResult.class);
                if (pr.getCode() == 200 && !pr.getImages().isEmpty()) {
                    String base64 = pr.getImages().get(0);
                    if (!checkPic(chain, base64, cmd)) return;
                    base64 = "base64://" + base64.replace("data:image/jpeg;base64,", "");
                    chain.sendReplyMsg(MediaMessage.imageMedia(base64));
                } else {
                    MessageHandler.getLogger().warn("Prodia v1 绘图警告 {}", result);
                    chain.sendMsg(new TextMessage("Prodia v1 绘图出错"));
                }
            }
        } catch (Exception e) {
            MessageHandler.getLogger().error("Prodia v1 绘图报错 {}", e.getMessage());
        }
    }

    @BotCommand(cmd = "psd", description = "使用 Prodia StableDiffusion 绘图")
    public void psd(MetadataChain chain) {
        try {
            String cmd = chain.getBasicCommand();
            cmd = "masterpiece, best quality, " + cmd.substring(3).trim();
            JSONObject params = new JSONObject();
            params.put("prompt", cmd);
            params.put("model", "prodia-stablediffusion");
            params.put("data", new ProdiaData(AIConfig.SD));
            chain.sendReplyMsg(new TextMessage("使用 Prodia StableDiffusion 生成图片，请稍后..."));
            String result = HttpUtils.sendJSONPost("https://nexra.aryahcr.cc/api/image/complements", params, true);
            if (!StringUtil.isBlank(result)) {
                if (!result.contains("{")) return;
                result = result.substring(result.indexOf("{"));
                ProdiaResult pr = JSON.parseObject(result, ProdiaResult.class);
                if (pr.getCode() == 200 && !pr.getImages().isEmpty()) {
                    String base64 = pr.getImages().get(0);
                    if (!checkPic(chain, base64, cmd)) return;
                    base64 = "base64://" + base64.replace("data:image/jpeg;base64,", "");
                    chain.sendReplyMsg(MediaMessage.imageMedia(base64));
                } else {
                    MessageHandler.getLogger().warn("Prodia StableDiffusion 绘图警告 {}", result);
                    chain.sendMsg(new TextMessage("Prodia StableDiffusion 绘图出错"));
                }
            }
        } catch (Exception e) {
            MessageHandler.getLogger().error("Prodia StableDiffusion 绘图报错 {}", e.getMessage());
        }
    }

    @BotCommand(cmd = "pxl", description = "使用 Prodia StableDiffusion XL 绘图")
    public void pxl(MetadataChain chain) {
        try {
            String cmd = chain.getBasicCommand();
            cmd = "masterpiece, best quality, " + cmd.substring(3).trim();
            JSONObject params = new JSONObject();
            params.put("prompt", cmd);
            params.put("model", "prodia-stablediffusion-xl");
            params.put("data", new ProdiaData(AIConfig.XL));
            chain.sendReplyMsg(new TextMessage("使用 Prodia StableDiffusion XL 生成图片，请稍后..."));
            String result = HttpUtils.sendJSONPost("https://nexra.aryahcr.cc/api/image/complements", params, true);
            if (!StringUtil.isBlank(result)) {
                if (!result.contains("{")) return;
                result = result.substring(result.indexOf("{"));
                ProdiaResult pr = JSON.parseObject(result, ProdiaResult.class);
                if (pr.getCode() == 200 && !pr.getImages().isEmpty()) {
                    String base64 = pr.getImages().get(0);
                    if (!checkPic(chain, base64, cmd)) return;
                    base64 = "base64://" + base64.replace("data:image/jpeg;base64,", "");
                    chain.sendReplyMsg(MediaMessage.imageMedia(base64));
                } else {
                    MessageHandler.getLogger().warn("Prodia StableDiffusion XL 绘图警告 {}", result);
                    chain.sendMsg(new TextMessage("Prodia StableDiffusion XL 绘图出错"));
                }
            }
        } catch (Exception e) {
            MessageHandler.getLogger().error("Prodia StableDiffusion XL 绘图报错 {}", e.getMessage());
        }
    }

    private Boolean checkPic(MetadataChain chain, String base64, String cmd) {
        if (!BotHandler.isMaster(chain.getSender().getUser_id()) &&
                !BaiduUtils.checkLegal(base64.replace("data:image/jpeg;base64,", ""))) {
            chain.sendReplyMsg(new TextMessage("图片审核不合规！我要告诉主人去！"));
            if (chain.getGroup_id() != null)
                SimpleMessageUtils.sendMaster(new TextMessage(String.format("来自%s的不合法绘图\nQQ：%s\n昵称：%s\n标签：%s",
                        BotHandler.getGroupText(chain.getGroup_id()), chain.getSender().getUser_id(), chain.getSender().getNickname(), cmd)));
            else
                SimpleMessageUtils.sendMaster(new TextMessage(String.format("来自（%s-%s）的不合法绘图\n标签：%s",
                        chain.getSender().getNickname(), chain.getSender().getUser_id(), cmd)));
            base64 = "base64://" + base64.replace("data:image/jpeg;base64,", "");
            SimpleMessageUtils.sendMaster(MediaMessage.imageMedia(base64));
            return false;
        }
        return true;
    }

    @BotCommand(cmd = "查看模型", description = "查看能用的模型，gpt v1 sd xl", isMaster = true)
    public void getModels(MetadataChain chain) {
        try {
            String cmd = chain.getBasicCommand();
            cmd = cmd.substring(4).trim();
            List<String> list = AIConfig.getModels(cmd);
            if (list.isEmpty()) {
                chain.sendReplyMsg(new TextMessage("没有这种模型"));
            } else {
                List<Object> messages = new ArrayList<>();
                switch (cmd.toLowerCase()) {
                    case "gpt" -> messages.add(new TextMessage("GPT 模型列表"));
                    case "v1" -> messages.add(new TextMessage("V1 模型列表"));
                    case "sd" -> messages.add(new TextMessage("StableDiffusion 模型列表"));
                    case "xl" -> messages.add(new TextMessage("StableDiffusion XL 模型列表"));
                }
                AtomicInteger i = new AtomicInteger();
                StringBuilder sb = new StringBuilder();
                list.forEach(str -> {
                    if (!sb.isEmpty()) sb.append("\n");
                    sb.append((i.getAndIncrement())).append("  ").append(str);
                });
                messages.add(new TextMessage(sb.toString()));
                chain.sendListForwardMsg(messages);
            }
        } catch (Exception e) {
            MessageHandler.getLogger().error("查看模型报错 {}", e.getMessage());
        }
    }

    @BotCommand(cmd = "设置模型", description = "设置模型，gpt v1 sd xl", isMaster = true)
    public void setModel(MetadataChain chain) {
        try {
            String cmd = chain.getBasicCommand();
            cmd = cmd.substring(4).trim();
            String[] arr = cmd.split(" ");
            if (arr.length != 2 || !StringUtil.isNumeric(arr[1])) return;
            chain.sendReplyMsg(new TextMessage(AIConfig.setModel(arr[0], Integer.parseInt(arr[1]))));
        } catch (Exception e) {
            MessageHandler.getLogger().error("查看模型报错 {}", e.getMessage());
        }
    }

    @BotCommand(cmd = "当前模型", description = "查看AI当前模型", isMaster = true)
    public void currentModel(MetadataChain chain) {
        try {
            String cmd = chain.getBasicCommand();
            if (cmd.length() != 4) return;
            chain.sendReplyMsg(new TextMessage(String.format("当前模型：\nGPT：%s\nV1：%s\nSD：%s\nSD_XL：%s",
                    AIConfig.GPT, AIConfig.V1, AIConfig.SD, AIConfig.XL)));
        } catch (Exception e) {
            MessageHandler.getLogger().error("查看模型报错 {}", e.getMessage());
        }
    }

    @BotCommand(cmd = "清空会话", alias = "清空对话", description = "用户清空会话", permit = false)
    public void clear(MetadataChain chain) {
        try {
            String cmd = chain.getBasicCommand();
            if (cmd.length() == 4) {
                BingData.clearModels(chain.getSender().getUser_id());
                GptData.clearModels(chain.getSender().getUser_id());
                Gpt4FreeData.clearModels(chain.getSender().getUser_id());
                chain.sendReplyMsg(new TextMessage("会话已重置"));
            }
        } catch (Exception e) {
            MessageHandler.getLogger().error("清空会话报错 {}", e.getMessage());
        }
    }

    @BotCommand(cmd = "清空全部会话", alias = "清空全部对话", description = "主人清空会话", isMaster = true)
    public void clearAll(MetadataChain chain) {
        try {
            String cmd = chain.getBasicCommand();
            if (cmd.length() == 6) {
                BingData.clearModels(null);
                GptData.clearModels(null);
                Gpt4FreeData.clearModels(null);
                chain.sendReplyMsg(new TextMessage("全部会话已重置"));
            }
        } catch (Exception e) {
            MessageHandler.getLogger().error("清空全部会话报错 {}", e.getMessage());
        }
    }
}
