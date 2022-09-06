package com.timevale.forward.dal.condition;

import com.timevale.forward.dal.annotation.WildcardEscape;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Data
@Builder
public class ModelCondition {

    /**
     * names
     */
    @WildcardEscape
    private String name;

    /**
     * ownerIds
     */
    private List<String> ownerIds;

}
