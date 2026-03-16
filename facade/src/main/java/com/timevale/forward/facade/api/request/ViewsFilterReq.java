package com.timevale.forward.facade.api.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author qiyuan
 * create on 2025/7/14
 */
@Data
@ApiModel("视图过滤条件")
// 基类：包含公共字段（如 type），并定义多态类型
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,          // 使用字段名（如 "product"、"biz"）区分类型
        include = JsonTypeInfo.As.PROPERTY,  // 类型信息放在 JSON 的某个字段里（这里是 "viewsType"）
        property = "viewsType"                    // 指定 JSON 里的字段名（这里是 "viewsType"）
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = ViewsProductDemandReq.class, name = "11"),  // type="PRODUCT_DEMAND" → ViewsProductDemandReq
        @JsonSubTypes.Type(value = ViewsBizDemandReq.class, name = "10"),      // type="BIZ_DEMAND" → ViewsBizDemandReq
        @JsonSubTypes.Type(value = ViewsBugOfflineReq.class, name = "13")      // type="BUG_OFFLINE" → ViewsBugOfflineReq
})
public abstract class ViewsFilterReq {
    @JsonProperty("viewsType")
    @ApiModelProperty(value = "业务类型：10-业务需求，11-产品需求，12-项目，13-线下bug，14-线上bug", required = true)
    protected String viewsType;
}