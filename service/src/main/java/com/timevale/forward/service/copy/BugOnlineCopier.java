package com.timevale.forward.service.copy;

import com.timevale.forward.dal.condition.BugOnlineListCondition;
import com.timevale.forward.dal.entity.BugOnlineDO;
import com.timevale.forward.dal.entity.BugOnlineListDO;
import com.timevale.forward.facade.api.query.BugOnlineQueryList;
import com.timevale.forward.facade.api.request.BugOnlineAddReq;
import com.timevale.forward.facade.api.request.BugOnlineModifyReq;
import com.timevale.forward.facade.api.request.BugOnlinePriorityGetReq;
import com.timevale.forward.facade.api.result.BugOnlineDetailVO;
import com.timevale.forward.facade.api.result.BugOnlineSimpleVO;
import com.timevale.forward.facade.api.result.BugOnlineVO;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.model.middle.BugOnlineMD;
import com.timevale.forward.service.utils.date.DateUtil;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.Collection;
import java.util.List;

/**
 * @Date 2022/3/18 13:54
 * @Author 望轩
 */
@Mapper(
        imports = {
                DateUtil.class,
                BugOnlineStatusEnum.class,
                BugOnlineEnvEnum.class,
                BugOnlineSourceEnum.class,
                BugOnlineBeloneEnum.class,
                BugOnlinePriorityEnum.class,
                BugOnlineReasonEnum.class,
                BugOnlineRecurrentEnum.class,
                BugOnlineReasonStageEnum.class,
                BugOnlineCategoryEnum.class,
                CustomerCountEnum.class,
                UserCountEnum.class,
                ProblemOccurredTimeEnum.class,
                BugOnlineGenerationStageEnum.class
        }
)
public interface BugOnlineCopier {
    BugOnlineCopier INSTANCE = Mappers.getMapper(BugOnlineCopier.class);

    /**
     * BugOnlineDO --> BugOnlineDetailVO
     *
     * @param bugDO 对象
     * @return BugOfflineListCondition
     */
    @Mapping(target = "envName", expression = "java(BugOnlineEnvEnum.getTextByCode(bugDO.getEnv()))")
    @Mapping(target = "statusName", expression = "java(BugOnlineStatusEnum.getTextByCode(bugDO.getStatus()))")
    @Mapping(target = "sourceName", expression = "java(BugOnlineSourceEnum.getTextByCode(bugDO.getSource()))")
    @Mapping(target = "belongName", expression = "java(BugOnlineBeloneEnum.getTextByCode(bugDO.getBelong()))")
    @Mapping(target = "categoryName", expression = "java(BugOnlineCategoryEnum.getTextByCode(bugDO.getCategory()))")
    @Mapping(target = "priorityName", expression = "java(BugOnlinePriorityEnum.getTextByCode(bugDO.getPriority()))")
    @Mapping(target = "recurrentName", expression = "java(BugOnlineRecurrentEnum.getTextByCode(bugDO.getRecurrent()))")
    @Mapping(target = "reasonName", expression = "java(BugOnlineReasonEnum.getTextByCode(bugDO.getReason()))")
    @Mapping(target = "reasonStageName", expression = "java(BugOnlineReasonStageEnum.getTextByCode(bugDO.getReasonStage()))")
    @Mapping(target = "dismissCauseName", expression = "java(BugOnlineReasonEnum.getTextByCode(bugDO.getDismissCause()))")
    @Mapping(target = "dismissCauseStageName", expression = "java(BugOnlineReasonStageEnum.getTextByCode(bugDO.getDismissCauseStage()))")
    @Mapping(target = "customerCountName", expression = "java(CustomerCountEnum.getByTextCode(bugDO.getCustomerCount()))")
    @Mapping(target = "userCountName", expression = "java(UserCountEnum.getTextByCode(bugDO.getUserCount()))")
    @Mapping(target = "problemOccurredTimeName", expression = "java(ProblemOccurredTimeEnum.getTextByCode(bugDO.getProblemOccurredTime()))")
    @Mapping(target = "generationStageName", expression = "java(BugOnlineGenerationStageEnum.getTextByCode(bugDO.getGenerationStage()))")
    BugOnlineDetailVO convert(BugOnlineDO bugDO);

