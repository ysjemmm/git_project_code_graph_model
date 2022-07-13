package com.timevale.forward.service.component.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageInfo;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.CustomDemandListCondition;
import com.timevale.forward.dal.dao.CustomDemandMapper;
import com.timevale.forward.dal.entity.CustomDemandDO;
import com.timevale.forward.facade.api.result.CustomDemandVO;
import com.timevale.forward.model.bo.ProductEndBO;
import com.timevale.forward.model.enums.BizDemandStatusEnum;
import com.timevale.forward.model.enums.ProblemTypeEnum;
import com.timevale.forward.service.component.CustomDemandComponent;
import com.timevale.forward.service.copy.CustomDemandCopier;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.date.DateUtil;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author xingyun
 * @date 2021-12-16 11:37
 **/
@Component
@Slf4j
public class CustomDeamndComponentImpl implements CustomDemandComponent {


    @Resource
    private CustomDemandMapper customDemandMapper;

    @Value("${custom.demand.receiver}")
    private String receiver;

    @Override
    public BaseResult<PageQueryResult<CustomDemandVO>> list(CustomDemandListCondition condition) {
        // 分页数据
        buildConditionBeforeQuery(condition);
        List<CustomDemandDO> customDemandDOList;
        if(condition.getProductDemandId()==null){
            customDemandDOList = customDemandMapper.list(condition);
        }else {
            customDemandDOList = customDemandMapper.linkCustomDemandList(condition.getProductDemandId());
        }
        List<CustomDemandVO> customDemandVOList = CustomDemandCopier.INSTANCE.convert(customDemandDOList);

        List<ProductEndBO> receivers = JSONObject.parseArray(receiver, ProductEndBO.class);
        Map<Integer, ProductEndBO> receiverMap = receivers.stream().collect(Collectors.toMap(ProductEndBO::getCode, k -> k, (v1, v2) -> v2));
        customDemandVOList.forEach(a->{
            a.setProductEndText(receiverMap.get(a.getProductEnd()).getName());
            a.setStatusText(BizDemandStatusEnum.getTextByCode(a.getStatus()));
            a.setCauseText(ProblemTypeEnum.getTextByCode(a.getCause()));
        });

        PageInfo<CustomDemandDO> pageInfo = new PageInfo<>(customDemandDOList);
        PageQueryResult<CustomDemandVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(customDemandVOList);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);
        return BaseResult.success(pageQueryResult);
    }

    private void buildConditionBeforeQuery(CustomDemandListCondition condition) {
        condition.setCreateDateStart(DateUtil.getStartOfDay(condition.getCreateDateStart()));
        condition.setCreateDateEnd(DateUtil.getEndOfDay(condition.getCreateDateEnd()));
        condition.setProjectEndDateStart(DateUtil.getStartOfDay(condition.getProjectEndDateStart()));
        condition.setProjectEndDateEnd(DateUtil.getEndOfDay(condition.getProjectEndDateEnd()));
    }

}
