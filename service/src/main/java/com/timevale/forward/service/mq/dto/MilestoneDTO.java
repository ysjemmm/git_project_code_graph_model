package com.timevale.forward.service.mq.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * @author by YangXu
 * @date 2023/02/15 13:57
 */
@Getter
@Setter
public class MilestoneDTO {
    private Date planEndDate;
    private Date planStartDate;
    private Date actualEndDate;
    private Date actualStartDate;
    private Boolean suspend;
    private Boolean invalid;
    private Integer milestoneType;
    private Long milestoneRelationId;
}
