package com.timevale.forward.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.BizDomainCondition;
import com.timevale.forward.dal.dao.BizDomainGroupMapper;
import com.timevale.forward.dal.dao.BizDomainGroupRelationMapper;
import com.timevale.forward.dal.dao.BizDomainMapper;
import com.timevale.forward.dal.dao.ProductLineMapper;
import com.timevale.forward.dal.entity.BizDomainDO;
import com.timevale.forward.dal.entity.BizDomainGroupDO;
import com.timevale.forward.dal.entity.BizDomainGroupRelationDO;
import com.timevale.forward.dal.entity.ProductLineDO;
import com.timevale.forward.facade.api.client.BizDomainService;
import com.timevale.forward.facade.api.query.BizDomainQueryList;
import com.timevale.forward.facade.api.request.BizDomainAddReq;
import com.timevale.forward.facade.api.request.BizDomainModifyReq;
import com.timevale.forward.facade.api.request.GetBizDomainGroupsByNamesReq;
import com.timevale.forward.facade.api.request.UpdateBizDomainListingStatusReq;
import com.timevale.forward.facade.api.result.BizDomainGroupSimpleVO;
import com.timevale.forward.facade.api.result.BizDomainVO;
import com.timevale.forward.service.copy.BizDomainCopier;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.aop.LogPoint;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.base.util.AssertUtil;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author by YangXu
 * @date 2021/12/13 17:10
 */
@Slf4j
@LogPoint
@RestService
public class BizDomainServiceImpl implements BizDomainService {

    @Resource
    private BizDomainMapper bizDomainMapper;
    @Resource
    private ProductLineMapper productLineMapper;
    @Resource
    private BizDomainGroupMapper bizDomainGroupMapper;

    @Resource
    private BizDomainGroupRelationMapper bizDomainGroupRelationMapper;

