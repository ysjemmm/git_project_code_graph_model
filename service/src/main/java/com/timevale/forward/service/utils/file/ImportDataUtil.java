package com.timevale.forward.service.utils.file;

import cn.hutool.core.collection.CollUtil;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class ImportDataUtil {

    private ImportDataUtil() {}

    /**
     * 校验文件大小
     * @param file MultipartFile
     */
    public static void validateFile(MultipartFile file) {
        // 设定阈值，例如1MB
        long thresholdSize = (long) 1024 * 1024 * 100;
        if (file.getSize() > thresholdSize) {
            throw new BaseBizRuntimeException("上传的文件不可大于100MB");
        }
    }

    /**
     * 校验文件头
     * @param bufferedReader BufferedReader
     * @param fileHeader 文件头
     * @throws IOException 异常
     */
    public static List<String> getVerifiedFileHeader(BufferedReader bufferedReader, String[] fileHeader) throws IOException {
        // 读取表头行进行校验
        String headerStr = bufferedReader.readLine();
        if (StringUtils.isBlank(headerStr)) {
            throw new BaseBizRuntimeException("文件头不可为空，请下载模版重新导入");
        }
        int standardHeadLength = fileHeader.length;
        // 获取表头行数据
        String[] header = headerStr.split(",", -1);
        // 获取表头行数据list
        List<String> headerList = Arrays.asList(header);
        List<String> lackFields = new ArrayList<>(standardHeadLength);
        for (String s : fileHeader) {
            if (!headerList.contains(s)) {
                lackFields.add(s);
            }
        }
        if (CollUtil.isNotEmpty(lackFields)) {
            throw new BaseBizRuntimeException("文件头缺失字段：" + String.join(",", lackFields));
        }
        return headerList;
    }

    public static int[] getArrayOfPositionsForVerificationFile(List<String> splitFileHeader, String[] fileHeader) {
        // 新的索引位置
        int[] newIndex = new int[fileHeader.length];
        for (int i = 0; i < fileHeader.length; i++) {
            newIndex[i] = splitFileHeader.indexOf(fileHeader[i]);
        }
        return newIndex;
    }

    public static String[] splitLineData(String line, int fileHeaderLength) {
        if (line == null || line.isEmpty()) {
            log.warn("Input line cannot be null or empty");
            throw new IllegalArgumentException("Input line cannot be null or empty");
        }

        List<String> fields = getFields(line);

        String[] data = fields.toArray(new String[0]);

        if (data.length < fileHeaderLength) {
            log.warn(line + ": Input line does not contain enough data fields");
            throw new IllegalArgumentException("Input line does not contain enough data fields");
        }

        if (data.length > fileHeaderLength) {
            log.warn(line + ": Input row contains redundant data fields");
            throw new IllegalArgumentException("Input row contains redundant data fields");
        }

        return data;
    }

    private static List<String> getFields(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"' && !inQuotes) {
                // 开始引号
                inQuotes = true;
            } else if (c == '"' && inQuotes) {
                // 结束引号
                inQuotes = false;
            } else if (c == ',' && !inQuotes) {
                // 字段分隔符
                fields.add(currentField.toString().trim());
                currentField = new StringBuilder();
            } else {
                // 普通字符
                currentField.append(c);
            }
        }

        // 添加最后一个字段
        fields.add(currentField.toString().trim());
        return fields;
    }

    /**
     * 去除对象所有String字段的所有空白字符
     * @param item 对象实例
     * @param <T> 对象类型
     */
    public static <T> void trimAllStringFields(T item) {
        if (item == null) {
            return;
        }

        Class<?> clazz = item.getClass();

        // 获取所有声明的字段（包括私有字段）
        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {
            try {
                // 只处理String类型的字段
                if (field.getType() == String.class) {
                    // 设置字段可访问（包括私有字段）
                    field.setAccessible(true);

                    // 获取字段值
                    String value = (String) field.get(item);

                    // 如果值不为空，则去除所有空白字符并设置回字段
                    if (value != null) {
                        String trimmedValue = value.replaceAll("\\s+", " ").trim();
                        field.set(item, trimmedValue);
                    }
                }
            } catch (Exception e) {
                // 记录日志但不中断处理
                log.warn("Failed to trim field: " + field.getName() + " in class: " + clazz.getSimpleName(), e);
            }
        }
    }
}