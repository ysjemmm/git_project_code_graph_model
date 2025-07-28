package com.timevale.forward.model.bo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * URL组件数据类
 */
@Data
@AllArgsConstructor
public class UrlComponentBO {

    private String encryptedUserId;

    private String shortUrlCode;
}