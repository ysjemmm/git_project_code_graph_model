package com.timevale.forward.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.ProductLineCondition;
import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.facade.api.UpdateProductLineListingStatusReq;
import com.timevale.forward.facade.api.client.ProductLineService;
import com.timevale.forward.facade.api.query.ProductLineQueryList;
import com.timevale.forward.facade.api.request.ProductLineAddReq;
import com.timevale.forward.facade.api.request.ProductLineModifyReq;
import com.timevale.forward.facade.api.result.ModelVO;
import com.timevale.forward.facade.api.result.ProductLineModelVO;
import com.timevale.forward.facade.api.result.ProductLineVO;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.ModelCopier;
import com.timevale.forward.service.copy.ProductLineCopier;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.base.util.AssertUtil;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author by YangXu
 * @date 2021/12/13 17:07
 */
@Slf4j
@RestService
public class ProductLineServiceImpl implements ProductLineService {

    @Resource
    private ProductLineMapper productLineMapper;

    @Resource
    private BizDomainMapper bizDomainMapper;

    @Resource
    private ModelMapper modelMapper;

    @Resource
    private BizDemandMapper bizDemandMapper;

    @Resource
    private ProductDemandMapper productDemandMapper;

    @Resource
    private ProjectProductLineMapper projectProductLineMapper;

    @Resource
    private BugOnlineProductLineMapper bugOnlineProductLineMapper;

    @Resource
    private BugOfflineMapper bugOfflineMapper;

    @Resource
    private TaskMapper taskMapper;

    @Resource
    private TroubleTicketMapper troubleTicketMapper;

    @Override
    public BaseResult<List<ProductLineVO>> listBizDomainProductLines(Long bizDomainId) {
        List<ProductLineDO> productLineDOList = productLineMapper.getBizDomainId(bizDomainId);
        final BizDomainDO bizDomainDO = bizDomainMapper.selectById(bizDomainId);
        List<ProductLineVO> productLineVOList = ProductLineCopier.INSTANCE.convert(productLineDOList);
        // 填充产品线对应业务域负责人信息
        productLineVOList.forEach(e -> {
            e.setBizDomainOwner(bizDomainDO.getOwner());
            e.setBizDomainOwnerId(bizDomainDO.getOwnerId());
            e.setBizDomainName(bizDomainDO.getName());
        });
        return BaseResult.success(productLineVOList);
    }

    @Override
    public BaseResult<List<ProductLineVO>> productLineList() {
        List<BizDomainDO> bizDomainDOList = bizDomainMapper.selectAllBizDomain();

        List<ProductLineDO> productLineDOList = productLineMapper.selectAllProductLine();

        return BaseResult.success(build(productLineDOList, bizDomainDOList));
    }

