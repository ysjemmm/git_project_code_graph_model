package com.timevale.forward.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.BizDomainCondition;
import com.timevale.forward.dal.dao.BizDomainMapper;
import com.timevale.forward.dal.dao.ProductLineMapper;
import com.timevale.forward.dal.entity.BizDomainDO;
import com.timevale.forward.dal.entity.ProductLineDO;
import com.timevale.forward.facade.api.client.BizDomainService;
import com.timevale.forward.facade.api.query.BizDomainQueryList;
import com.timevale.forward.facade.api.request.BizDomainAddReq;
import com.timevale.forward.facade.api.request.BizDomainModifyReq;
import com.timevale.forward.facade.api.result.BizDomainVO;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.BizDomainCopier;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.aop.LogPoint;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

import javax.annotation.Resource;
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

    @Override
    public BaseResult<List<BizDomainVO>> bizDomainList() {
        List<BizDomainVO> result = BizDomainCopier.INSTANCE.convert(bizDomainMapper.selectAllBizDomain());
        return BaseResult.success(result);
    }

    @Override
    public BaseResult<PageQueryResult<BizDomainVO>> bizDomainList(BizDomainQueryList bizDomainQueryList) {
        BizDomainCondition condition = BizDomainCopier.INSTANCE.convert(bizDomainQueryList);
        PageHelper.startPage(bizDomainQueryList.pageNum, bizDomainQueryList.pageSize, CommonConstant.DEFAULT_ORDER_BY);

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
}
