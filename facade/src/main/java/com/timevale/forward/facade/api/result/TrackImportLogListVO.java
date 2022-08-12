package com.timevale.forward.facade.api.result;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("事件属性信息")
public class TrackImportLogListVO extends ToString {

    @ApiModelProperty("id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @ApiModelProperty("处理状态：0处理成功，1处理失败")
    private Integer status;

    @ApiModelProperty("处理状态描述")
    private String statusName;

    @ApiModelProperty("文件id")
    private String fileId;

    @ApiModelProperty("导入事件数")
    private Integer importCount;

    @ApiModelProperty("导入事件错误数")
    private Integer importFailCount;

    @ApiModelProperty("导入结果：0导入成功，1导入失败")
    private Integer result;

    @ApiModelProperty("导入结果描述")
    private String resultName;

    @ApiModelProperty("操作人")
    private String createMan;

    @ApiModelProperty("操作人id")
    private String createManId;

    @ApiModelProperty("导入时间")
    private Date createDate;
}
