package com.timevale.forward.service.impl;


import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.BizDemandLinkProductDemandListCondition;
import com.timevale.forward.dal.condition.ProductBizDemandCondition;
import com.timevale.forward.dal.dao.BizDemandMapper;
import com.timevale.forward.dal.dao.ProductBizDemandMapper;
import com.timevale.forward.dal.dao.ProductDemandMapper;
import com.timevale.forward.dal.entity.BizDemandDO;
import com.timevale.forward.dal.entity.BizDemandLinkProductDemandListDO;
import com.timevale.forward.dal.entity.ProductBizDemandDO;
import com.timevale.forward.facade.api.client.BizDemandProductDemandService;
import com.timevale.forward.facade.api.client.ProductDemandService;
import com.timevale.forward.facade.api.query.BizDemandLinkProductDemandQueryList;
import com.timevale.forward.facade.api.query.BizDemandProductDemandQueryList;
import com.timevale.forward.facade.api.request.BizDemandLinkProductDemandReq;
import com.timevale.forward.facade.api.request.BizDemandUnlinkProductDemandReq;
import com.timevale.forward.facade.api.result.BizDemandLinkProductDemandVO;
import com.timevale.forward.facade.api.result.ProductDemandDetailVO;
import com.timevale.forward.model.enums.PriorityEnum;
import com.timevale.forward.model.enums.ProductDemandStatusEnum;
import com.timevale.forward.service.copy.BizDemandCopier;
import com.timevale.forward.service.copy.ProductBizDemandCopier;
import com.timevale.forward.service.utils.DateUtil;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.StringUtil;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author by YangXu
 * @date 2021/12/23 18:03
 */
@Slf4j
@RestService
public class BizDemandProductDemandServiceImpl implements BizDemandProductDemandService {

    @Resource
    ProductBizDemandMapper productBizDemandMapper;

    @Resource
    ProductDemandMapper productDemandMapper;

    @Resource
    BizDemandMapper bizDemandMapper;

    @Resource
    ProductDemandService productDemandService;

    @Override
    public BaseResult<PageQueryResult<BizDemandLinkProductDemandVO>> linkedProductDemandList(BizDemandProductDemandQueryList bizDemandProductDemandQueryList) {
        // 开始分页
        PageHelper.startPage(bizDemandProductDemandQueryList.pageNum, bizDemandProductDemandQueryList.pageSize);

        List<BizDemandLinkProductDemandListDO> DOList = productDemandMapper.selectByBizDemandId(bizDemandProductDemandQueryList.getBizDemandId());
        List<BizDemandLinkProductDemandVO> VOList = BizDemandCopier.INSTANCE.transform(DOList);

        VOList.forEach( e -> {
            e.setPriorityText(PriorityEnum.getTextByCode(e.getPriority()));
            e.setStatusText(ProductDemandStatusEnum.getTextByCode(e.getStatus()));
        });

        // 返回分页数据
        PageInfo<BizDemandLinkProductDemandListDO> pageInfo = new PageInfo<>(DOList);
        PageQueryResult<BizDemandLinkProductDemandVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(VOList);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);

