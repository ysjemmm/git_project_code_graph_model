package com.timevale.forward.dal.dto;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Data
public class PublishPlanDTO {

    private Long id;

    private String name;

    private List<String> apps;

    private Date windowStart;

    private Date windowEnd;

    private String status;

    private String releaseStatus;

    private String createPerson;

    private Boolean emergency;

}