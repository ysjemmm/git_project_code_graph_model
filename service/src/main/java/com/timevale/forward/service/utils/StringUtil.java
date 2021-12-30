package com.timevale.forward.service.utils;

/**
 * @author by YangXu
 * @date 2021/12/30 14:22
 */
public class StringUtil {

    public static String toLikeStr(String str){
        if(str == null || "".equals(str)){
            return str;
        }
        str = str.replaceAll("/", "//");
        str = str.replaceAll("%", "/%");
        str = str.replaceAll("_", "/_");
        return str;
    }
}
