package com.timevale.forward.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.BizDemandListCondition;
import com.timevale.forward.dal.condition.ProductBizDemandCondition;
import com.timevale.forward.dal.dao.BizDemandMapper;
import com.timevale.forward.dal.dao.FileMapper;
import com.timevale.forward.dal.dao.PersonMapper;
import com.timevale.forward.dal.dao.ProductBizDemandMapper;
import com.timevale.forward.dal.entity.BizDemandDO;
import com.timevale.forward.dal.entity.PersonDO;
import com.timevale.forward.dal.entity.ProductBizDemandDO;
import com.timevale.forward.facade.api.client.BizDemandService;
import com.timevale.forward.facade.api.query.BizDemandQueryList;
import com.timevale.forward.facade.api.query.BizDemandSubProductDemandQueryList;
import com.timevale.forward.facade.api.request.BizDemandAddReq;
import com.timevale.forward.facade.api.request.BizDemandModifyReq;
import com.timevale.forward.facade.api.result.BizDemandDetailVO;
import com.timevale.forward.facade.api.result.BizDemandVO;
import com.timevale.forward.facade.api.result.ProductDemandVO;
import com.timevale.forward.model.enums.BizDemandStatusEnum;
import com.timevale.forward.model.enums.PersonTypeEnum;
import com.timevale.forward.service.copy.BizDemandCopier;
import com.timevale.forward.service.copy.PersonCopier;
import com.timevale.forward.service.copy.ProductBizDemandCopier;
import com.timevale.forward.service.integration.erp.ErpMessageClient;
import com.timevale.forward.service.integration.erp.model.ActionCardMsg;
import com.timevale.forward.service.integration.erp.model.MarkdownMsg;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author by YangXu
 * @date 2021/12/14 15:05
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

    // @Resource
    ProductBizDemandMapper productBizDemandMapper;

    // @Resource
    ErpMessageClient erpMessageClient;

    @Override
    public BaseResult<PageQueryResult<BizDemandVO>> list(BizDemandQueryList bizDemandQueryList) {
        // 开始分页
        PageHelper.startPage(bizDemandQueryList.pageNum, bizDemandQueryList.pageSize);

        // 查询对应业务需求，并转换为VO
        BizDemandListCondition bizDemandListCondition = BizDemandCopier.INSTANCE.convert(bizDemandQueryList);
        List<BizDemandDO> bizDemandDOList = bizDemandMapper.select(bizDemandListCondition);
        List<BizDemandVO> bizDemandVOList = BizDemandCopier.INSTANCE.convert(bizDemandDOList);

        // 转换后返回数据
        return BaseResult.success(BizDemandCopier.INSTANCE.convert(
                ResultUtil.pageSuccess(new PageInfo<>(bizDemandVOList))));
    }

    @Override
    public BaseResult<Boolean> updateStatus(Long bizDemandId) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        // 修改业务需求状态 —— 作废
        BizDemandDO bizDemandDO = bizDemandMapper.selectById(bizDemandId);
        bizDemandDO.setStatus(BizDemandStatusEnum.INVALID.getCode());
        bizDemandDO.setModifyMan(userInfo.getAlias());
        bizDemandDO.setModifyManId(userInfo.getId());
        bizDemandMapper.update(bizDemandDO);

        // 接收人通知（待实现）
        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> addBizDemand(BizDemandAddReq bizDemandAddReq) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        // 新增业务需求
        BizDemandDO bizDemandDO = BizDemandCopier.INSTANCE.convert(bizDemandAddReq);
        bizDemandDO.setStatus(BizDemandStatusEnum.RECEIVED.getCode());
        bizDemandDO.setCreateMan(userInfo.getAlias());
        bizDemandDO.setCreateManId(userInfo.getId());
        bizDemandMapper.insert(bizDemandDO);

        // 添加抄送人
        long bizDemandId = bizDemandDO.getId();
        List<PersonDO> personDOList = PersonCopier.INSTANCE.convert(bizDemandAddReq.getRecipients());
        for (PersonDO personDO : personDOList) {
            personDO.setBizDemandId(bizDemandId);
            personDO.setType(PersonTypeEnum.BIZ_DEMAND_CC.getCode());
            personDO.setCreateMan(userInfo.getAlias());
            personDO.setCreateManId(userInfo.getId());
        }
        personMapper.inserts(personDOList);

        // 接收人通知（待实现）

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<BizDemandDetailVO> getBizDemandById(Long bizDemandId) {
        BizDemandDO bizDemandDO = bizDemandMapper.selectById(bizDemandId);
        BizDemandDetailVO bizDemandDetailVO = BizDemandCopier.INSTANCE.convert(bizDemandDO);
        return BaseResult.success(bizDemandDetailVO);
    }

    @Override
    public BaseResult<Boolean> modify(BizDemandModifyReq bizDemandModifyReq) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        // 修改业务需求
        BizDemandDO bizDemandDO = BizDemandCopier.INSTANCE.convert(bizDemandModifyReq);
        bizDemandDO.setModifyMan(userInfo.getAlias());
        bizDemandDO.setModifyManId(userInfo.getId());
        bizDemandMapper.update(bizDemandDO);
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> agree(Long bizDemandId, Integer planReleaseDate) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        // 修改业务需求状态 —— 接收，添加预期上线时间
        BizDemandDO bizDemandDO = bizDemandMapper.selectById(bizDemandId);
        bizDemandDO.setStatus(BizDemandStatusEnum.RECEIVED.getCode());
        bizDemandDO.setPlanReleaseDate(planReleaseDate);
        bizDemandDO.setModifyMan(userInfo.getAlias());
        bizDemandDO.setModifyManId(userInfo.getId());
        bizDemandMapper.update(bizDemandDO);

        // 通知需求提交人（待实现）

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> reject(Long bizDemandId, Byte reason) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        // 修改业务需求状态 —— 驳回，添加驳回原因
        BizDemandDO bizDemandDO = bizDemandMapper.selectById(bizDemandId);
        bizDemandDO.setStatus(BizDemandStatusEnum.REJECT.getCode());
        bizDemandDO.setReason(reason);
        bizDemandDO.setModifyMan(userInfo.getAlias());
        bizDemandDO.setModifyManId(userInfo.getId());
        bizDemandMapper.update(bizDemandDO);

        // 提交人通知（待实现）
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> transfer(Long bizDemandId, String receiveMan) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        // 转交：修改接收人
        BizDemandDO bizDemandDO = bizDemandMapper.selectById(bizDemandId);
        bizDemandDO.setReceiveMan(receiveMan);
        bizDemandDO.setModifyMan(userInfo.getAlias());
        bizDemandDO.setModifyManId(userInfo.getId());
        bizDemandMapper.update(bizDemandDO);

        // 转交人通知（待实现）
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<PageQueryResult<ProductDemandVO>> matchProductDemandList(BizDemandSubProductDemandQueryList bizDemandSubProductDemandQueryList) {
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> linkOrUnLinkProductDemand(Long bizDemandId, List<Long> productIdList) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        // 获取当前关联数据
        List<ProductBizDemandDO> list = productBizDemandMapper.select(ProductBizDemandCondition.builder()
                .bizDemandId(bizDemandId)
                .build());

        // 数据转换为集合，判断交集补集
        Set<Long> newLinkData = new HashSet<>(productIdList);
        Map<Long, Boolean> oldLinkDate = list.stream()
                .collect(Collectors.toMap(ProductBizDemandDO::getProductDemandId, ProductBizDemandDO::getIsDeleted));

        // 更新和新增数据的集合
        List<ProductBizDemandDO> insertLinkDate = Lists.newArrayList();
        List<ProductBizDemandDO> updateLinkDate = Lists.newArrayList();

        // 判断旧数据是否存在新数据中，更新逻辑删除标识
        for (Map.Entry<Long, Boolean> entry : oldLinkDate.entrySet()) {
            Boolean isDeleted = null;
            if(newLinkData.contains(entry.getKey())){
                if(entry.getValue()){
                    isDeleted = false;
                }
            }else{
                if(!entry.getValue()){
                   isDeleted = true;
                }
            }
            if(isDeleted != null){
                ProductBizDemandDO productBizDemandDO = ProductBizDemandCopier.INSTANCE.convert(bizDemandId, entry.getKey(), isDeleted);
                productBizDemandDO.setModifyMan(userInfo.getAlias());
                productBizDemandDO.setModifyManId(userInfo.getId());
                updateLinkDate.add(productBizDemandDO);
            }
        }
        // 判断新数据是否在旧数据中，添加新增数据
        for (Long productId : newLinkData) {
            if(!oldLinkDate.containsKey(productId)){
                ProductBizDemandDO productBizDemandDO = ProductBizDemandCopier.INSTANCE.convert(bizDemandId, productId, false);
                productBizDemandDO.setCreateMan(userInfo.getAlias());
                productBizDemandDO.setCreateManId(userInfo.getId());
                insertLinkDate.add(productBizDemandDO);
            }
        }

        // 新增和更新数据
        productBizDemandMapper.inserts(insertLinkDate);
        productBizDemandMapper.updates(updateLinkDate);

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> testNotice(Integer type) {
        if(type.equals(1)){
            erpMessageClient.sendMarkdownMsg(MarkdownMsg.builder()
                    .receivers(Lists.newArrayList("yangxu"))
                    .title("test")
                    .content("测试内容")
                    .build());
        }else{
            erpMessageClient.sendActionCardMsg(ActionCardMsg.builder()
                    .receivers(Lists.newArrayList("yangxu"))
                    .title("test2")
                    .markdown("测试内容2")
                    .singleTitle("跳转连接文案")
                    .singleUrl("https://weibo.com/")
                    .build());
        }

        return BaseResult.success(true);
    }
}
