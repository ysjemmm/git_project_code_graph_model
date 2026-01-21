package com.timevale.forward.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.BizDomainGroupCondition;
import com.timevale.forward.dal.condition.BizDomainGroupMatchCondition;
import com.timevale.forward.dal.condition.ProductLineCondition;
import com.timevale.forward.dal.dao.BizDomainGroupMapper;
import com.timevale.forward.dal.dao.BizDomainGroupRelationMapper;
import com.timevale.forward.dal.dao.BizDomainMapper;
import com.timevale.forward.dal.dao.ProductLineMapper;
import com.timevale.forward.dal.entity.BizDomainGroupDO;
import com.timevale.forward.dal.entity.BizDomainGroupRelationDO;
import com.timevale.forward.dal.entity.ProductLineDO;
import com.timevale.forward.facade.api.client.BizDomainGroupService;
import com.timevale.forward.facade.api.query.BizDomainGroupMatchQueryList;
import com.timevale.forward.facade.api.query.BizDomainGroupQueryList;
import com.timevale.forward.facade.api.request.*;
import com.timevale.forward.facade.api.result.BizDomainGroupVO;
import com.timevale.forward.facade.api.result.BizDomainVO;
import com.timevale.forward.facade.api.result.ProductLineVO;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.BizDomainCopier;
import com.timevale.forward.service.copy.BizDomainGroupCopier;
import com.timevale.forward.service.copy.ProductLineCopier;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.aop.LogPoint;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.base.util.AssertUtil;
import com.timevale.mandarin.base.util.CollectionUtils;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author by qiyuan
 * @date 2025/08/25 17:10
 */
@Slf4j
@LogPoint
@RestService
public class BizDomainGroupServiceImpl implements BizDomainGroupService {
    @Resource
    private BizDomainGroupMapper bizDomainGroupMapper;
    @Resource
    private BizDomainGroupRelationMapper bizDomainGroupRelationMapper;
    @Resource
    private BizDomainMapper bizDomainMapper;
    @Resource
    private ProductLineMapper productLineMapper;

    @Override
    public BaseResult<List<BizDomainGroupVO>> bizDomainGroupList() {
        List<BizDomainGroupVO> result = BizDomainGroupCopier.INSTANCE.convert(bizDomainGroupMapper.selectAllBizDomainGroup());
        List<BizDomainVO> allBizDomainVOS = BizDomainCopier.INSTANCE.convert(bizDomainMapper.selectAllBizDomain());
        List<BizDomainGroupRelationDO> allBizDomainGroupRelationDOS = bizDomainGroupRelationMapper.selectAllBizDomainGroupRelation();
        for (BizDomainGroupVO bizDomainGroupVO : result) {
            List<BizDomainGroupRelationDO> bizDomainGroupRelationDOS = allBizDomainGroupRelationDOS.stream()
                    .filter(e -> Objects.equals(e.getBizDomainGroupId(), bizDomainGroupVO.getId())).collect(Collectors.toList());
            bizDomainGroupVO.setBizDomainVOS(allBizDomainVOS.stream()
                    .filter(e -> bizDomainGroupRelationDOS.stream()
                            .anyMatch(it -> Objects.equals(e.getId(), it.getBizDomainId())))
                    .collect(Collectors.toList()));
        }
        return BaseResult.success(result);
    }

