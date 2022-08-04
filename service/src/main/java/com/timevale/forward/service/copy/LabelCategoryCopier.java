package com.timevale.forward.service.copy;

import com.alibaba.fastjson.JSONObject;
import com.timevale.forward.dal.condition.LabelCategoryListCondition;
import com.timevale.forward.dal.entity.LabelCategoryDO;
import com.timevale.forward.facade.api.query.LabelCategoryQueryList;
import com.timevale.forward.facade.api.query.LabelInCategoryQueryList;
import com.timevale.forward.facade.api.request.LabelCategoryAddReq;
import com.timevale.forward.facade.api.request.LabelCategoryModifyReq;
import com.timevale.forward.facade.api.result.LabelCategoryDetailVO;
import com.timevale.forward.facade.api.result.LabelCategorySimpleVO;
import com.timevale.forward.facade.api.result.LabelCategoryVO;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.util.Lists;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * @author by YangXu
 * @date 2021/12/15 10:30
 */
@Mapper
public interface LabelCategoryCopier {

    LabelCategoryCopier INSTANCE = Mappers.getMapper(LabelCategoryCopier.class);


    /**
     *
     * @param labelInCategoryQueryList labelInCategoryQueryList
     * @return LabelCategoryListCondition
     */
    LabelCategoryListCondition convert(LabelInCategoryQueryList labelInCategoryQueryList);

    /**
     *
     * @param labelCategoryQueryList labelCategoryQueryList
     * @return LabelCategoryListCondition
     */
    LabelCategoryListCondition convert(LabelCategoryQueryList labelCategoryQueryList);

    /**
     *
     * @param labelCategoryDOList labelCategoryDOList
     * @return LabelCategorySimpleVO
     */
    List<LabelCategorySimpleVO> convert(List<LabelCategoryDO> labelCategoryDOList);

    /**
     *
     * @param labelCategoryDOList labelCategoryDOList
     * @return LabelCategorySimpleVO
     */

    List<LabelCategoryVO> change(List<LabelCategoryDO> labelCategoryDOList);

    /**
     *
     * @param labelCategoryDOList labelCategoryDOList
     * @return LabelCategorySimpleVO
     */

    List<LabelCategorySimpleVO> changeT(List<LabelCategoryDO> labelCategoryDOList);


    /**
     *
     * @param labelCategoryDO labelCategoryDO
     * @return LabelCategoryVO
     */
    @Mapping(source = "type", target = "types", qualifiedByName = "typeMappingList")
    @Mapping(source = "markMan", target = "markMans", qualifiedByName = "markManMappingList")
    LabelCategoryVO change(LabelCategoryDO labelCategoryDO);
    /**
     *
     * @param labelCategoryDO labelCategoryDO
     * @return LabelCategoryDetailVO
     */
    @Mapping(source = "type", target = "types", qualifiedByName = "typeMappingList")
    @Mapping(source = "deptId", target = "deptIds", qualifiedByName = "deptMappingList")
    @Mapping(source = "markMan", target = "markMans", qualifiedByName = "markManMappingList")
    @Mapping(source = "markManId", target = "markManIds", qualifiedByName = "markManIdMappingList")
    LabelCategoryDetailVO convert(LabelCategoryDO labelCategoryDO);
    /**
     *
     * @param labelCategoryAddReq labelCategoryAddReq
     * @return LabelCategoryDO
     */
    @Mapping(source = "types", target = "type", qualifiedByName = "typeMappingStr")
    @Mapping(source = "deptIds", target = "deptId", qualifiedByName = "deptMappingStr")
    @Mapping(source = "markMans", target = "markMan", qualifiedByName = "markManMappingStr")
    @Mapping(source = "markManIds", target = "markManId", qualifiedByName = "markManIdMappingStr")
    LabelCategoryDO convert(LabelCategoryAddReq labelCategoryAddReq);

    /**
     *
     * @param labelCategoryModifyReq labelCategoryModifyReq
     * @return LabelCategoryDO
     */
    @Mapping(source = "types", target = "type", qualifiedByName = "typeMappingStr")
    @Mapping(source = "deptIds", target = "deptId", qualifiedByName = "deptMappingStr")
    @Mapping(source = "markMans", target = "markMan", qualifiedByName = "markManMappingStr")
    @Mapping(source = "markManIds", target = "markManId", qualifiedByName = "markManIdMappingStr")
    LabelCategoryDO convert(LabelCategoryModifyReq labelCategoryModifyReq);

    @Named("typeMappingStr")
    default String typeMappingStr(List<Integer> types){
        if(CollectionUtils.isEmpty(types)){
            return StringUtils.EMPTY;
        }
        return JSONObject.toJSONString(types);
    }

    @Named("deptMappingStr")
    default String deptMappingStr(List<Long> deptIds){
        if(CollectionUtils.isEmpty(deptIds)){
            return StringUtils.EMPTY;
        }
        return JSONObject.toJSONString(deptIds);
    }

    @Named("markManMappingStr")
    default String markManMappingStr(List<String> markMans){
        if(CollectionUtils.isEmpty(markMans)){
            return StringUtils.EMPTY;
        }
        return JSONObject.toJSONString(markMans);
    }

    @Named("markManIdMappingStr")
    default String markManIdMappingStr(List<String> markManIds){
        if(CollectionUtils.isEmpty(markManIds)){
            return StringUtils.EMPTY;
        }
        return JSONObject.toJSONString(markManIds);
    }

    @Named("typeMappingList")
    default List<Integer> typeMappingList(String type){
        if(StringUtils.isEmpty(type)){
            return Lists.emptyList();
        }
        return JSONObject.parseArray(type,Integer.class);
    }

    @Named("deptMappingList")
    default List<Long> deptMappingList(String deptId){
        if(StringUtils.isEmpty(deptId)){
            return Lists.emptyList();
        }
        return JSONObject.parseArray(deptId,Long.class);
    }
    @Named("markManMappingList")
    default List<String> markManMappingList(String markMan){
        if(StringUtils.isEmpty(markMan)){
            return Lists.emptyList();
        }
        return JSONObject.parseArray(markMan,String.class);
    }
    @Named("markManIdMappingList")
    default List<String> markManIdMappingList(String markManId){
        if(StringUtils.isEmpty(markManId)){
            return Lists.emptyList();
        }
        return JSONObject.parseArray(markManId,String.class);
    }


}
