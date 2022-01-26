package com.timevale.forward.facade.api.result;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;


/**
 * @author by YangXu
 * @date 2022/01/26 10:28
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("首页-项目工时看板")
public class HomePageProjectBoardVO extends ToString {
    @ApiModelProperty("项目人员id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    @ApiModelProperty("项目人员姓名")
    private String userName;

    @ApiModelProperty("项目阶段时间段")
    private List<HomePageProjectTimeVO> homePageProjectTimeVOList;

}
