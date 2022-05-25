package com.timevale.forward.dal.dto;

import lombok.Data;

import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Data
public class PublishPlanResultDTO {

    private Integer count;

    private List<PublishPlanDTO> list;

}