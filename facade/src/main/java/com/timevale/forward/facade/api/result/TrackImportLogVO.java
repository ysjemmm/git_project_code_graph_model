package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * @author by YangXu
 * @date 2022/08/08 16:19
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("埋点导入记录")
public class TrackImportLogVO extends ToString {

    @ApiModelProperty("导入时间")
    private Date createDate;

    @ApiModelProperty("操作人")
    private String createMan;

    @ApiModelProperty("操作人id")
    private String createManId;

    @ApiModelProperty("导入事件数")
    private Integer importCount;

    @ApiModelProperty("事件错误量")
    private Integer importFailCount;

    @ApiModelProperty("导入结果：0导入成功，1导入失败")
    private Integer result;

    @ApiModelProperty("导入结果-描述")
    private String resultName;

    @ApiModelProperty("处理状态：0处理成功，1处理失败")
    private Integer status;

    @ApiModelProperty("处理状态-描述")
    private String statusName;
}
