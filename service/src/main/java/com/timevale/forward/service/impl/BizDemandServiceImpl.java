package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.BizDemandMapper;
import com.timevale.forward.dal.dao.FileMapper;
import com.timevale.forward.dal.dao.PersonMapper;
import com.timevale.forward.dal.entity.BizDemandDO;
import com.timevale.forward.dal.entity.PersonDO;
import com.timevale.forward.facade.api.client.BizDemandService;
import com.timevale.forward.facade.api.query.BizDemandQueryList;
import com.timevale.forward.facade.api.query.BizDemandSubProductDemandQueryList;
import com.timevale.forward.facade.api.request.BizDemandAddReq;
import com.timevale.forward.facade.api.request.BizDemandModifyReq;
import com.timevale.forward.facade.api.result.BizDemandDetailVO;
import com.timevale.forward.facade.api.result.BizDemandVO;
import com.timevale.forward.facade.api.result.ProductDemandVO;
import com.timevale.forward.model.enums.BizDemandStatusEnum;
import com.timevale.forward.service.copy.BizDemandCopier;
import com.timevale.forward.service.copy.PersonCopier;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author by YangXu
 * @Date 2021/12/14 15:05
 */
@Slf4j
@RestService
public class BizDemandServiceImpl implements BizDemandService {

    // @Resource
    BizDemandMapper bizDemandMapper;

    // @Resource
    PersonMapper personMapper;

    // @Resource
    FileMapper fileMapper;

    @Override
    public BaseResult<PageQueryResult<BizDemandVO>> list(BizDemandQueryList bizDemandQueryList) {
        return null;
    }

    @Override
    public BaseResult<Boolean> updateStatus(Long bizDemandId) {
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> addBizDemand(BizDemandAddReq bizDemandAddReq) {
        // 新增业务需求
        BizDemandDO bizDemandDO = BizDemandCopier.INSTANCE.convert(bizDemandAddReq);
        bizDemandMapper.insert(bizDemandDO);

        // 添加抄送人
        long bizDemandId = bizDemandDO.getId();
        List<PersonDO> personDOList = PersonCopier.INSTANCE.convert(bizDemandAddReq.getRecipients());
        for (PersonDO personDO : personDOList) {
            personDO.setBizDemandId(bizDemandId);
        }
        personMapper.inserts(personDOList);

        // 接收人通知（待实现）

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<BizDemandDetailVO> getBizDemandById(Long bizDemandId) {
        return null;
    }

    @Override
    public BaseResult<Boolean> modify(BizDemandModifyReq bizDemandModifyReq) {
        return null;
    }

    @Override
    public BaseResult<Boolean> agree(Long bizDemandId, Integer planReleaseDate) {
        BizDemandDO bizDemandDO = bizDemandMapper.selectById(bizDemandId);
        bizDemandDO.setStatus(BizDemandStatusEnum.RECEIVED.getCode());
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> reject(Long bizDemandId, Integer reason) {
        return null;
    }

    @Override
    public BaseResult<Boolean> transfer(Long bizDemandId, String receiveMan) {
        return null;
    }

    @Override
    public BaseResult<PageQueryResult<ProductDemandVO>> matchProductDemandList(BizDemandSubProductDemandQueryList bizDemandSubProductDemandQueryList) {
        return null;
    }

    @Override
    public BaseResult<Boolean> linkOrUnLinkProductDemand(List<Long> productIdList) {
        return null;
    }
}
