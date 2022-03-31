package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.BizDemandMapper;
import com.timevale.forward.dal.dao.ProductBizDemandMapper;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.dao.ProjectNodeMapper;
import com.timevale.forward.dal.entity.ProductBizDemandDO;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.dal.entity.ProjectNodeDO;
import com.timevale.forward.facade.api.client.DataCorrectService;
import com.timevale.forward.facade.api.request.DataModifyReq;
import com.timevale.forward.model.enums.DataCorrectTypeEnum;
import com.timevale.forward.model.enums.ProjectStatusEnum;
import com.timevale.forward.service.component.ProductDemandComponent;
import com.timevale.forward.service.component.ProjectComponent;
import com.timevale.mandarin.common.annotation.RestService;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;

import javax.annotation.Resource;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Slf4j
@RestService
public class DataCorrectServiceImpl implements DataCorrectService {

    @Resource
    private ProjectComponent projectComponent;

    @Resource
    private ProjectMapper projectMapper;

    @Resource
    private ProjectNodeMapper projectNodeMapper;

    @Resource
    private ProductDemandComponent productDemandComponent;

    @Resource
    private ProductBizDemandMapper productBizDemandMapper;

    @Resource
    private BizDemandMapper bizDemandMapper;


    @Override
    public BaseResult<Boolean> modify(DataModifyReq dataModifyReq) {
        if (DataCorrectTypeEnum.PROJECT.getCode().equals(dataModifyReq.getType())) {
            List<ProjectDO> projectDOList = projectMapper.getByIds(dataModifyReq.getIds());
            projectDOList.forEach(a -> {
                if (!ProjectStatusEnum.SUSPEND.getCode().equals(a.getStatus())) {
                    List<ProjectNodeDO> projectNodes = projectNodeMapper.get(a.getId());
                    projectComponent.fillInfo(projectNodes, a);
                    a.setRetainModifyDate(true);
                    projectMapper.update(a);
                    productDemandComponent.updateProductDemandStatus(a.getId(), a.getStatus(),true);
                }
            });
            log.info("数据订正,更新项目完成");
        } else if (DataCorrectTypeEnum.PRODUCT_DEMAND.getCode().equals(dataModifyReq.getType())) {
            List<ProjectDO> projectDOList = projectMapper.selectByProductDemandIdList(dataModifyReq.getIds());
            projectDOList.forEach(a -> {
                productDemandComponent.updateProductDemandStatus(a.getId(), a.getStatus(),true);
            });
            log.info("数据订正,更新产品需求完成");
        } else if (DataCorrectTypeEnum.BIZ_DEMAND.getCode().equals(dataModifyReq.getType())) {
            List<Long> bizDemandIds = dataModifyReq.getIds();
            Map<Integer, List<Long>> condition = new HashMap<>();
            bizDemandIds.forEach(a->{
                List<ProductBizDemandDO> productDemands = productBizDemandMapper.getByBizDemandId(a);
                productDemands.stream().map(ProductBizDemandDO::getStatus).min(Comparator.comparingInt(o -> o))
                        .ifPresent(minStauts -> productDemandComponent.processBizDemandStatus(condition, minStauts, a));
            });
            condition.forEach((k, v) -> {
                //更新产品需求下的所有业务需求状态
                bizDemandMapper.updateByIds(v, k,true);
            });
            log.info("数据订正,更新业务需求完成");
        }
        return BaseResult.success();
    }

    @Override
    public BaseResult<Boolean> calculateStatus() {
        List<Integer> status = Lists.newArrayList(ProjectStatusEnum.DEVING.getCode(), ProjectStatusEnum.TESTING.getCode());
        List<ProjectDO> list = projectMapper.getByStatus(status);
        list.forEach(a->{
            List<ProjectNodeDO> projectNodes = projectNodeMapper.get(a.getId());
            projectComponent.fillInfo(projectNodes, a);
            a.setRetainModifyDate(true);
            projectMapper.update(a);
            productDemandComponent.updateProductDemandStatus(a.getId(), a.getStatus(),true);
        });
        log.info("数据订正,状态变更完成");
        return BaseResult.success();
    }
}
