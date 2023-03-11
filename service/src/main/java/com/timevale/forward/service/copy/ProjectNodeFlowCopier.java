package com.timevale.forward.service.copy;

import com.alibaba.fastjson.JSONObject;
import com.timevale.forward.dal.entity.ProjectNodeFlowDO;
import com.timevale.forward.facade.api.request.PersonAddReq;
import com.timevale.forward.facade.api.request.ProjectNodeFlowAddReq;
import com.timevale.forward.facade.api.result.ProjectNodeFlowDetailVO;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.util.Lists;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

@Mapper
public interface ProjectNodeFlowCopier {

    ProjectNodeFlowCopier INSTANCE = Mappers.getMapper(ProjectNodeFlowCopier.class);

    /**
     * 转换转换DO
     *
     * @param projectNodeFlowAddReq projectNodeFlowAddReq 对象
     * @return ProjectFlowDO
     */
    @Mapping(source = "pd.userName", target = "pd")
    @Mapping(source = "pd.userId", target = "pdId")
    @Mapping(source = "po.userName", target = "po")
    @Mapping(source = "po.userId", target = "poId")
    @Mapping(source = "d.userName", target = "d")
    @Mapping(source = "d.userId", target = "did")
    @Mapping(source = "bis", target = "biz", qualifiedByName = "bizMappingStr")
    @Mapping(source = "bis", target = "bizId", qualifiedByName = "bizIdMappingStr")
    ProjectNodeFlowDO convert(ProjectNodeFlowAddReq projectNodeFlowAddReq);

    @Named("bizMappingStr")
    default String bizMappingStr(List<PersonAddReq> bis){
        if(CollectionUtils.isEmpty(bis)){
            return StringUtils.EMPTY;
        }
        List<String> result = bis.stream().map(PersonAddReq::getUserName).collect(Collectors.toList());
        return JSONObject.toJSONString(result);
    }

    @Named("bizIdMappingStr")
    default String bizIdMappingStr(List<PersonAddReq> bis){
        if(CollectionUtils.isEmpty(bis)){
            return StringUtils.EMPTY;
        }
        List<String> result = bis.stream().map(PersonAddReq::getUserId).collect(Collectors.toList());
        return JSONObject.toJSONString(result);
    }

    @Mapping(source = "unreviewed", target = "unrevieweds", qualifiedByName = "unreviewedMappingList")
    @Mapping(source = "reviewFail", target = "reviewFails", qualifiedByName = "reviewFailMappingList")
    @Mapping(source = "biz", target = "bis", qualifiedByName = "bizMappingList")
    ProjectNodeFlowDetailVO convert(ProjectNodeFlowDO projectNodeFlowDO);

    @Named("unreviewedMappingList")
    default List<String> unreviewedMappingList(String unrevieweds){
        if(StringUtils.isEmpty(unrevieweds)){
            return Lists.newArrayList();
        }
        return JSONObject.parseArray(unrevieweds,String.class);
    }

    @Named("reviewFailMappingList")
    default List<String> reviewFailMappingList(String reviewFails){
        if(StringUtils.isEmpty(reviewFails)){
            return Lists.newArrayList();
        }
        return JSONObject.parseArray(reviewFails,String.class);
    }

    @Named("bizMappingList")
    default List<String> bizMappingList(String biz){
        if(StringUtils.isEmpty(biz)){
            return Lists.newArrayList();
        }
        return JSONObject.parseArray(biz,String.class);
    }
}
