package com.timevale.forward.service.utils.duplicate;

import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.timevale.forward.dal.condition.BizDemandListCondition;
import com.timevale.forward.dal.condition.ProductDemandListCondition;
import com.timevale.forward.dal.entity.ProductDemandListDO;
import com.timevale.forward.facade.api.query.BizDemandQueryList;
import com.timevale.forward.facade.api.query.ProductDemandQueryList;
import com.timevale.forward.facade.api.result.BizDemandVO;
import com.timevale.forward.facade.api.result.BizLabelSimpleVO;
import com.timevale.forward.facade.api.result.ProductDemandVO;
import com.timevale.forward.model.enums.AscriptionEnum;
import com.timevale.forward.model.enums.BizTypeEnum;
import com.timevale.forward.model.enums.PriorityEnum;
import com.timevale.forward.model.enums.ProductDemandStatusEnum;
import com.timevale.forward.model.enums.ProductDemandTypeEnum;
import com.timevale.forward.service.component.BizLabelComponent;
import com.timevale.forward.service.copy.ProductDemandCopier;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.common.result.PageQueryResult;
import com.timevale.security.facade.response.BaseInfoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Component
public class GroupDuplicateUtil {

    private final BizLabelComponent bizLabelComponent;

    private final InnerUserPersonClient innerUserPersonClient;

    public void sortByCreateDate(List<BizDemandVO> resultList) {
        final Date BIG_DATE = new Date(Long.MAX_VALUE);
        resultList.sort(Comparator.<BizDemandVO, Integer>comparing(x -> x.getProjectId() != null ? 0 : 1)
                .thenComparing(x -> Optional.ofNullable(x.getProjectCreateDate()).orElse(BIG_DATE))
                .thenComparing(BizDemandVO::getCreateDate)
                .reversed());
    }

    public boolean isResultIsEmpty(BizDemandQueryList bizDemandQueryList, BizDemandListCondition condition, String userId) {
        // 标志是否有对应数据
        boolean resultIsEmpty = false;

        // 根据tabs添加不同的效果
        String ascription = bizDemandQueryList.getAscription();
        if (AscriptionEnum.CURRENT_USER.toString().equals(ascription)) {
            condition.setSubmitManIdList(Lists.newArrayList(userId));
        } else if (AscriptionEnum.RECEIVE.toString().equals(ascription)) {
            condition.setReceiveManIdList(Lists.newArrayList(userId));
        } else if (AscriptionEnum.COPIER.toString().equals(ascription)) {
            condition.setCopier(userId);
        } else {
            List<String> teamMemberIdList = innerUserPersonClient.getAllMyStaffWithSelf(userId, true);
            if (AscriptionEnum.TEAM_SUBMIT.toString().equals(ascription)) {
                Set<String> createIdSet = new HashSet<>(condition.getSubmitManIdList());
                if (!createIdSet.isEmpty()) {
                    teamMemberIdList = teamMemberIdList.stream().filter(createIdSet::contains).collect(Collectors.toList());
                    resultIsEmpty = teamMemberIdList.isEmpty();
                }
                condition.setSubmitManIdList(teamMemberIdList);
            } else if (AscriptionEnum.TEAM_RECEIVE.toString().equals(ascription)) {
                Set<String> receiveIdSet = new HashSet<>(condition.getReceiveManIdList());
                if (!receiveIdSet.isEmpty()) {
                    teamMemberIdList = teamMemberIdList.stream().filter(receiveIdSet::contains).collect(Collectors.toList());
                    resultIsEmpty = teamMemberIdList.isEmpty();
                }
                condition.setReceiveManIdList(teamMemberIdList);
            }
        }
        return resultIsEmpty;
    }

    public PageQueryResult<ProductDemandVO> getDemandVOQueryResultVO(List<ProductDemandListDO> productDemandListDO) {
        List<ProductDemandVO> productDemandVOList = ProductDemandCopier.INSTANCE.convert(productDemandListDO);

        if (CollectionUtils.isEmpty(productDemandVOList)) {
            return ResultUtil.pageEmpty();
        }
        List<Long> productDemandIds = productDemandListDO.stream().map(ProductDemandListDO::getId).collect(Collectors.toList());

        Map<Long, List<BizLabelSimpleVO>> bizLabelMap = bizLabelComponent.getBizLabelMap(productDemandIds, BizTypeEnum.PRODUCT_DEMAND.getCode());

        for (ProductDemandVO a : productDemandVOList) {
            a.setStatusName(ProductDemandStatusEnum.getTextByCode(a.getStatus()));
            a.setPriorityName(PriorityEnum.getTextByCode(a.getPriority()));

            List<BizLabelSimpleVO> labelSimpleVOList = bizLabelMap.get(a.getId());
            if (CollectionUtils.isNotEmpty(labelSimpleVOList)) {
                a.setLabelNames(labelSimpleVOList);
            }

            String typeName = a.getType().stream()
                    .map(ProductDemandTypeEnum::getTextByCode)
                    .collect(Collectors.joining(","));
            a.setTypeName(typeName);
        }
        // 分页数据
        PageInfo<ProductDemandListDO> pageInfo = new PageInfo<>(productDemandListDO);
        PageQueryResult<ProductDemandVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(productDemandVOList);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);

        return pageQueryResult;
    }

    public boolean setOwnerIdByAscription(ProductDemandQueryList productDemandQueryList, String userId, ProductDemandListCondition condition, InnerUserPersonClient innerUserPersonClient) {
        if (AscriptionEnum.CURRENT_USER.name().equals(productDemandQueryList.getAscription())) {
            condition.getOwnerIds().add(userId);
        } else if (AscriptionEnum.TEAM.name().equals(productDemandQueryList.getAscription())) {
            List<String> allMyStaffWithSelf = innerUserPersonClient.getAllMyStaffWithSelf(userId, true);
            log.info("我和我的下属:{}", allMyStaffWithSelf);
            if (!CollectionUtils.isEmpty(productDemandQueryList.getOwnerIds())) {
                allMyStaffWithSelf.retainAll(productDemandQueryList.getOwnerIds());
                log.info("我和我的下属,过滤后:{}", allMyStaffWithSelf);
            }
            if (CollectionUtils.isEmpty(allMyStaffWithSelf)) {
                //所选人员不在我的团队中
                return true;
            }
            condition.setOwnerIds(allMyStaffWithSelf);
        } else if (AscriptionEnum.DEPARTMENT.name().equals(productDemandQueryList.getAscription())) {
            List<BaseInfoResponse> baseInfos = innerUserPersonClient.getPersonByAccountNew(Lists.newArrayList(userId));

            String groupId = baseInfos.get(0).getDefaultGroup().getGroupId();
            List<String> accountIds = innerUserPersonClient.getAllByGroupId(groupId);
            log.info("用户默认部门id:{},同部门人员:{}", groupId, accountIds);
            if (!CollectionUtils.isEmpty(productDemandQueryList.getOwnerIds())) {
                accountIds.retainAll(productDemandQueryList.getOwnerIds());
                log.info("用户默认部门id:{},过滤后:{}", groupId, accountIds);
            }
            if (CollectionUtils.isEmpty(accountIds)) {
                //所选人员不在我的部门中
                return true;
            }
            condition.setOwnerIds(accountIds);
        } else if (AscriptionEnum.COPIER.name().equals(productDemandQueryList.getAscription())) {
            condition.setCopierId(userId);
        }
        return false;
    }
}
