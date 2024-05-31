package com.bot.inori.bot.action;

import com.bot.inori.bot.model.res.ReplyMsg;
import com.bot.inori.bot.utils.annotation.BotCommand;
import com.bot.inori.bot.handler.BotHandler;
import com.bot.inori.bot.handler.MessageHandler;
import com.bot.inori.bot.model.data.ImageCacheData;
import com.bot.inori.bot.model.req.ForwardGroupMessage;
import com.bot.inori.bot.model.req.ForwardPrivateMessage;
import com.bot.inori.bot.model.req.MediaMessage;
import com.bot.inori.bot.model.res.AtMsg;
import com.bot.inori.bot.model.res.FileMsg;
import com.bot.inori.bot.model.res.MetadataChain;
import com.bot.inori.bot.utils.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@SuppressWarnings("unused")
public class GifAction {

    @BotCommand(cmd = "gif", alias = "GIF", description = "基础的gif操作，d（解码） r（旋转，后面可加毫秒数） x（缩小） ")
    public void gif(MetadataChain chain) {
        String content = chain.getBasicCommand().substring(3).trim();
        FileMsg img = chain.getFirstImage();
        if (img == null && chain.getReply() != null) {
            String url = ImageCacheData.gutData(chain.getReply().getId());
            if (url != null) img = new FileMsg(null, url);
        }
        if (img == null) return;
        AtMsg at = chain.getFirstAt();
        if (content.startsWith("d")) decodeGIF(chain, img);
        else if (content.startsWith("r")) {
            content = content.substring(1).trim();
            int seconds = 20;
            if (!StringUtil.isBlank(content) && StringUtil.isNumeric(content)) {
                seconds = Integer.parseInt(content);
                if (seconds < 20) seconds = 20;
            }
            rotate(chain, img, at, seconds);
        } else if (content.startsWith("x")) {
            InputStream stream = GifUtils.reduceGif(HttpUtils.getStream(img.getUrl(), false), 400, 400);
            if (stream != null) chain.sendMsg(MediaMessage.imageMedia(FileUtils.dlStream2LS(stream)));
        } else generateGIF(chain, content);
    }

    @BotCommand(cmd = "倒转", permit = false, description = "gif倒转")
    public void reserve(MetadataChain chain) {
        try {
            if (chain.getBasicCommand().trim().length() != 2) return;
            String url = null;
            FileMsg img = chain.getFirstImage();
            ReplyMsg reply = chain.getReply();
            if (img != null) url = img.getUrl();
            else if (reply != null) url = ImageCacheData.gutData(reply.getId());
            if (url != null) {
                BufferedImage bi = ImageIO.read(new URL(url));
                if (bi != null && bi.getType() == BufferedImage.TYPE_BYTE_INDEXED) {
                    String path = Objects.requireNonNull(BotHandler.getFile("working/temp.gif")).getAbsolutePath();
                    ImageIO.write(bi, "gif", new File(path));
                    GifUtils.reverseGif(HttpUtils.getStream(url, false), path);
                    path = FileUtils.dlOrMoveImage2LS(path, false);
                    chain.sendMsg(MediaMessage.imageMedia(path));
                }
            }
        } catch (Exception e) {
            MessageHandler.getLogger().error("倒转GIF报错！ {}", e.getMessage());
        }
    }

    @BotCommand(cmd = "加速", permit = false, description = "gif加速")
    public void speedup(MetadataChain chain) {
        speed(chain);
    }

    @BotCommand(cmd = "减速", permit = false, description = "gif减速")
    public void speed_down(MetadataChain chain) {
        speed(chain);
    }

    private void speed(MetadataChain chain) {
        try {
            String cmd = chain.getBasicCommand().trim();
            if (cmd.length() != 2) return;
            String url = null;
            FileMsg img = chain.getFirstImage();
            ReplyMsg reply = chain.getReply();
            if (img != null) url = img.getUrl();
            else if (reply != null) url = ImageCacheData.gutData(reply.getId());
            if (url != null) {
                BufferedImage bi = ImageIO.read(new URL(url));
                if (bi != null && bi.getType() == BufferedImage.TYPE_BYTE_INDEXED) {
                    String path = Objects.requireNonNull(BotHandler.getFile("working/temp.gif")).getAbsolutePath();
                    ImageIO.write(bi, "gif", new File(path));
                    GifUtils.speed(HttpUtils.getStream(url, false), path, "加速".equals(cmd));
                    path = FileUtils.dlOrMoveImage2LS(path, false);
                    chain.sendMsg(MediaMessage.imageMedia(path));
                }
            }
        } catch (Exception e) {
            MessageHandler.getLogger().error("变速GIF报错！ {}", e.getMessage());
        }
    }

