package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.dao.BizDemandMapper;
import com.timevale.forward.dal.dao.ProductBizDemandMapper;
import com.timevale.forward.dal.dao.ProductDemandMapper;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.entity.BizDemandDO;
import com.timevale.forward.dal.entity.ProductBizDemandDO;
import com.timevale.forward.dal.entity.ProductDemandDO;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.model.enums.BizDemandStatusEnum;
import com.timevale.forward.model.enums.ProductDemandStatusEnum;
import com.timevale.forward.service.component.BizDemandComponent;
import com.timevale.forward.service.component.MessageComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author by YangXu
 * @date 2022/01/04 10:29
 */
@Component
@Slf4j
public class BizDemandComponentImpl implements BizDemandComponent {

    @Resource
    BizDemandMapper bizDemandMapper;

    @Resource
    ProductDemandMapper productDemandMapper;

    @Resource
    ProjectMapper projectMapper;

    @Resource
    ProductBizDemandMapper productBizDemandMapper;

    @Resource
    MessageComponent messageComponent;

    @Override
    public void updateBizDemandStatusAsLinkProductDemand(Long bizDemandId) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        List<ProductBizDemandDO> productBizDemandDOList = productBizDemandMapper.getByBizDemandId(bizDemandId);
        List<Long> productDemandIdList = productBizDemandDOList.stream().map(ProductBizDemandDO::getProductDemandId).collect(Collectors.toList());
        List<ProductDemandDO> productDemandDOList = productDemandMapper.selectByIdList(productDemandIdList);

        // 筛出最小“未作废”产品需求状态
        Integer status = null;
        for (ProductDemandDO productDemandDO : productDemandDOList) {
            Integer productDemandStatus = productDemandDO.getStatus();
            if(productDemandStatus.equals(ProductDemandStatusEnum.INVALID.getCode())
                    || productDemandStatus.equals(ProductDemandStatusEnum.WAITING.getCode())){
                continue;
            }
            status = status == null ? productDemandStatus : Math.min(status, productDemandStatus);
        }

        // 根据产品需求状态判断业务需求状态
        int result;
        boolean notice = true;
        if(ProductDemandStatusEnum.INCLUDED.getCode().equals(status)){
            result = BizDemandStatusEnum.INCLUDE_PROJECT.getCode();
        }else if(ProductDemandStatusEnum.PROGRESS.getCode().equals(status)){
            result = BizDemandStatusEnum.PROJECTING.getCode();
        }else if(ProductDemandStatusEnum.ONLINE.getCode().equals(status)){
            result = BizDemandStatusEnum.AVAILABLE.getCode();
        }else{
            notice = false;
            result = BizDemandStatusEnum.RECEIVED.getCode();
        }

        BizDemandDO bizDemandDO = bizDemandMapper.selectById(bizDemandId);
        bizDemandDO.setStatus(result);
        bizDemandDO.setModifyMan(userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getId());
        bizDemandDO.setModifyManId(userInfo.getId());
        bizDemandMapper.update(bizDemandDO);

        if(notice){
            // 获取项目发布时间
            List<ProjectDO> projectDOList = projectMapper.selectByProductDemandIdList(productDemandIdList);
            Date date = projectDOList.get(0).getPlanEndDate();
            for (ProjectDO projectDO : projectDOList) {
                Date projectEndDate = projectDO.getActualEndDate() == null? projectDO.getPlanEndDate(): projectDO.getActualEndDate();
                date = date.after(projectEndDate)? date: projectEndDate;
            }
            // 钉钉通知
            messageComponent.bizDemandStatusChangeMsg(
                    bizDemandDO.getCreateManId(),
                    bizDemandDO.getName(),
                    BizDemandStatusEnum.getTextByCode(bizDemandDO.getStatus()),
                    date.toString());
        }
    }
}
