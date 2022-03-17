package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.ImprovementMeasureMapper;
import com.timevale.forward.dal.entity.ImprovementMeasureDO;
import com.timevale.forward.facade.api.client.ImprovementMeasureService;
import com.timevale.forward.facade.api.query.ImprovementMeasureQueryList;
import com.timevale.forward.facade.api.request.ImprovementMeasureAddReq;
import com.timevale.forward.facade.api.request.ImprovementMeasureCompleteReq;
import com.timevale.forward.facade.api.request.ImprovementMeasureDeleteReq;
import com.timevale.forward.facade.api.request.ImprovementMeasureModifyReq;
import com.timevale.forward.facade.api.result.TroubleTicketVO;
import com.timevale.forward.model.enums.ImprovementMeasureStatusEnum;
import com.timevale.forward.service.copy.ImprovementMeasureCopier;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.common.annotation.RestService;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author by YangXu
 * @date 2022/03/16 17:54
 */
@Slf4j
@RestService
public class ImprovementMeasureServiceImpl implements ImprovementMeasureService {

    @Resource
    ImprovementMeasureMapper improvementMeasureMapper;

    @Override
    public BaseResult<Boolean> add(ImprovementMeasureAddReq improvementMeasureAddReq) {
        ImprovementMeasureDO improvementMeasureDO = ImprovementMeasureCopier.INSTANCE.convert(improvementMeasureAddReq);
        improvementMeasureMapper.insert(improvementMeasureDO);

        if(improvementMeasureDO.getTodo()){
            // 发送待办
        }

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> modify(ImprovementMeasureModifyReq improvementMeasureModifyReq) {
        return null;
    }

    @Override
    public BaseResult<Boolean> delete(ImprovementMeasureDeleteReq improvementMeasureDeleteReq) {
        // 查询是否有对应事项
        Long id = improvementMeasureDeleteReq.getId();
        ImprovementMeasureDO improvementMeasureDO = improvementMeasureMapper.selectById(id);
        if(improvementMeasureDO == null){
            throw new BaseBizRuntimeException("该事项不存在");
        }

        // 修改事项逻辑删除标志
        improvementMeasureDO.setIsDeleted(true);
        improvementMeasureMapper.update(improvementMeasureDO);

        // 待办处理

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> complete(ImprovementMeasureCompleteReq improvementMeasureCompleteReq) {
        // 查询是否有对应事项
        Long id = improvementMeasureCompleteReq.getId();
        ImprovementMeasureDO improvementMeasureDO = improvementMeasureMapper.selectById(id);
        if(improvementMeasureDO == null){
            throw new BaseBizRuntimeException("该事项不存在");
        }

        // 修改事项逻辑删除标志
        improvementMeasureDO.setStatus(ImprovementMeasureStatusEnum.COMPLETED.getCode());
        improvementMeasureMapper.update(improvementMeasureDO);

        // 待办处理

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<List<TroubleTicketVO>> list(ImprovementMeasureQueryList improvementMeasureQueryList) {
        return null;
    }
}
