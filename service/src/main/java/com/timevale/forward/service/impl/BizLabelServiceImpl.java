package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.BizChangeLogMapper;
import com.timevale.forward.dal.dao.BizLabelMapper;
import com.timevale.forward.dal.dao.LabelMapper;
import com.timevale.forward.dal.entity.BizChangeLogDO;
import com.timevale.forward.dal.entity.BizLabelDO;
import com.timevale.forward.dal.entity.LabelDO;
import com.timevale.forward.facade.api.client.BizLabelService;
import com.timevale.forward.facade.api.query.BizLabelQueryList;
import com.timevale.forward.facade.api.request.BizLabelAddReq;
import com.timevale.forward.model.enums.BizChangeLogFieldEnum;
import com.timevale.forward.model.enums.BizChangeLogTypeEnum;
import com.timevale.forward.model.enums.BizTypeEnum;
import com.timevale.forward.model.enums.ButtonActionEnum;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.BizLabelCopier;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.common.annotation.RestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Slf4j
@RestService
public class BizLabelServiceImpl implements BizLabelService {

    @Resource
    private BizLabelMapper bizLabelMapper;

    @Resource
    private BizChangeLogMapper bizChangeLogMapper;

    @Resource
    private LabelMapper labelMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> markOrUnMark(BizLabelAddReq bizLabelAddReq) {
        log.info("标签标记,参数:{}", bizLabelAddReq);
        BizLabelDO bizLabelDO = BizLabelCopier.INSTANCE.convert(bizLabelAddReq);
        LabelDO labelDO = labelMapper.get(bizLabelAddReq.getLabelId());
        if(labelDO==null){
            throw new BaseBizRuntimeException("该标签已被删除");
        }
        if(bizLabelAddReq.getAdd()){
            bizLabelMapper.insert(bizLabelDO);
        }else {
            bizLabelDO.setIsDeleted(true);
            bizLabelMapper.update(bizLabelDO);
        }
        addLog(bizLabelDO.getBizId(),bizLabelDO.getType(),labelDO.getName(),bizLabelAddReq.getAdd());
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<List<Long>> getSelectedLabel(BizLabelQueryList bizLabelQueryList) {
        log.info("标签查询,参数:{}", bizLabelQueryList);
        List<BizLabelDO> list = bizLabelMapper.list(bizLabelQueryList.getBizId(), bizLabelQueryList.getType());
        List<Long> labelIds = list.stream().map(BizLabelDO::getLabelId).collect(Collectors.toList());
        return BaseResult.success(labelIds);
    }

    private void addLog(Long mainId, Integer type, String newValue, Boolean add) {
        Integer logType=0;
        if(BizTypeEnum.PROJECT.getCode().equals(type)){
            logType= BizChangeLogTypeEnum.PROJECT.getCode();
        }else if(BizTypeEnum.PRODUCT_DEMAND.getCode().equals(type)){
            logType=BizChangeLogTypeEnum.PRODUCT_DEMAND.getCode();
        }else if(BizTypeEnum.BIZ_DEMAND.getCode().equals(type)){
            logType=BizChangeLogTypeEnum.BIZ_DEMAND.getCode();
        }else if(BizTypeEnum.BUG_OFFLINE.getCode().equals(type)){
            logType=BizChangeLogTypeEnum.BUG_OFFLINE.getCode();
        }else if(BizTypeEnum.BUG_ONLINE.getCode().equals(type)){
            logType=BizChangeLogTypeEnum.BUG_ONLINE.getCode();
        }
        BizChangeLogDO logDO = new BizChangeLogDO();
        logDO.setType(logType);
        logDO.setMainId(mainId);
        logDO.setField(BizChangeLogFieldEnum.LABEL.getText());
        logDO.setOldValue(newValue);
        logDO.setNewValue(newValue);
        String action = add ? ButtonActionEnum.ADD.getText() : ButtonActionEnum.DELETE.getText();
        logDO.setAction(action);
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        String createMan = userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName();
        String createManId = userInfo.getId();
        logDO.setCreateMan(createMan);
        logDO.setCreateManId(createManId);
        bizChangeLogMapper.insert(logDO);
    }
}
