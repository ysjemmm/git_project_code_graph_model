package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@ApiModel("产品需求文档对象")
public class ProductDemandDocumentVO extends ToString {

    @ApiModelProperty("产品需求id")
    private Long id;

    @ApiModelProperty("产品需求主题")
    private String name;

    @ApiModelProperty("产品需求负责人id")
    private String ownerId;

    @ApiModelProperty("负责人")
    private String owner;

    @ApiModelProperty("需求描述")
    private String desc;

    @ApiModelProperty("附件")
    private List<FileVO> files;

}