        return BaseResult.success(pageQueryResult);
    }

    @Override
    public BaseResult<ProductDemandDetailVO> getProductDemand(Long productDemandId) {
        return productDemandService.get(productDemandId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> linkProductDemand(BizDemandLinkProductDemandReq bizDemandLinkProductDemandReq) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        Long bizDemandId = bizDemandLinkProductDemandReq.getId();
        List<Long> productDemandIdList = bizDemandLinkProductDemandReq.getProductDemandIdList();

        BizDemandDO bizDemandDO = bizDemandMapper.selectById(bizDemandId);
        if(bizDemandDO == null){
            throw new BaseBizRuntimeException("不存在该业务需求");
        }

        // 获取当前关联数据
        List<ProductBizDemandDO> list = productBizDemandMapper.select(ProductBizDemandCondition.builder()
                .bizDemandId(bizDemandId)
                .build());

        // 数据转换为集合，判断交集补集
        Set<Long> newLinkData = new HashSet<>(productDemandIdList);
        Map<Long, ProductBizDemandDO> oldLinkDate = list.stream()
                .collect(Collectors.toMap(ProductBizDemandDO::getProductDemandId, Function.identity(), (a, b) -> a));

        // 更新和新增数据的集合
        List<ProductBizDemandDO> insertLinkDate = Lists.newArrayList();
        List<Long> updateLinkDate = Lists.newArrayList();

        // 判断旧数据是否存在新数据中，更新逻辑删除标识
        for (Map.Entry<Long, ProductBizDemandDO> entry : oldLinkDate.entrySet()) {
            if(newLinkData.contains(entry.getKey()) && entry.getValue().getIsDeleted()){
                updateLinkDate.add(entry.getValue().getId());
            }
        }

        // 判断新数据是否在旧数据中，添加新增数据
        for (Long productDemandId : newLinkData) {
            if(!oldLinkDate.containsKey(productDemandId)){
                ProductBizDemandDO productBizDemandDO = ProductBizDemandCopier.INSTANCE.convert(bizDemandId, productDemandId);
                productBizDemandDO.setCreateMan(userInfo.getAlias());
                productBizDemandDO.setCreateManId(userInfo.getId());
                insertLinkDate.add(productBizDemandDO);
            }
        }

        // 新增和更新非空数据
        if(!insertLinkDate.isEmpty()){productBizDemandMapper.inserts(insertLinkDate);}
        if(!updateLinkDate.isEmpty()){productBizDemandMapper.updates(updateLinkDate, false, userInfo.getAlias(), userInfo.getId());}

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> unlinkProductDemand(BizDemandUnlinkProductDemandReq bizDemandUnlinkProductDemandReq) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        // 查询对应数据
        Long bizDemandId = bizDemandUnlinkProductDemandReq.getBizDemandId();
        Long productDemandId = bizDemandUnlinkProductDemandReq.getProductDemandId();

        List<ProductBizDemandDO> list = productBizDemandMapper.select(ProductBizDemandCondition.builder()
                .bizDemandId(bizDemandId)
                .productDemandId(productDemandId)
                .isDeleted(false)
                .build());


        if(list == null){
            throw new BaseBizRuntimeException("不存在对应的关联关系");
        }

        ProductBizDemandDO productBizDemandDO = list.get(0);
        productBizDemandDO.setModifyManId(userInfo.getAlias());
        productBizDemandDO.setModifyManId(userInfo.getId());

        productBizDemandMapper.delete(productBizDemandDO);

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<PageQueryResult<BizDemandLinkProductDemandVO>> matchProductDemandList(BizDemandLinkProductDemandQueryList bizDemandSubProductDemandQueryList) {
        // 开始分页
        PageHelper.startPage(bizDemandSubProductDemandQueryList.pageNum, bizDemandSubProductDemandQueryList.pageSize);
        // 转换查询条件
        BizDemandLinkProductDemandListCondition condition = BizDemandCopier.INSTANCE.convert(bizDemandSubProductDemandQueryList);
        // 通配符处理
        condition.setName(StringUtil.toLikeStr(condition.getName()));
        // 日期处理
        bizDemandSubProductDemandQueryList.setCreateDateStart(DateUtil.getStartOfDay(bizDemandSubProductDemandQueryList.getCreateDateStart()));
        bizDemandSubProductDemandQueryList.setCreateDateEnd(DateUtil.getEndOfDay(bizDemandSubProductDemandQueryList.getCreateDateEnd()));

        // 查询当前业务需求已经关联的产品需求
        List<ProductBizDemandDO> productBizDemandDOList = productBizDemandMapper.select(ProductBizDemandCondition.builder()
                .bizDemandId(bizDemandSubProductDemandQueryList.getBizDemandId())
                .isDeleted(false)
                .build());
        Set<Long> productBizDemandDOSet = productBizDemandDOList.stream().map(ProductBizDemandDO::getProductDemandId).collect(Collectors.toSet());

        // 查询符合条件的产品需求，并过滤已经关联的，已经作废的
        List<BizDemandLinkProductDemandListDO> productDemandDOList = productDemandMapper.selectListOfBizDemandLink(condition);
        productDemandDOList = productDemandDOList.stream().filter(e -> !productBizDemandDOSet.contains(e.getId())).collect(Collectors.toList());
        productDemandDOList = productDemandDOList.stream().filter(e -> !e.getStatus().equals(ProductDemandStatusEnum.INVALID.getCode())).collect(Collectors.toList());
        List<BizDemandLinkProductDemandVO> bizDemandLinkProductDemandVOList = BizDemandCopier.INSTANCE.transform(productDemandDOList);

        // 业务需求状态信息赋值
        bizDemandLinkProductDemandVOList.forEach(e -> {
            e.setPriorityText(PriorityEnum.getTextByCode(e.getPriority()));
            e.setStatusText(ProductDemandStatusEnum.getTextByCode(e.getStatus()));
        });

        PageInfo<BizDemandLinkProductDemandListDO> pageInfo = new PageInfo<>(productDemandDOList);
        PageQueryResult<BizDemandLinkProductDemandVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(bizDemandLinkProductDemandVOList);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);

        return BaseResult.success(pageQueryResult);
    }

}
