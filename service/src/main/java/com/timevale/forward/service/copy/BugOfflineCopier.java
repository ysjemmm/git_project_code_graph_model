package com.timevale.forward.service.copy;

import com.timevale.forward.dal.condition.BugOfflineGroupCondition;
import com.timevale.forward.dal.condition.BugOfflineListCondition;
import com.timevale.forward.dal.dto.BugOfflineBelongDistributionDTO;
import com.timevale.forward.dal.dto.BugOfflineCountDTO;
import com.timevale.forward.dal.dto.BugOfflineReasonDistributionDTO;
import com.timevale.forward.dal.entity.BugOfflineDO;
import com.timevale.forward.dal.entity.BugOfflineListDO;
import com.timevale.forward.facade.api.query.BugOfflineGroupList;
import com.timevale.forward.facade.api.query.BugOfflineQueryList;
import com.timevale.forward.facade.api.request.BugOfflineAddReq;
import com.timevale.forward.facade.api.request.BugOfflineModifyReq;
import com.timevale.forward.facade.api.result.*;
import com.timevale.forward.model.enums.BugBelongEnum;
import com.timevale.forward.model.enums.BugReasonEnum;
import com.timevale.forward.model.middle.BugOfflineMD;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author by YangXu
 * @date 2022/02/23 17:53
 */
@Mapper(imports = {BugReasonEnum.class, BugBelongEnum.class, StringUtils.class, Arrays.class, Collectors.class})
public interface BugOfflineCopier {

    BugOfflineCopier INSTANCE = Mappers.getMapper(BugOfflineCopier.class);

    /**
     * 转换转换DO
     *
     * @param bugOfflineQueryListt 对象
     * @return BugOfflineListCondition
     */
    BugOfflineListCondition convert(BugOfflineQueryList bugOfflineQueryListt);

    /**
     * 转换转换DO
     *
     * @param bugOfflineAddReq 对象
     * @return BugOfflineDO
     */
    BugOfflineDO convert(BugOfflineAddReq bugOfflineAddReq);

    /**
     * 转换DO
     *
     * @param bugOfflineModifyReq 对象
     * @return BugOfflineDO
     */
    BugOfflineDO convert(BugOfflineModifyReq bugOfflineModifyReq);

    /**
     * 转换转换DO
     *
     * @param bugOfflineListDO 对象
     * @return BugOfflineVO
     */
    BugOfflineVO convert(BugOfflineListDO bugOfflineListDO);

    /**
     * BugOfflineDO  -->  BugOfflineDetailVO
     *
     * @param bugOfflineDO 参数
     * @return BugOfflineDetailVO
     */
    @Mapping(source = "modifyDate", target = "lastModifyDate")
    @Mapping(source = "createDate", target = "submitDate")
    BugOfflineDetailVO transform(BugOfflineDO bugOfflineDO);

    /**
     * 转换转换MD
     *
     * @param bugOfflineDO 对象
     * @return BugOfflineVO
     */
    BugOfflineMD do2md(BugOfflineDO bugOfflineDO);


    @Mapping(target = "reasonName", expression = "java(BugReasonEnum.getTextByCode(req.getReason()))")
    BugOfflineReasonDistributionVO convert(BugOfflineReasonDistributionDTO req);

    @Mapping(target = "belongName", expression = "java(BugBelongEnum.getTextByCode(req.getBelong()))")
    BugOfflineBelongDistributionVO convert(BugOfflineBelongDistributionDTO req);

    List<BugOfflineCountVO> convertCount(List<BugOfflineCountDTO> countList);

    List<BugOfflineReasonDistributionVO> convertReasonDistributions(List<BugOfflineReasonDistributionDTO> req);
    List<BugOfflineBelongDistributionVO> convertBelongDistributions(List<BugOfflineBelongDistributionDTO> req);

    /**
     * 转换分组筛选条件
     *
     * @param queryList 对象
     * @return BugOfflineGroupCondition
     */
    @Mapping(target = "notInLabelIds", expression = "java(StringUtils.isNotEmpty(queryList.getNotInLabelIds()) ? Arrays.stream(queryList.getNotInLabelIds().split(\",\")).map(Long::valueOf).collect(Collectors.toList()) : null)")
    @Mapping(target = "notInOperatorIds", expression = "java(StringUtils.isNotEmpty(queryList.getNotInOperatorIds()) ? Arrays.asList(queryList.getNotInOperatorIds().split(\",\")) : null)")
    @Mapping(target = "notInProposerIds", expression = "java(StringUtils.isNotEmpty(queryList.getNotInProposerIds()) ? Arrays.asList(queryList.getNotInProposerIds().split(\",\")) : null)")
    BugOfflineGroupCondition convert(BugOfflineGroupList queryList);
}