    private static void decodeGIF(MetadataChain chain, FileMsg img) {
        List<InputStream> list = GifUtils.gifToSteam(img.getUrl());
        if (!list.isEmpty()) {
            if (chain.getGroup_id() != null) {
                ForwardGroupMessage fgm = new ForwardGroupMessage(chain.getGroup_id());
                for (InputStream is : list) {
                    fgm.appendNode(MediaMessage.imageMedia(FileUtils.dlStream2LS(is)));
                    if (fgm.size() == 40) {
                        SimpleMessageUtils.sendGroupForwardMessage(chain.getSession(), fgm);
                        fgm = new ForwardGroupMessage(chain.getGroup_id());
                    }
                }
                if (fgm.size() > 0) SimpleMessageUtils.sendGroupForwardMessage(chain.getSession(), fgm);
            } else {
                ForwardPrivateMessage fpm = new ForwardPrivateMessage(chain.getSender().getUser_id());
                for (InputStream is : list) {
                    fpm.appendNode(MediaMessage.imageMedia(FileUtils.dlStream2LS(is)));
                    HttpUtils.closeStream(is);
                    if (fpm.size() == 40) {
                        SimpleMessageUtils.sendPrivateForwardMessage(chain.getSession(), fpm);
                        fpm = new ForwardPrivateMessage(chain.getSender().getUser_id());
                    }
                }
                if (fpm.size() > 0) SimpleMessageUtils.sendPrivateForwardMessage(chain.getSession(), fpm);
            }
        }
    }

    private static void rotate(MetadataChain chain, FileMsg img, AtMsg at, int seconds) {
        try {
            String path = Objects.requireNonNull(BotHandler.getFile("working/temp.gif")).getAbsolutePath();
            String url;
            if (img != null) url = img.getUrl();
            else url = BotHandler.getQQHeadUrl(at.getQq());
            BufferedImage bi = ImageIO.read(new URL(url));
            if (bi != null) {
                GifUtils.imagesRotate(bi, path, seconds);
                chain.sendMsg(MediaMessage.imageMedia(FileUtils.dlOrMoveImage2LS(path, false)));
            }
        } catch (Exception e) {
            MessageHandler.getLogger().error("旋转图片报错！ {}", e.getMessage());
        }
    }

    private static void generateGIF(MetadataChain chain, String content) {
        try {
            List<BufferedImage> images = new ArrayList<>();
            if (StringUtil.isBlank(content)) {
                List<FileMsg> list = chain.getImages();
                if (!list.isEmpty()) {
                    for (FileMsg msg : list) images.add(ImageIO.read(new URL(msg.getUrl())));
                }
            } else {
                if (StringUtil.isNumeric(content)) {
                    List<String> list = PixivUtils.getPixivMsg(Long.parseLong(content), 1);
                    if (list.size() > 1) {
                        for (String str : list) {
                            if (str.contains("?")) str = str.substring(0, str.indexOf("?"));
                            images.add(ImageIO.read(new URL(str)));
                        }
                    } else chain.sendMsg("图源必须包含1张以上图片");
                }
            }
            if (!images.isEmpty()) {
                String path = Objects.requireNonNull(BotHandler.getFile("working/temp.gif")).getAbsolutePath();
                InputStream stream = GifUtils.imagesToGIF(images, path, null);
                if (stream != null) {
                    chain.sendMsg(MediaMessage.imageMedia(FileUtils.dlStream2LS(stream)));
                    HttpUtils.closeStream(stream);
                }
            }
        } catch (Exception e) {
            MessageHandler.getLogger().error("合成GIF报错！ {}", e.getMessage());
        }
    }
}
