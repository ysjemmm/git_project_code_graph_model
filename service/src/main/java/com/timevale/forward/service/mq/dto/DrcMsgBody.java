package com.timevale.forward.service.mq.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

/**
 * @author by YangXu
 * @date 2023/02/10 10:09
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class DrcMsgBody {

    private String action;

    private String daName;

    private String tableName;

    private String rowKey;

    private String after;

    private String before;

    private String content;

    private String gtId;
}
