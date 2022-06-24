package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.ColumnHideMapper;
import com.timevale.forward.dal.entity.ColumnHideDO;
import com.timevale.forward.facade.api.client.ColumnHideService;
import com.timevale.forward.facade.api.request.ColumnHideGetReq;
import com.timevale.forward.facade.api.request.ColumnHideModifyReq;
import com.timevale.forward.facade.api.result.ColumnHideVO;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.ColumnHideCopier;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.common.annotation.RestService;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;

/**
 * @author by YangXu
 * @date 2022/06/24 09:49
 */
@Slf4j
@RestService
public class ColumnHideServiceImpl implements ColumnHideService {

    @Resource
    private ColumnHideMapper columnHideMapper;

    @Override
    public BaseResult<ColumnHideVO> get(ColumnHideGetReq columnHideGetReq) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        Integer model = columnHideGetReq.getModel();
        Integer tabType = columnHideGetReq.getTabType();

        ColumnHideDO columnHideDO = columnHideMapper.select(model, tabType, userInfo.getId());
        if(columnHideDO == null){
            columnHideDO = new ColumnHideDO();
            columnHideDO.setContent("");
            columnHideDO.setModel(model);
            columnHideDO.setTabType(tabType);
        }

        return BaseResult.success(ColumnHideCopier.INSTANCE.convert(columnHideDO));
    }

    @Override
    public BaseResult<Boolean> update(ColumnHideModifyReq columnHideModifyReq) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        Integer model = columnHideModifyReq.getModel();
        Integer tabType = columnHideModifyReq.getTabType();
        String content = columnHideModifyReq.getContent();

        // 判断是否存在数据库中
        ColumnHideDO columnHideDO = columnHideMapper.select(model, tabType, userInfo.getId());
        if(columnHideDO == null){
            columnHideDO = ColumnHideCopier.INSTANCE.convert(columnHideModifyReq);
            columnHideDO.setBelongMan(userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName());
            columnHideDO.setBelongManId(userInfo.getId());
            columnHideMapper.insert(columnHideDO);
        }else{
            columnHideDO.setContent(content);
            columnHideMapper.updateById(columnHideDO);
        }

        return BaseResult.success(true);
    }
}
