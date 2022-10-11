package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.dao.BizDemandCustomMapper;
import com.timevale.forward.dal.entity.BizDemandCustomDO;
import com.timevale.forward.facade.api.request.BizDemandCustomAddReq;
import com.timevale.forward.service.component.BizDemandCustomComponent;
import com.timevale.forward.service.copy.BizDemandCustomCopier;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.List;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

/**
 * 业务需求和客户关联
 * @author yexuan
 * @date 2022-09-23 15:27 yexuan
 */
@Component
@Slf4j
public class BizDemandCustomComponentImpl implements BizDemandCustomComponent {

    @Resource
    private BizDemandCustomMapper bizDemandCustomMapper;

    @Override
    public void add(List<BizDemandCustomAddReq> addReqList,Long bizDemandId) {
        log.info("新增时,业务需求和客户关联接收参数:list={}", addReqList);
        if(CollectionUtils.isEmpty(addReqList)){
            return;
        }
        List<BizDemandCustomDO> bizDemandCustomDOList = BizDemandCustomCopier.INSTANCE.convertList(addReqList);

        bizDemandCustomDOList.forEach(b->{
            b.setBizDemandId(bizDemandId);
        });
        bizDemandCustomMapper.batchInsert(bizDemandCustomDOList);
    }

    @Override
    public void update(List<BizDemandCustomAddReq> addReqList, Long bizDemandId) {
        log.info("修改时,业务需求和客户关联接收参数:list={}", addReqList);
        BizDemandCustomDO bizDemandCustomDO = new BizDemandCustomDO();
        bizDemandCustomDO.setBizDemandId(bizDemandId);
        bizDemandCustomMapper.delete(bizDemandCustomDO);
        add(addReqList,bizDemandId);
    }

    @Override
    public List<BizDemandCustomDO> selectByBizDemandId(Long bizDemandId) {
        return bizDemandCustomMapper.selectByBizDemandId(bizDemandId);
    }
}
