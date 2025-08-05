package com.timevale.forward.service.utils.file;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

public class PinyinConverter {
    
    /**
     * 将汉字字符串转换为拼音
     * @param chinese 中文字符串
     * @return 拼音字符串
     */
    public static String toPinyin(String chinese) {
        StringBuilder pinyin = new StringBuilder();
        HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
        // 小写拼音
        format.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        // 不带声调
        format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        
        char[] chars = chinese.toCharArray();
        for (char c : chars) {
            try {
                String[] arr = PinyinHelper.toHanyuPinyinStringArray(c, format);
                if (arr == null || arr.length == 0) {
                    // 非汉字字符保留原样
                    pinyin.append(c);
                } else {
                    // 多音字取第一个读音
                    pinyin.append(arr[0]);
                }
            } catch (BadHanyuPinyinOutputFormatCombination e) {
                pinyin.append(c);
            }
        }
        return pinyin.toString();
    }
    
    /**
     * 获取汉字字符串的拼音首字母
     * @param chinese 中文字符串
     * @return 拼音首字母字符串
     */
    public static String toFirstLetter(String chinese) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < chinese.length(); i++) {
            String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray(chinese.charAt(i));
            if (pinyinArray != null && pinyinArray.length > 0) {
                // 取第一个拼音的首字母
                result.append(pinyinArray[0].charAt(0));
            } else {
                // 非汉字字符保留原样
                result.append(chinese.charAt(i));
            }
        }
        return result.toString();
    }
}