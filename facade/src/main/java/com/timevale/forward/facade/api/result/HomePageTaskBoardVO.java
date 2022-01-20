package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @author: xingyun
 * @create: 2021-12-13 13:53
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("首页-任务工时看板")
public class HomePageTaskBoardVO extends ToString {

    @ApiModelProperty("项目人员姓名")
    private String name;

    @ApiModelProperty("项目阶段时间段")
    private List<HomePageProjectTimeVO> homePageProjectTimeVO;

}
