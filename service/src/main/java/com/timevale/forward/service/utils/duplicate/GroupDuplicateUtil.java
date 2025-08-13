package com.timevale.forward.service.utils.duplicate;

import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.timevale.forward.dal.condition.ProductDemandListCondition;
import com.timevale.forward.dal.entity.ProductDemandListDO;
import com.timevale.forward.facade.api.query.ProductDemandQueryList;
import com.timevale.forward.facade.api.result.BizLabelSimpleVO;
import com.timevale.forward.facade.api.result.ProductDemandVO;
import com.timevale.forward.facade.api.result.ProductLineAnalyseVO;
import com.timevale.forward.facade.api.result.QueryResultVO;
import com.timevale.forward.model.enums.AscriptionEnum;
import com.timevale.forward.model.enums.BizTypeEnum;
import com.timevale.forward.model.enums.PriorityEnum;
import com.timevale.forward.model.enums.ProductDemandStatusEnum;
import com.timevale.forward.model.enums.ProductDemandTypeEnum;
import com.timevale.forward.service.component.BizLabelComponent;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.common.result.PageQueryResult;
import com.timevale.security.facade.response.BaseInfoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Component
public class GroupDuplicateUtil {

    private final BizLabelComponent bizLabelComponent;

    public QueryResultVO<ProductDemandVO> getDemandVOQueryResultVO(List<ProductDemandListDO> productDemandListDO, List<ProductDemandVO> productDemandVOList, List<ProductLineAnalyseVO> analyseVOList) {
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

        QueryResultVO<ProductDemandVO> queryResultVO = new QueryResultVO<>();
        queryResultVO.setPageQueryResult(pageQueryResult);
        queryResultVO.setAnalyseVOList(analyseVOList);
        return queryResultVO;
    }

    public List<ProductLineAnalyseVO> getProductLineAnalyseVOS(List<ProductDemandListDO> allProductDemandListDO) {
        Map<Long, List<ProductDemandListDO>> bizDemandListDOMap = allProductDemandListDO.stream().collect(Collectors.groupingBy(ProductDemandListDO::getProductLineId));
        log.info("业务查询产品线分析：{}", bizDemandListDOMap);

        List<ProductLineAnalyseVO> analyseVOList = new ArrayList<>();
        bizDemandListDOMap.forEach((k, v) -> {
            ProductLineAnalyseVO analyseVO = new ProductLineAnalyseVO();
            Optional<ProductDemandListDO> any = v.stream().findAny();
            any.ifPresent(e -> {
                analyseVO.setCount(v.size());
                analyseVO.setProductLineId(e.getProductLineId());
                analyseVO.setProductLineName(e.getProductLineName());
                analyseVOList.add(analyseVO);
            });
        });
        //逆序排序
        analyseVOList.sort((a, b) -> b.getCount().compareTo(a.getCount()));
        return analyseVOList;
    }

    public boolean setOwnerIdByAscription(ProductDemandQueryList productDemandQueryList, UserInfo userInfo, ProductDemandListCondition condition, InnerUserPersonClient innerUserPersonClient) {
        if (AscriptionEnum.CURRENT_USER.name().equals(productDemandQueryList.getAscription())) {
            condition.getOwnerIds().add(userInfo.getId());
        } else if (AscriptionEnum.TEAM.name().equals(productDemandQueryList.getAscription())) {
            List<String> allMyStaffWithSelf = innerUserPersonClient.getAllMyStaffWithSelf(userInfo.getId(), true);
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
            List<BaseInfoResponse> baseInfos = innerUserPersonClient.getPersonByAccountNew(Lists.newArrayList(userInfo.getId()));

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
            condition.setCopierId(userInfo.getId());
        }
        return false;
    }
}
