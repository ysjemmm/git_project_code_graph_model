package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author mayang
 * @date 2025-10-15 19:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@ApiModel("产品需求详情-资源评估")
public class SimpleResourcePlanItemVO extends ToString {
    @ApiModelProperty("产品需求-负责人列表")
    private List<PersonVO> owners;

    @ApiModelProperty("关联的资源类型")
    private SimpleResourceType resourceType;

    @ApiModelProperty("关联的需求评估（人天）")
    private BigDecimal resourceTime;

    @Getter
    @AllArgsConstructor
    public enum SimpleResourceType {
        FRONTEND("frontTime"),
        BACKEND("backTime"),
        QA("qaTime"),
        PRODUCT("productTime"),
        UED("uedTime"),
        OPS("opsTime"),
        SECURITY("securityTime");

        private final String timeLabel;

        public static SimpleResourceType fromResourceType (@NonNull String resourceType) {
            switch (resourceType) {
                case "frontend":
                    return FRONTEND;
                case "backend":
                    return BACKEND;
                case "test":
                    return QA;
                case "ued":
                    return UED;
                case "product":
                    return PRODUCT;
                case "ops":
                    return OPS;
                case "security":
                    return SECURITY;
                default:
                    throw new IllegalArgumentException("未知的 resourceType 属性");
            }
        }
    }

    public static List<SimpleResourcePlanItemVO> defaultWithAllType () {
        return Arrays.stream(SimpleResourceType.values()).map(type -> new SimpleResourcePlanItemVO()
                .setResourceType(type)
                .setResourceTime(new BigDecimal("0"))
                .setOwners(new ArrayList<>())).collect(Collectors.toList());
    }

    public static SimpleResourcePlanItemVO create (@NonNull String resourceType) {
        return new SimpleResourcePlanItemVO()
                .setResourceType(SimpleResourceType.fromResourceType(resourceType))
                .setResourceTime(new BigDecimal("0"))
                .setOwners(new ArrayList<>());
    }

}
