package com.timevale.forward.service.utils.file;

import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpHeaders;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @auther: yuhua
 * @date: 2025/7/31 15:44
 * @description:
 */
public class FileUtil {

    public static void templateDownload(HttpServletResponse response, InputStream inputStream, String downloadName, OutputStream outputStream) throws IOException {
        if (inputStream == null) {
            throw new FileNotFoundException("File not found: " + downloadName);
        }

        // 设置响应头信息
        String encodeFileName = URLEncoder.encode(downloadName, "UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodeFileName + "\"");
        response.setHeader(HttpHeaders.CONTENT_TYPE, "text/csv");

        IOUtils.copy(inputStream, outputStream);
    }

    /**
     * 将失败的数据记录下载到本地文件
     * @param failedRecords 失败记录列表
     * @param response HttpServletResponse对象
     * @param filename 下载文件名
     * @throws IOException IO异常
     */
    public static void downloadFailedData(List<String> failedRecords, HttpServletResponse response, String filename) throws IOException {
        // 设置响应头信息
        String encodeFileName = URLEncoder.encode(filename, "UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodeFileName + "\"");
        response.setHeader(HttpHeaders.CONTENT_TYPE, "text/csv;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");

        // 将数据写入响应输出流
        try (OutputStream outputStream = response.getOutputStream();
             InputStream inputStream = new ByteArrayInputStream(String.join("\n", failedRecords).getBytes(StandardCharsets.UTF_8))) {
            IOUtils.copy(inputStream, outputStream);
            outputStream.flush();
        }
    }

}
