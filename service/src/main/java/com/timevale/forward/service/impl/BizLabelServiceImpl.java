package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.BizLabelMapper;
import com.timevale.forward.dal.dao.LabelMapper;
import com.timevale.forward.dal.entity.BizLabelDO;
import com.timevale.forward.dal.entity.LabelDO;
import com.timevale.forward.facade.api.client.BizLabelService;
import com.timevale.forward.facade.api.query.BizLabelQueryList;
import com.timevale.forward.facade.api.request.BizLabelAddReq;
import com.timevale.forward.facade.api.result.LabelDetailVO;
import com.timevale.forward.service.component.BizLabelComponent;
import com.timevale.forward.service.copy.BizLabelCopier;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.common.annotation.RestService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.assertj.core.util.Lists;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;
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
    private BizLabelComponent bizLabelComponent;

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
        List<BizLabelDO> list = bizLabelMapper.list(bizLabelAddReq.getBizId(), bizLabelAddReq.getType());
        boolean match = list.stream().map(BizLabelDO::getLabelId).anyMatch(a -> Objects.equals(a, bizLabelAddReq.getLabelId()));
        if(bizLabelAddReq.getAdd()&&!match){
            bizLabelMapper.insert(bizLabelDO);
        }else {
            bizLabelDO.setIsDeleted(true);
            bizLabelMapper.update(bizLabelDO);
        }
        if(!match||!bizLabelAddReq.getAdd()){
            bizLabelComponent.addLog(bizLabelDO.getBizId(),Lists.newArrayList(labelDO.getId()),bizLabelDO.getType(),bizLabelAddReq.getAdd());
        }
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<List<LabelDetailVO>> getSelectedLabel(BizLabelQueryList bizLabelQueryList) {
        log.info("标签查询,参数:{}", bizLabelQueryList);
        List<BizLabelDO> list = bizLabelMapper.list(bizLabelQueryList.getBizId(), bizLabelQueryList.getType());
        List<Long> labelIds = list.stream().map(BizLabelDO::getLabelId).collect(Collectors.toList());
        if(CollectionUtils.isEmpty(labelIds)){
            return BaseResult.success(Lists.emptyList());
        }
        List<LabelDO> labelDOList = labelMapper.getByIds(labelIds);
        List<LabelDetailVO> bizLabelDOList = labelDOList.stream().map(a -> {
            LabelDetailVO labelDetailVO = new LabelDetailVO();
            labelDetailVO.setId(a.getId());
            labelDetailVO.setName(a.getName());
            return labelDetailVO;
        }).collect(Collectors.toList());
        return BaseResult.success(bizLabelDOList);
    }

}