    @Override
    public BaseResult<PageQueryResult<BizDomainVO>> bizDomainListWithOrder(BizDomainQueryList bizDomainQueryList) {

        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        PageHelper.startPage(bizDomainQueryList.pageNum, bizDomainQueryList.pageSize);
        List<BizDomainDO> bizDomainDOList = bizDomainMapper.selectWithOrder(userInfo.getId(), bizDomainQueryList.getName(), bizDomainQueryList.getListingStatus());
        List<BizDomainVO> bizDomainVOList = BizDomainCopier.INSTANCE.convert(bizDomainDOList);
        PageInfo<BizDomainDO> pageInfo = new PageInfo<>(bizDomainDOList);
        PageQueryResult<BizDomainVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(bizDomainVOList);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);
        return BaseResult.success(pageQueryResult);
    }

    @Override
    public BaseResult<List<BizDomainVO>> bizDomainList() {
        List<BizDomainVO> result = BizDomainCopier.INSTANCE.convert(bizDomainMapper.selectAllBizDomain());
        return BaseResult.success(result);
    }

    @Override
    public BaseResult<PageQueryResult<BizDomainVO>> bizDomainList(BizDomainQueryList bizDomainQueryList) {
        BizDomainCondition condition = BizDomainCopier.INSTANCE.convert(bizDomainQueryList);
        PageHelper.startPage(bizDomainQueryList.pageNum, bizDomainQueryList.pageSize, "sort_order desc, modify_date desc, id desc");

        List<BizDomainDO> bizDomainDOList = bizDomainMapper.selectByCondition(condition);
        List<BizDomainVO> bizDomainVOList = BizDomainCopier.INSTANCE.convert(bizDomainDOList);
        PageInfo<BizDomainDO> pageInfo = new PageInfo<>(bizDomainDOList);
        PageQueryResult<BizDomainVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(bizDomainVOList);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);
        return BaseResult.success(pageQueryResult);
    }

    @Override
    public BaseResult<Boolean> add(BizDomainAddReq bizDomainAddReq) {
        BizDomainDO bizDomainDO = BizDomainCopier.INSTANCE.convert(bizDomainAddReq);
        bizDomainMapper.insert(bizDomainDO);
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> update(BizDomainModifyReq bizDomainModifyReq) {
        BizDomainDO bizDomainDO = BizDomainCopier.INSTANCE.convert(bizDomainModifyReq);
        bizDomainMapper.update(bizDomainDO);
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> delOrUnDelete(Long bizDomainId) {
        BizDomainDO bizDomainDO = bizDomainMapper.selectById(bizDomainId);
        if(!bizDomainDO.getIsDeleted()){
            List<ProductLineDO> productLineDOList = productLineMapper.getBizDomainId(bizDomainId);
            if(CollectionUtils.isNotEmpty(productLineDOList)){
                List<String> names = productLineDOList.stream().map(ProductLineDO::getName).collect(Collectors.toList());
                throw new BaseBizRuntimeException("该业务域下存在产品线:"+names+",不能删除");
            }
            bizDomainDO.setIsDeleted(true);
            bizDomainMapper.update(bizDomainDO);
            return BaseResult.success(true);
        }
        //恢复
        bizDomainDO.setIsDeleted(false);
        bizDomainMapper.update(bizDomainDO);
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> updateBizDomainListingStatus(UpdateBizDomainListingStatusReq req) {
        Long bizDomainId = req.getBizDomainId();
        Integer listingStatus = req.getListingStatus();
        AssertUtil.notNull(bizDomainId, "业务域ID不能为空");
        AssertUtil.notNull(listingStatus, "上架状态不能为空");
        AssertUtil.checkState(listingStatus == 0 || listingStatus == 1, "上架状态有误");

        BizDomainDO bizDomainDO = bizDomainMapper.selectById(bizDomainId);
        AssertUtil.notNull(bizDomainDO, "业务域ID有误");

        Integer currentListingStatus = bizDomainDO.getListingStatus();
        if (listingStatus.equals(currentListingStatus)) {
            return BaseResult.success(true);
        }

        BizDomainDO updateBizDomainDO = new BizDomainDO();

        updateBizDomainDO.setId(bizDomainId);
        updateBizDomainDO.setListingStatus(listingStatus);

        bizDomainMapper.update(updateBizDomainDO);

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> deleteBizDomain(Long bizDomainId) {
        BizDomainDO bizDomainDO = bizDomainMapper.selectById(bizDomainId);
        AssertUtil.notNull(bizDomainDO, "业务域ID有误");
        if (bizDomainDO.getIsDeleted()) {
            BaseResult.success(true);
        }

        List<ProductLineDO> productLineDOList = productLineMapper.getBizDomainId(bizDomainId);
        if (CollectionUtils.isNotEmpty(productLineDOList)) {
            List<String> names = productLineDOList.stream().map(ProductLineDO::getName).collect(Collectors.toList());
            throw new BaseBizRuntimeException("该业务域下存在产品线" + names + "不能删除");
        }

        BizDomainDO updateBizDomainDO = new BizDomainDO();

        updateBizDomainDO.setId(bizDomainId);
        updateBizDomainDO.setIsDeleted(true);

        bizDomainMapper.update(updateBizDomainDO);

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<PageQueryResult<BizDomainVO>> simpleList(BizDomainQueryList bizDomainQueryList) {
        BizDomainCondition condition = BizDomainCopier.INSTANCE.convert(bizDomainQueryList);
        PageHelper.startPage(bizDomainQueryList.pageNum, bizDomainQueryList.pageSize);

        List<BizDomainDO> bizDomainDOList = bizDomainMapper.simpleList(condition);
        List<BizDomainVO> bizDomainVOList = BizDomainCopier.INSTANCE.convert(bizDomainDOList);
        PageInfo<BizDomainDO> pageInfo = new PageInfo<>(bizDomainDOList);
        PageQueryResult<BizDomainVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(bizDomainVOList);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);
        return BaseResult.success(pageQueryResult);
    }

    @Override
    public BaseResult<List<BizDomainGroupSimpleVO>> getBizDomainGroupsByNames(GetBizDomainGroupsByNamesReq req) {
        List<String> bizDomainNames = req.getBizDomainNames();
        if (CollectionUtils.isEmpty(bizDomainNames)) {
            return BaseResult.success(java.util.Collections.emptyList());
        }

        // 1. 根据业务域名称查询业务域
        List<BizDomainDO> bizDomainDOList = bizDomainMapper.selectByName(bizDomainNames);
        if (CollectionUtils.isEmpty(bizDomainDOList)) {
            return BaseResult.success(java.util.Collections.emptyList());
        }

        // 2. 获取业务域ID列表
        List<Long> bizDomainIds = bizDomainDOList.stream()
                .map(BizDomainDO::getId)
                .collect(Collectors.toList());

        // 3. 查询业务域组(只返回存在需求的业务域组)
        List<BizDomainGroupDO> bizDomainGroupDOList = bizDomainGroupMapper.selectGroupsWithDemandsByBizDomainIds(bizDomainIds);
        if (CollectionUtils.isEmpty(bizDomainGroupDOList)) {
            return BaseResult.success(java.util.Collections.emptyList());
        }

        // 4. 转换为VO
        List<BizDomainGroupSimpleVO> result = bizDomainGroupDOList.stream()
                .map(group -> {
                    BizDomainGroupSimpleVO vo = new BizDomainGroupSimpleVO();
                    vo.setId(group.getId());
                    vo.setName(group.getName());
                    return vo;
                })
                .collect(Collectors.toList());

        return BaseResult.success(result);
    }

    @Override
    public BaseResult<Boolean> isEsignBizDomainGroup(String bizDomainName) {
        // 1. 参数校验
        if (StringUtils.isBlank(bizDomainName)) {
            return BaseResult.success(false);
        }

        // 2. 根据业务域名称查询业务域
        List<BizDomainDO> bizDomainDOList = bizDomainMapper.selectByName(Collections.singletonList(bizDomainName));
        if (CollectionUtils.isEmpty(bizDomainDOList)) {
            return BaseResult.success(false);
        }

        BizDomainDO bizDomainDO = bizDomainDOList.get(0);
        if (bizDomainDO == null || bizDomainDO.getId() == null) {
            return BaseResult.success(false);
        }

        // 3. 查询业务域组关系
        List<BizDomainGroupRelationDO> bizDomainGroupRelationDOS = bizDomainGroupRelationMapper.selectByBizDomainId(bizDomainDO.getId());
        if (CollectionUtils.isEmpty(bizDomainGroupRelationDOS)) {
            return BaseResult.success(false);
        }

        // 4. 提取业务域组ID并过滤null值
        List<Long> bizDomainGroupIds = bizDomainGroupRelationDOS.stream()
                .filter(relation -> relation != null && relation.getBizDomainGroupId() != null)
                .map(BizDomainGroupRelationDO::getBizDomainGroupId)
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(bizDomainGroupIds)) {
            return BaseResult.success(false);
        }

        // 5. 查询业务域组信息
        List<BizDomainGroupDO> bizDomainGroupDOList = bizDomainGroupMapper.getByIds(bizDomainGroupIds);
        if (CollectionUtils.isEmpty(bizDomainGroupDOList)) {
            return BaseResult.success(false);
        }

        // 6. 检查是否存在e签宝业务域集
        boolean anyMatch = bizDomainGroupDOList.stream()
                .filter(group -> group != null && group.getName() != null)
                .anyMatch(group -> "e签宝业务域集".equals(group.getName()));

        return BaseResult.success(anyMatch);
    }

}
