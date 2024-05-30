package com.bot.inori.bot.action.func.pet;

import com.bot.inori.bot.handler.BotHandler;
import com.bot.inori.bot.handler.MessageHandler;
import com.bot.inori.bot.utils.GifUtils;
import com.bot.inori.bot.utils.HttpUtils;
import kotlin.Pair;
import moe.dituon.petpet.share.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class PetCenter {

    private static BasePetService bps = null;

    public static void initPet() {
        bps = new BasePetService();
        BaseServiceConfig bsc = new BaseServiceConfig();
        bps.readBaseServiceConfig(bsc);
        bps.readData(BotHandler.getDir("pet").listFiles());
    }

    public static List<String> getAlis() {
        if (bps == null) initPet();
        List<String> resList = new LinkedList<>();
        bps.getAliaMap().forEach((s, strings) -> resList.add(s));
        return resList;
    }

    public static String getDataByAlia(String alia) {
        if (bps == null) initPet();
        AtomicReference<String> res = new AtomicReference<>();
        bps.getDataMap().forEach((s, map) -> {
            if (map == null || map.getAlias() == null) return;
            for (String str : map.getAlias()) {
                if (alia.equals(str)) res.set(s);
            }
        });
        return res.get();
    }

    public static List<String> getData() {
        if (bps == null) initPet();
        List<String> resList = new LinkedList<>();
        bps.getDataMap().forEach((s, strings) -> resList.add(s));
        return resList;
    }

    public static String getKeyAliasListString() {
        if (bps == null) initPet();
        return bps.keyListString;
    }

    public static Pair<InputStream, String> generatePet(BufferedImage from, InputStream to, String petkey, Boolean isGif) {
        List<String> files = null;
        try {
            String[] arr = petkey.split(" ");
            List<String> list = new ArrayList<>();
            if (arr.length > 1) {
                petkey = arr[0];
                list.addAll(Arrays.asList(arr).subList(1, arr.length));
            }
            if (!getAlis().contains(petkey) && !getData().contains(petkey)) return null;
            if (!getData().contains(petkey)) petkey = getDataByAlia(petkey);
            Pair<InputStream, String> resultPair = null;
            TextExtraData ted = new TextExtraData("", "", "", list);
            GifAvatarExtraDataProvider avatarExtraData;
            TemplateDTO data = bps.getDataMap().get(petkey);
            if (data != null && to != null && isGif && data.getType() != Type.GIF) {
                files = GifUtils.gifToImages(to, Objects.requireNonNull(BotHandler.getDir("working/gif")).getAbsolutePath());
                List<Integer> delays = new ArrayList<>();
                List<BufferedImage> images = new ArrayList<>();
                if (!files.isEmpty()) {
                    for (String file : files) {
                        String[] array = file.split("\\?");
                        BufferedImage img = ImageIO.read(new File(array[0]));
                        avatarExtraData = new GifAvatarExtraDataProvider(
                                () -> List.of(from),
                                () -> List.of(img),
                                null,
                                null,
                                null
                        );
                        Pair<InputStream, String> pair = bps.generateImage(petkey, avatarExtraData, ted, null);
                        if (pair != null) {
                            images.add(ImageIO.read(pair.getFirst()));
                            HttpUtils.closeStream(pair.getFirst());
                            delays.add(Integer.parseInt(array[1]));
                        }
                    }
                }
                InputStream steam = GifUtils.imagesToGIF(images, Objects.requireNonNull(BotHandler.getFile("working/temp.gif")).getAbsolutePath(), delays);
                resultPair = new Pair<>(steam, "gif");
            } else if (to != null) {
                BufferedImage image = ImageIO.read(to);
                avatarExtraData = new GifAvatarExtraDataProvider(
                        () -> List.of(from),
                        () -> List.of(image),
                        null,
                        null,
                        null
                );
                resultPair = bps.generateImage(petkey, avatarExtraData, ted, null);
            }
            return resultPair;
        } catch (Exception e) {
            MessageHandler.getLogger().error("pet报错！", e);
            return null;
        } finally {
            if (files != null && !files.isEmpty()) {
                files.forEach(file -> {
                    File f = new File(file.split("\\?")[0]);
                    if (f.exists() && f.isFile()) {
                        if (f.delete()) MessageHandler.getLogger().debug("删除缓存文件成功！");
                        else MessageHandler.getLogger().debug("删除缓存文件失败");
                    }
                });
            }
        }
    }
}
