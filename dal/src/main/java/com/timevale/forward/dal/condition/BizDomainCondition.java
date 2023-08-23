package com.timevale.forward.dal.condition;

import com.timevale.forward.dal.annotation.WildcardEscape;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Data
@Builder
public class BizDomainCondition{

    /**
     * names
     */
    @WildcardEscape
    private String name;

    /**
     * ownerIds
     */
    private List<String> ownerIds;

    /**
     * 上架状态：0-未上架，1-已上架
     */
    private Integer listingStatus;

}
