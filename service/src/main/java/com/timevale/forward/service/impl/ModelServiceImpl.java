package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.ModelMapper;
import com.timevale.forward.dal.entity.ModelDO;
import com.timevale.forward.facade.api.client.ModelService;
import com.timevale.forward.facade.api.request.ModelAddReq;
import com.timevale.forward.facade.api.request.ModelModifyReq;
import com.timevale.forward.service.copy.ModelCopier;
import com.timevale.mandarin.common.annotation.RestService;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;

/**
 * @author by YangXu
 * @date 2021/12/13 17:07
 */
@Slf4j
@RestService
public class ModelServiceImpl implements ModelService {

    @Resource
    ModelMapper modelMapper;

    @Override
    public BaseResult<Boolean> add(ModelAddReq modelAddReq) {
        ModelDO modelDO = ModelCopier.INSTANCE.convert(modelAddReq);
        modelMapper.insert(modelDO);
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> update(ModelModifyReq modelModifyReq) {
        ModelDO modelDO = ModelCopier.INSTANCE.convert(modelModifyReq);
        modelMapper.update(modelDO);
        return BaseResult.success(true);
    }
}