    @Override
    public BaseResult<PageQueryResult<ProductLineVO>> productLineList(ProductLineQueryList productLineQueryList) {
        ProductLineCondition condition = ProductLineCopier.INSTANCE.convert(productLineQueryList);
        List<BizDomainDO> bizDomainDOList = bizDomainMapper.selectAllBizDomain();

        PageHelper.startPage(productLineQueryList.pageNum, productLineQueryList.pageSize, CommonConstant.DEFAULT_ORDER_BY);
        List<ProductLineDO> productLineDOList = productLineMapper.selectByCondition(condition);

        List<ProductLineVO> productLineVOList = build(productLineDOList, bizDomainDOList);
        PageInfo<ProductLineDO> pageInfo = new PageInfo<>(productLineDOList);
        PageQueryResult<ProductLineVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(productLineVOList);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);
        return BaseResult.success(pageQueryResult);
    }

    @Override
    public BaseResult<List<ProductLineVO>> getProductLines(Long projectId) {
        List<ProductLineDO> productLineDO = productLineMapper.get(projectId);
        List<ProductLineVO> productLineVOList = ProductLineCopier.INSTANCE.convert(productLineDO);
        return BaseResult.success(productLineVOList);
    }

    @Override
    public BaseResult<List<ProductLineModelVO>> listProductLineModes() {
        List<ProductLineDO> productLineDOList = productLineMapper.selectAllProductLine();

        List<BizDomainDO> bizDomainDOList = bizDomainMapper.selectAllBizDomain();

        List<ProductLineModelVO> productLineVOList = ProductLineCopier.INSTANCE.change(productLineDOList);

        List<ModelDO> modelDOList = modelMapper.selectAllModel();
        List<ModelVO> modelVOList = ModelCopier.INSTANCE.convert(modelDOList);
        Map<Long, List<ModelVO>> modelMap = modelVOList.stream().collect(Collectors.groupingBy(ModelVO::getProductLineId));

        Map<Long, BizDomainDO> bizDomainIdMap = bizDomainDOList.stream()
                .collect(Collectors.toMap(BizDomainDO::getId, Function.identity()));

        productLineVOList.forEach(e -> {
            e.setModels(modelMap.get(e.getId()));
            BizDomainDO bizDomainDO = bizDomainIdMap.get(e.getBizDomainId());
            if (bizDomainDO != null) {
                e.setBizDomainName(bizDomainDO.getName());
            }
        });
        return BaseResult.success(productLineVOList);
    }

    @Override
    public BaseResult<Boolean> add(ProductLineAddReq productLineAddReq) {
        ProductLineDO exists = productLineMapper.selectByName(productLineAddReq.getName());
        AssertUtil.checkState(exists == null, "产品线名称已存在");
        ProductLineDO productLineDO = ProductLineCopier.INSTANCE.convert(productLineAddReq);
        productLineMapper.insert(productLineDO);
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> update(ProductLineModifyReq productLineModifyReq) {
        if (productLineModifyReq.getName() != null) {
            ProductLineDO exists = productLineMapper.selectByName(productLineModifyReq.getName());
            AssertUtil.checkState(exists == null ||
                            Objects.equals(exists.getId(), productLineModifyReq.getId()),
                    "产品线名称已存在");
        }
        ProductLineDO productLineDO = ProductLineCopier.INSTANCE.convert(productLineModifyReq);
        productLineMapper.update(productLineDO);
        if (productLineDO.getProductLineLevel() == null) {
            productLineMapper.updateProductLineLevel(productLineDO.getId(), null);
        }

        return BaseResult.success(true);
    }

    private List<ProductLineVO> build(List<ProductLineDO> productLineDOList, List<BizDomainDO> bizDomainDOList) {
        // 获取业务域及其负责人
        Map<Long, BizDomainDO> bizDomainIdMap = bizDomainDOList.stream()
                .collect(Collectors.toMap(BizDomainDO::getId, Function.identity()));

        List<ProductLineVO> productLineVOList = ProductLineCopier.INSTANCE.convert(productLineDOList);

        // 填充产品线对应业务域负责人信息
        productLineVOList.forEach(e -> {
            BizDomainDO bizDomainDO = bizDomainIdMap.get(e.getBizDomainId());
            e.setBizDomainOwner(bizDomainDO.getOwner());
            e.setBizDomainOwnerId(bizDomainDO.getOwnerId());
            e.setBizDomainName(bizDomainDO.getName());
        });
        return productLineVOList;
    }

    @Override
    public BaseResult<Boolean> updateProductLineListingStatus(UpdateProductLineListingStatusReq req) {
        Long productLineId = req.getProductLineId();
        Integer listingStatus = req.getListingStatus();
        AssertUtil.notNull(productLineId, "产品线ID不能为空");
        AssertUtil.notNull(listingStatus, "上架状态不能为空");
        AssertUtil.checkState(listingStatus == 0 || listingStatus == 1, "上架状态有误");

        ProductLineDO productLineDO = productLineMapper.selectById(productLineId);
        AssertUtil.notNull(productLineDO, "产品线ID有误");

        Integer currentListingStatus = productLineDO.getListingStatus();
        if (listingStatus.equals(currentListingStatus)) {
            return BaseResult.success(true);
        }

        ProductLineDO updateProductLineDO = new ProductLineDO();

        updateProductLineDO.setId(productLineId);
        updateProductLineDO.setListingStatus(listingStatus);

        productLineMapper.update(updateProductLineDO);

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> deleteProductLine(Long productLineId) {
        AssertUtil.notNull(productLineId, "产品线ID不能为空");
        ProductLineDO productLineDO = productLineMapper.selectById(productLineId);
        AssertUtil.notNull(productLineDO, "产品线ID有误");

        // 业务需求判断
        List<BizDemandDO> bizDemandDOList = bizDemandMapper.getByProductLineId(productLineId);
        if(CollectionUtils.isNotEmpty(bizDemandDOList)) {
            throw new BaseBizRuntimeException("该产品线已被使用，不能删除");
        }

        // 产品需求判断
        List<ProductDemandDO> productDemandDOList = productDemandMapper.getByProductLineId(productLineId);
        if(CollectionUtils.isNotEmpty(productDemandDOList)) {
            throw new BaseBizRuntimeException("该产品线已被使用，不能删除");
        }

        // 产研项目判断
        List<ProjectProductLineDO> projectProductLineDOList = projectProductLineMapper.getByProductLineId(productLineId);
        if(CollectionUtils.isNotEmpty(projectProductLineDOList)) {
            throw new BaseBizRuntimeException("该产品线已被使用，不能删除");
        }

        // 线上bug判断
        List<BugOnlineProductLineDO> bugOnlineProductLineDOList = bugOnlineProductLineMapper.getByProductLineId(productLineId);
        if(CollectionUtils.isNotEmpty(bugOnlineProductLineDOList)) {
            throw new BaseBizRuntimeException("该产品线已被使用，不能删除");
        }

        // 线下bug判断
        List<BugOfflineDO> bugOfflineDOList = bugOfflineMapper.getByProductLineId(productLineId);
        if(CollectionUtils.isNotEmpty(bugOfflineDOList)) {
            throw new BaseBizRuntimeException("该产品线已被使用，不能删除");
        }

        // 任务判断
        List<TaskDO> taskDOList = taskMapper.getByProductLineId(productLineId);
        if(CollectionUtils.isNotEmpty(taskDOList)) {
            throw new BaseBizRuntimeException("该产品线已被使用，不能删除");
        }

        // 故障单判断
        List<TroubleTicketDO> troubleTicketDOList = troubleTicketMapper.getByProductLineId(productLineId);
        if(CollectionUtils.isNotEmpty(troubleTicketDOList)) {
            throw new BaseBizRuntimeException("该产品线已被使用，不能删除");
        }

        ProductLineDO updateProductLineDO = new ProductLineDO();

        updateProductLineDO.setId(productLineId);
        updateProductLineDO.setIsDeleted(true);

        productLineMapper.update(updateProductLineDO);

        return BaseResult.success(true);
    }

}