    /**
     * BugOnlineListDO --> BugOnlineVO
     *
     * @param listDO 对象
     * @return BugOnlineVO
     */
    @Mapping(target = "slaRemainHours", ignore = true)
    @Mapping(target = "envName", expression = "java(BugOnlineEnvEnum.getTextByCode(listDO.getEnv()))")
    @Mapping(target = "statusName", expression = "java(BugOnlineStatusEnum.getTextByCode(listDO.getStatus()))")
    @Mapping(target = "sourceName", expression = "java(BugOnlineSourceEnum.getTextByCode(listDO.getSource()))")
    @Mapping(target = "belongName", expression = "java(BugOnlineBeloneEnum.getTextByCode(listDO.getBelong()))")
    @Mapping(target = "categoryName", expression = "java(BugOnlineCategoryEnum.getTextByCode(listDO.getCategory()))")
    @Mapping(target = "priorityName", expression = "java(BugOnlinePriorityEnum.getTextByCode(listDO.getPriority()))")
    @Mapping(target = "reasonName", expression = "java(BugOnlineReasonEnum.getTextByCode(listDO.getReason()))")
    @Mapping(target = "reasonStageName", expression = "java(BugOnlineReasonStageEnum.getTextByCode(listDO.getReasonStage()))")
    @Mapping(target = "dismissCauseName", expression = "java(BugOnlineReasonEnum.getTextByCode(listDO.getDismissCause()))")
    @Mapping(target = "dismissCauseStageName", expression = "java(BugOnlineReasonStageEnum.getTextByCode(listDO.getDismissCauseStage()))")
    BugOnlineVO convert(BugOnlineListDO listDO);

    /**
     * bugOnlineDO --> BugOnlineVO
     *
     * @param bugOnlineDO 对象
     * @return BugOnlineVO
     */
    BugOnlineVO convertT(BugOnlineDO bugOnlineDO);

    /**
     * bugOnlineQueryList --> BugOnlineListCondition
     *
     * @param query 对象
     * @return BugOnlineListCondition
     */
    @Mapping(target = "createDateLeft", expression = "java(DateUtil.getStartOfDay(query.getCreateDateLeft()))")
    @Mapping(target = "createDateRight", expression = "java(DateUtil.getEndOfDay(query.getCreateDateRight()))")
    @Mapping(target = "modifyDateLeft", expression = "java(DateUtil.getStartOfDay(query.getModifyDateLeft()))")
    @Mapping(target = "modifyDateRight", expression = "java(DateUtil.getEndOfDay(query.getModifyDateRight()))")
    BugOnlineListCondition convert(BugOnlineQueryList query);

    /**
     * BugOnlineAddReq --> BugOnlineDO
     *
     * @param bugOnlineAddReq 对象
     * @return BugOnlineDO
     */
    BugOnlineDO req2do(BugOnlineAddReq bugOnlineAddReq);

    /**
     * BugOnlineModifyReq --> BugOnlineDO
     *
     * @param bugOnlineModifyReq 参数
     * @return 返回参数
     */
    BugOnlineDO change(BugOnlineModifyReq bugOnlineModifyReq);

    /**
     * BugOnlineModifyReq --> BugOnlineMD
     *
     * @param req 参数
     */
    @Mapping(target = "reasonName", expression="java(BugOnlineReasonEnum.getFullTextByCode(req.getReason()))")
    @Mapping(target = "dismissCauseName", expression="java(BugOnlineReasonEnum.getFullTextByCode(req.getDismissCause()))")
    BugOnlineMD req2md(BugOnlineModifyReq req);

    /**
     * BugOnlineDO --> BugOnlineMD
     *
     * @param bugOnlineDO 参数
     * @return 返回值
     */
    @Mapping(target = "reasonName", expression="java(BugOnlineReasonEnum.getFullTextByCode(bugOnlineDO.getReason()))")
    @Mapping(target = "dismissCauseName", expression="java(BugOnlineReasonEnum.getFullTextByCode(bugOnlineDO.getDismissCause()))")
    BugOnlineMD do2md(BugOnlineDO bugOnlineDO);

    /**
     *
     * @param bugOnlineListDOList 参数
     * @return 返回值
     */
    List<BugOnlineVO> convert(List<BugOnlineListDO> bugOnlineListDOList);

    BugOnlineSimpleVO do2svo(BugOnlineDO bugOnlineDO);

    BugOnlinePriorityGetReq do2req(BugOnlineDO bugOnlineDO, Collection<Long> productLineIds);

    BugOnlineListCondition convert(BugOnlineListCondition condition);
}