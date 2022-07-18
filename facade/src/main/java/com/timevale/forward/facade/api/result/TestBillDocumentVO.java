package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@ApiModel("提测单文档说明对象")
public class TestBillDocumentVO extends ToString {

    @ApiModelProperty("冒烟用例链接")
    private String caseUrl;

    @ApiModelProperty("全量用例链接")
    private String allCaseUrl;

    @ApiModelProperty("创建人id")
    private String createManId;
    @ApiModelProperty("创建人")
    private String createMan;
    @ApiModelProperty("更新人id")
    private String modifyManId;
    @ApiModelProperty("更新人")
    private String modifyMan;
    @ApiModelProperty("创建时间")
    private Date createDate;
    @ApiModelProperty("更新时间")
    private Date modifyDate;

    @ApiModelProperty("附件集合")
    private List<FileVO> files;

}
