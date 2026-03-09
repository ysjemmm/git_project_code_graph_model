package com.timevale.forward.service.utils.richtext;

import cn.hutool.core.util.StrUtil;
import com.timevale.crm.sdk.common.entity.integration.dto.FileDownloadDTO;
import com.timevale.crm.sdk.common.utils.file.FileUtil;
import com.timevale.filesystem.common.service.result.GetDownloadUrlResult;
import com.timevale.forward.service.integration.OssClient;
import com.timevale.forward.service.utils.EnvUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 富文本图片地址刷新
 */
@Slf4j
@Component
public class RichTextImageUrlRefresher {

    private static final Pattern IMG_TAG_PATTERN = Pattern.compile("<img\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern SRC_ATTR_PATTERN = Pattern.compile("(?<![\\w-])src\\s*=\\s*(\"[^\"]*\"|'[^']*'|[^\\s>]+)", Pattern.CASE_INSENSITIVE);

    @Resource
    private OssClient ossClient;

    @Resource
    private EnvUtils envUtils;

    public String refresh(String content) {
        if (StrUtil.isBlank(content) || !StrUtil.containsIgnoreCase(content, "<img")) {
            return content;
        }

        Matcher matcher = IMG_TAG_PATTERN.matcher(content);
        StringBuffer buffer = new StringBuffer();
        Map<String, String> urlCache = new HashMap<>();

        while (matcher.find()) {
            String imageTag = matcher.group();
            String refreshedTag = refreshImageTag(imageTag, urlCache);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(refreshedTag));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private String refreshImageTag(String imageTag, Map<String, String> urlCache) {
        String fileId = extractAttr(imageTag, "alt");
        if (StrUtil.isBlank(fileId)) {
            return imageTag;
        }

        String downloadUrl = urlCache.computeIfAbsent(fileId, this::resolveDownloadUrl);
        if (StrUtil.isBlank(downloadUrl)) {
            return imageTag;
        }

        Matcher srcMatcher = SRC_ATTR_PATTERN.matcher(imageTag);
        if (srcMatcher.find()) {
            return srcMatcher.replaceFirst(Matcher.quoteReplacement("src=\"" + downloadUrl + "\""));
        }

        int insertPos = imageTag.endsWith("/>") ? imageTag.length() - 2 : imageTag.length() - 1;
        if (insertPos < 0) {
            return imageTag;
        }
        return imageTag.substring(0, insertPos) + " src=\"" + downloadUrl + "\"" + imageTag.substring(insertPos);
    }

    private String extractAttr(String imageTag, String attrName) {
        Pattern attrPattern = Pattern.compile("(?<![\\w-])" + attrName + "\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s>]+))", Pattern.CASE_INSENSITIVE);
        Matcher matcher = attrPattern.matcher(imageTag);
        if (!matcher.find()) {
            return "";
        }
        for (int i = 1; i <= 3; i++) {
            String value = matcher.group(i);
            if (StrUtil.isNotBlank(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private String resolveDownloadUrl(String fileId) {
        try {
            if (fileId.contains("-")) {
                return Optional.ofNullable(ossClient.getDownloadUrl(fileId))
                        .map(GetDownloadUrlResult::getUrl)
                        .orElse("");
            }
            return Optional.ofNullable(FileUtil.getFileDownloadInfo(fileId, envUtils.getEnv()))
                    .map(FileDownloadDTO::getDownloadUrl)
                    .orElse("");
        } catch (Exception e) {
            log.warn("刷新富文本图片地址失败, fileId:{}", fileId, e);
            return "";
        }
    }
}
