package com.timevale.forward.service.integration;

import com.alibaba.fastjson.JSONObject;
import com.timevale.filesystem.common.service.api.FileSystemService;
import com.timevale.filesystem.common.service.query.BaseInput;
import com.timevale.filesystem.common.service.query.GetDownloadUrlInput;
import com.timevale.filesystem.common.service.query.GetSignUrlInput;
import com.timevale.filesystem.common.service.result.GetDownloadUrlResult;
import com.timevale.filesystem.common.service.result.GetFileInfoResult;
import com.timevale.filesystem.common.service.result.GetSignUrlResult;
import com.timevale.mandarin.base.exception.BaseRuntimeException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author jingchun
 * created on 2021/10/15
 */
@Component
public class OssClient {

    @Value("${oss.projectId:3438757422}")
    private String projectId;

    @Resource
    private FileSystemService fileSystemService;


    public GetSignUrlResult getSignUrl(String fileName) {
        GetSignUrlInput input = new GetSignUrlInput();
        input.setFileName(fileName);
        input.setContentType(MediaType.APPLICATION_OCTET_STREAM.toString());
        input.setProjectId(projectId);
        input.setNeedHttps(true);
        return fileSystemService.getSignUrl(input);
    }


    public GetDownloadUrlResult getDownloadUrl(String fileKey) {
        GetDownloadUrlInput result = new GetDownloadUrlInput();
        result.setFileKey(fileKey);
        return fileSystemService.getDownloadUrl(result);
    }

    public GetFileInfoResult getFileInfo(String fileKey) {
        BaseInput input = new BaseInput();
        input.setFileKey(fileKey);
        return fileSystemService.getFileInfo(input);
    }


    public JSONObject uploadFile(String signUrl, String body) {
        String head = new String(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
        return uploadFile(signUrl, (head + body).getBytes());
    }


    public JSONObject uploadFile(String signUrl, byte[] body) {
        URI uri;
        try {
            uri = new URI(signUrl);
        } catch (URISyntaxException e) {
            throw new BaseRuntimeException(e);
        }
        RestTemplate restTemplate = new RestTemplateBuilder().build();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        HttpEntity<byte[]> httpEntity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.PUT, httpEntity, String.class);
        return JSONObject.parseObject(response.getBody());
    }


}
