package com.timevale.forward.service.impl;

import com.timevale.crm.sdk.common.entity.AccountInfo;
import com.timevale.crm.sdk.common.utils.SessionLocalUtil;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.BizLabelMapper;
import com.timevale.forward.dal.dao.LabelCategoryMapper;
import com.timevale.forward.dal.dao.LabelMapper;
import com.timevale.forward.dal.entity.BizLabelDO;
import com.timevale.forward.dal.entity.LabelCategoryDO;
import com.timevale.forward.dal.entity.LabelDO;
import com.timevale.forward.facade.api.client.BizLabelService;
import com.timevale.forward.facade.api.query.BizLabelQueryList;
import com.timevale.forward.facade.api.request.BizLabelAddListReq;
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
import java.util.Date;
import java.util.List;
import java.util.Map;
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

    @Resource
    private LabelCategoryMapper labelCategoryMapper;

    private static final int MAX_COUNT = 20;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> markOrUnMark(BizLabelAddReq bizLabelAddReq) {
        log.info("标签标记,参数:{}", bizLabelAddReq);
        BizLabelDO bizLabelDO = BizLabelCopier.INSTANCE.convert(bizLabelAddReq);
        LabelDO labelDO = labelMapper.get(bizLabelAddReq.getLabelId());
        if (labelDO == null) {
            throw new BaseBizRuntimeException("该标签已被删除");
        }

        //同个标签,只能添加一次
        if (bizLabelAddReq.getAdd() ) {
            List<BizLabelDO> list = bizLabelMapper.list(bizLabelAddReq.getBizId(), bizLabelAddReq.getType());
            boolean match = list.stream().map(BizLabelDO::getLabelId).anyMatch(a -> Objects.equals(a, bizLabelAddReq.getLabelId()));
            if(match){
                throw new BaseBizRuntimeException("同个标签不能重复添加,请刷新后重试");
            }
            if (list.size() == MAX_COUNT) {
                throw new BaseBizRuntimeException("添加的标签数量不能超过20");
            }
            bizLabelMapper.insert(bizLabelDO);
        } else {
            bizLabelDO.setIsDeleted(true);
            bizLabelMapper.update(bizLabelDO);
        }
        bizLabelComponent.addLog(bizLabelDO.getBizId(), Lists.newArrayList(labelDO.getId()), bizLabelDO.getType(), bizLabelAddReq.getAdd());

        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> batchAddLabels(BizLabelAddListReq req) {
        List<Long> labelIdList = req.getLabelIdList();
        if (CollectionUtils.isEmpty(labelIdList)) {
            return BaseResult.success(true);
        }

        log.info("batchMarkOrUnMark, req:{}", req);

        List<LabelDO> labelDOList = labelMapper.getByIds(labelIdList);
        if (labelDOList.size() < labelIdList.size()) {
            throw new BaseBizRuntimeException("存在已被删除的标签，请刷新后重试");
        }

        List<BizLabelDO> list = bizLabelMapper.list(req.getBizId(), req.getType());

        if (CollectionUtils.isNotEmpty(list)) {
            boolean match;
            for (Long labelId : labelIdList) {
                match = list.stream()
                        .map(BizLabelDO::getLabelId)
                        .anyMatch(a -> Objects.equals(a, labelId));

                if (match) {
                    throw new BaseBizRuntimeException("同个标签不能重复添加, 请刷新后重试");
                }
            }
        }

        int currentSize = list.size() + labelDOList.size();
        if (currentSize > MAX_COUNT) {
            throw new BaseBizRuntimeException("添加的标签数量不能超过20");
        }

        List<BizLabelDO> bizLabelDOList = convert2BizLabelDOList(req);

        bizLabelMapper.batchInsert(bizLabelDOList);

        bizLabelComponent.addLog(req.getBizId(), req.getLabelIdList(), req.getType(), true);

        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> batchUpdateLabels(BizLabelAddListReq req) {
        List<Long> labelIdList = req.getLabelIdList();
        if (CollectionUtils.isEmpty(labelIdList)) {
            return BaseResult.success(true);
        }

        log.info("batchMarkOrUnMark, req:{}", req);

        List<LabelDO> labelDOList = labelMapper.getByIds(labelIdList);
        if (labelDOList.size() < labelIdList.size()) {
            throw new BaseBizRuntimeException("存在已被删除的标签，请刷新后重试");
        }

        List<BizLabelDO> list = bizLabelMapper.list(req.getBizId(), req.getType());

        // 要删除的标签集合
        List<Long> deleteIds = list.stream().filter(bizLabelDO -> !labelIdList.contains(bizLabelDO.getLabelId())).map(BizLabelDO::getLabelId).collect(Collectors.toList());
        List<Long> addIds = labelIdList.stream().filter(labelId -> list.stream().noneMatch(bizLabelDO -> bizLabelDO.getLabelId().equals(labelId))).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(deleteIds)) {
            for (Long deleteId : deleteIds) {
                BizLabelAddReq bizLabelAddReq = new BizLabelAddReq();
                bizLabelAddReq.setAdd(false);
                bizLabelAddReq.setBizId(req.getBizId());
                bizLabelAddReq.setType(req.getType());
                bizLabelAddReq.setLabelId(deleteId);
                markOrUnMark(bizLabelAddReq);
            }
        }

        if (CollectionUtils.isEmpty(addIds)) {
            return BaseResult.success(true);
        }

        if (addIds.size() > MAX_COUNT) {
            throw new BaseBizRuntimeException("添加的标签数量不能超过20");
        }

        List<BizLabelDO> bizLabelDOList = convert2BizLabelDOList(req);

        List<BizLabelDO> dos = bizLabelDOList.stream().filter(bizLabelDO -> addIds.contains(bizLabelDO.getLabelId())).collect(Collectors.toList());

        bizLabelMapper.batchInsert(dos);

        bizLabelComponent.addLog(req.getBizId(), addIds, req.getType(), true);

        return BaseResult.success(true);
    }

    private List<BizLabelDO> convert2BizLabelDOList(BizLabelAddListReq req) {
        AccountInfo accountInfo = SessionLocalUtil.getUserSession();
        List<BizLabelDO> bizLabelDOList = Lists.newArrayList();

        for (Long labelId : req.getLabelIdList()) {
            BizLabelDO bizLabelDO = new BizLabelDO();
            bizLabelDO.setLabelId(labelId);
            bizLabelDO.setType(req.getType());
            bizLabelDO.setBizId(req.getBizId());
            bizLabelDO.setCreateMan(accountInfo.getAlias());
            bizLabelDO.setCreateManId(accountInfo.getAccount());
            bizLabelDO.setCreateDate(new Date());

            bizLabelDOList.add(bizLabelDO);
        }

        return bizLabelDOList;
    }

    @Override
    public BaseResult<List<LabelDetailVO>> getSelectedLabel(BizLabelQueryList bizLabelQueryList) {
        log.info("标签查询,参数:{}", bizLabelQueryList);
        List<BizLabelDO> list = bizLabelMapper.list(bizLabelQueryList.getBizId(), bizLabelQueryList.getType());
        List<Long> labelIds = list.stream().map(BizLabelDO::getLabelId).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(labelIds)) {
            return BaseResult.success(Lists.emptyList());
        }
        List<LabelDO> labelDOList = labelMapper.getByIds(labelIds);
        Map<Long, String> labelCategoryMap = labelCategoryMapper.get(labelDOList.stream().map(LabelDO::getLabelCategoryId).collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(LabelCategoryDO::getId, LabelCategoryDO::getName));
        List<LabelDetailVO> bizLabelDOList = labelDOList.stream().map(a -> {
            LabelDetailVO labelDetailVO = new LabelDetailVO();
            labelDetailVO.setId(a.getId());
            labelDetailVO.setName(a.getName());
            labelDetailVO.setCategoryId(a.getLabelCategoryId());
            labelDetailVO.setCategoryName(labelCategoryMap.getOrDefault(a.getLabelCategoryId(), ""));
            return labelDetailVO;
        }).collect(Collectors.toList());
        return BaseResult.success(bizLabelDOList);
    }

}
