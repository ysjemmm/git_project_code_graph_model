package com.timevale.forward.service.copy;

import com.alibaba.fastjson.JSONObject;
import com.timevale.forward.dal.entity.ProjectFlowDO;
import com.timevale.forward.facade.api.request.PersonAddReq;
import com.timevale.forward.facade.api.request.ProjectFlowAddReq;
import com.timevale.forward.facade.api.result.ProjectFlowDetailVO;
import com.timevale.forward.facade.api.result.ProjectFlowDocumentVO;
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
public interface ProjectFlowCopier {

    ProjectFlowCopier INSTANCE = Mappers.getMapper(ProjectFlowCopier.class);

    /**
     * 转换转换DO
     *
     * @param projectFlowAddReq 对象
     * @return ProjectFlowDO
     */
    @Mapping(source = "proposer.userName", target = "proposer")
    @Mapping(source = "proposer.userId", target = "proposerId")
    @Mapping(source = "reviews", target = "review", qualifiedByName = "reviewMappingStr")
    @Mapping(source = "reviews", target = "reviewId", qualifiedByName = "reviewIdMappingStr")
    ProjectFlowDO convert(ProjectFlowAddReq projectFlowAddReq);

    @Named("reviewMappingStr")
    default String reviewMappingStr(List<PersonAddReq> reviews){
        if(CollectionUtils.isEmpty(reviews)){
            return StringUtils.EMPTY;
        }
        List<String> result = reviews.stream().map(PersonAddReq::getUserName).collect(Collectors.toList());
        return JSONObject.toJSONString(result);
    }

    @Named("reviewIdMappingStr")
    default String reviewIdMappingStr(List<PersonAddReq> reviews){
        if(CollectionUtils.isEmpty(reviews)){
            return StringUtils.EMPTY;
        }
        List<String> result = reviews.stream().map(PersonAddReq::getUserId).collect(Collectors.toList());
        return JSONObject.toJSONString(result);
    }

    @Mapping(source = "reviewed", target = "revieweds", qualifiedByName = "reviewedMappingList")
    @Mapping(source = "unreviewed", target = "unrevieweds", qualifiedByName = "unreviewedMappingList")
    @Mapping(source = "reviewFail", target = "reviewFails", qualifiedByName = "reviewFailMappingList")
    ProjectFlowDetailVO convert(ProjectFlowDO projectFlowDO);

    @Named("reviewedMappingList")
    default List<String> reviewedMappingList(String reviewed){
        if(StringUtils.isEmpty(reviewed)){
            return Lists.newArrayList();
        }
        return JSONObject.parseArray(reviewed,String.class);
    }
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

    @Mapping(target = "files", ignore = true)
    ProjectFlowDocumentVO convert2Document(ProjectFlowDO projectFlowDO);
}