    @Override
    public BaseResult<PageQueryResult<BizDomainGroupVO>> bizDomainGroupList(BizDomainGroupQueryList bizDomainGroupQueryList) {
        BizDomainGroupCondition condition = BizDomainGroupCopier.INSTANCE.convert(bizDomainGroupQueryList);
        PageHelper.startPage(bizDomainGroupQueryList.pageNum, bizDomainGroupQueryList.pageSize, CommonConstant.DEFAULT_ORDER_BY);
        List<BizDomainGroupDO> bizDomainGroupDOList = bizDomainGroupMapper.selectByCondition(condition);
        List<BizDomainGroupVO> bizDomainGroupVOList = BizDomainGroupCopier.INSTANCE.convert(bizDomainGroupDOList);
        List<BizDomainVO> allBizDomainVOS = BizDomainCopier.INSTANCE.convert(bizDomainMapper.selectAllBizDomain());
        List<BizDomainGroupRelationDO> allBizDomainGroupRelationDOS = bizDomainGroupRelationMapper.selectAllBizDomainGroupRelation();
        for (BizDomainGroupVO bizDomainGroupVO : bizDomainGroupVOList) {
            List<BizDomainGroupRelationDO> bizDomainGroupRelationDOS = allBizDomainGroupRelationDOS.stream()
                    .filter(e -> Objects.equals(e.getBizDomainGroupId(), bizDomainGroupVO.getId())).collect(Collectors.toList());
            bizDomainGroupVO.setBizDomainVOS(allBizDomainVOS.stream()
                    .filter(e -> bizDomainGroupRelationDOS.stream()
                            .anyMatch(it -> Objects.equals(e.getId(), it.getBizDomainId())))
                    .collect(Collectors.toList()));
        }
        PageInfo<BizDomainGroupDO> pageInfo = new PageInfo<>(bizDomainGroupDOList);
        PageQueryResult<BizDomainGroupVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(bizDomainGroupVOList);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);
        return BaseResult.success(pageQueryResult);
    }

    @Override
    public BaseResult<PageQueryResult<BizDomainGroupVO>> matchBizDomainGroupList(BizDomainGroupMatchQueryList bizDomainGroupMatchQueryList) {
        BizDomainGroupMatchCondition condition = BizDomainGroupCopier.INSTANCE.convert(bizDomainGroupMatchQueryList);
        PageHelper.startPage(bizDomainGroupMatchQueryList.pageNum, bizDomainGroupMatchQueryList.pageSize, CommonConstant.DEFAULT_ORDER_BY);
        List<BizDomainGroupDO> bizDomainGroupDOList = bizDomainGroupMapper.matchBizDomainGroupList(condition);
        List<BizDomainGroupVO> bizDomainGroupVOList = BizDomainGroupCopier.INSTANCE.convert(bizDomainGroupDOList);
        PageInfo<BizDomainGroupDO> pageInfo = new PageInfo<>(bizDomainGroupDOList);
        PageQueryResult<BizDomainGroupVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(bizDomainGroupVOList);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);
        return BaseResult.success(pageQueryResult);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> add(BizDomainGroupAddReq bizDomainGroupAddReq) {
        BizDomainGroupDO bizDomainGroup = bizDomainGroupMapper.selectByName(bizDomainGroupAddReq.getName());
        if (bizDomainGroup != null) {
            throw new BaseBizRuntimeException("该业务域集的名称已存在,请修改后重试");
        }

        BizDomainGroupDO bizDomainGroupDO = BizDomainGroupCopier.INSTANCE.convert(bizDomainGroupAddReq);
        bizDomainGroupMapper.insert(bizDomainGroupDO);

        List<BizDomainGroupRelationDO> bizDomainGroupRelationDOS = new ArrayList<>();
        for (Long bizDomainId : bizDomainGroupAddReq.getBizDomainIds()) {
            bizDomainGroupRelationDOS.add(new BizDomainGroupRelationDO()
                    .setBizDomainId(bizDomainId)
                    .setBizDomainGroupId(bizDomainGroupDO.getId()));
        }
        bizDomainGroupRelationMapper.batchInsert(bizDomainGroupRelationDOS);
        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> update(BizDomainGroupModifyReq bizDomainGroupModifyReq) {
        BizDomainGroupDO bizDomainGroup  = bizDomainGroupMapper.selectByName(bizDomainGroupModifyReq.getName());
        if (bizDomainGroup != null && !Objects.equals(bizDomainGroup.getId(), bizDomainGroupModifyReq.getId())) {
            throw new BaseBizRuntimeException("该业务域集的名称已存在,请修改后重试");
        }
        BizDomainGroupDO bizDomainGroupDO = BizDomainGroupCopier.INSTANCE.convert(bizDomainGroupModifyReq);
        bizDomainGroupMapper.update(bizDomainGroupDO);

        List<BizDomainGroupRelationDO> bizDomainGroupRelationDOS = bizDomainGroupRelationMapper.selectByBizDomainGroupId(bizDomainGroupDO.getId());
        List<Long> dbBizDomainIds = bizDomainGroupRelationDOS.stream().map(BizDomainGroupRelationDO::getBizDomainId).collect(Collectors.toList());
        List<Long> delBizDomainIds = dbBizDomainIds.stream().filter(it -> !bizDomainGroupModifyReq.getBizDomainIds().contains(it)).collect(Collectors.toList());
        List<Long> addBizDomainIds = bizDomainGroupModifyReq.getBizDomainIds().stream().filter(it -> !dbBizDomainIds.contains(it)).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(addBizDomainIds)) {
            List<BizDomainGroupRelationDO> addBizDomainGroupRelationDOS = addBizDomainIds.stream().map(bizDomainId -> new BizDomainGroupRelationDO()
                    .setBizDomainGroupId(bizDomainGroupDO.getId())
                    .setBizDomainId(bizDomainId)).collect(Collectors.toList());

            bizDomainGroupRelationMapper.batchInsert(addBizDomainGroupRelationDOS);
        }
        if (CollectionUtils.isNotEmpty(delBizDomainIds)) {
            bizDomainGroupRelationMapper.batchDelete(bizDomainGroupDO.getId(), delBizDomainIds);
        }
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> updateBizDomainGroupListingStatus(Long bizDomainGroupId) {
        BizDomainGroupDO bizDomainGroupDO = bizDomainGroupMapper.selectById(bizDomainGroupId);
        AssertUtil.notNull(bizDomainGroupDO, "业务域集ID有误");
        if (bizDomainGroupDO.getIsDeleted()) {
            BaseResult.success(true);
        }
        BizDomainGroupDO updateBizDomainGroupDO = new BizDomainGroupDO();
        updateBizDomainGroupDO.setId(bizDomainGroupId);
        updateBizDomainGroupDO.setListingStatus(bizDomainGroupDO.getListingStatus() == 0 ? 1 : 0);
        bizDomainGroupMapper.update(updateBizDomainGroupDO);
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> deleteBizDomainGroup(Long bizDomainGroupId) {
        BizDomainGroupDO bizDomainGroupDO = bizDomainGroupMapper.selectById(bizDomainGroupId);
        AssertUtil.notNull(bizDomainGroupDO, "业务域集ID有误");
        if (bizDomainGroupDO.getIsDeleted()) {
            BaseResult.success(true);
        }

        BizDomainGroupDO updateBizDomainGroupDO = new BizDomainGroupDO();
        updateBizDomainGroupDO.setId(bizDomainGroupId);
        updateBizDomainGroupDO.setIsDeleted(true);
        bizDomainGroupMapper.update(updateBizDomainGroupDO);

        bizDomainGroupRelationMapper.batchDelete(bizDomainGroupId, null);
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<List<ProductLineVO>> productLineList(Long bizDomainGroupId) {
        List<BizDomainGroupRelationDO> bizDomainGroupRelationDOS = bizDomainGroupRelationMapper.selectByBizDomainGroupId(bizDomainGroupId);
        List<Long> bizDomainIds = bizDomainGroupRelationDOS.stream().map(BizDomainGroupRelationDO::getBizDomainId).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(bizDomainIds)) {
            return BaseResult.success(Lists.emptyList());
        }
        List<ProductLineDO> productLineDOS = productLineMapper.selectByCondition(ProductLineCondition.builder().bizDomainIds(bizDomainIds).build());
        List<ProductLineVO> productLineVOList = ProductLineCopier.INSTANCE.convert(productLineDOS);
        return BaseResult.success(productLineVOList);
    }

    @Override
    public BaseResult<PageQueryResult<BizDomainGroupVO>> bizDomainGroupListWithGroups(BizDomainGroupQueryList bizDomainGroupQueryList) {
        PageHelper.startPage(bizDomainGroupQueryList.pageNum, bizDomainGroupQueryList.pageSize);
        List<BizDomainGroupDO> bizDomainGroupDOList = bizDomainGroupMapper.selectBizDomainGroupsWithGroups(bizDomainGroupQueryList.getName());
        List<BizDomainGroupVO> bizDomainGroupVOList = BizDomainGroupCopier.INSTANCE.convert(bizDomainGroupDOList);
        
        PageInfo<BizDomainGroupDO> pageInfo = new PageInfo<>(bizDomainGroupDOList);
        PageQueryResult<BizDomainGroupVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(bizDomainGroupVOList);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);
        return BaseResult.success(pageQueryResult);
    }
}
