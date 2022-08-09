package org.sobadfish.shout.utils;

import cn.nukkit.utils.TextFormat;

import java.util.Random;

public class TextUtils {


    /**
     * 将文本中的颜色字符移除
     *
     * @param str 输入的字符串
     * @return 移除后的
     * */
    public static String clearColor(String str){
        int i;
        while ((i = str.indexOf("§")) != -1){
            str = str.substring(0,i)  + str.substring(i+2);
        }
        return str;
    }

    /**
     * 将文本设置为随机颜色
     *
     * @param str 输入的字符串
     * @return 移除后的
     * */
    public static String roundColor(String str){
        str = clearColor(str);
        int i;
        StringBuilder n = new StringBuilder();
        for(char c: str.toCharArray()){
            n.append(TextFormat.values()[new Random().nextInt(17)].toString()).append(c);
        }
        return n.toString();

    }

    /**
     * 计算文本的行数
     *
     * @param str 输入的字符串
     * @return 移除后的
     * */
    public static int mathLine(String str){
        return str.split("\\n").length;
    }
}
