package com.timevale.forward.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.BizDemandMapper;
import com.timevale.forward.dal.dao.BugOfflineMapper;
import com.timevale.forward.dal.dao.BugOnlineMapper;
import com.timevale.forward.dal.dao.PersonMapper;
import com.timevale.forward.dal.dao.ProductDemandMapper;
import com.timevale.forward.dal.entity.BaseDO;
import com.timevale.forward.dal.entity.BizDemandDO;
import com.timevale.forward.dal.entity.BugOfflineDO;
import com.timevale.forward.dal.entity.BugOnlineDO;
import com.timevale.forward.dal.entity.PersonDO;
import com.timevale.forward.dal.entity.ProductDemandDO;
import com.timevale.forward.facade.api.client.BizDemandService;
import com.timevale.forward.facade.api.client.PersonService;
import com.timevale.forward.facade.api.client.ProductDemandService;
import com.timevale.forward.facade.api.request.BatchTransferReq;
import com.timevale.forward.facade.api.request.PersonAddReq;
import com.timevale.forward.facade.api.request.RecipientAddReq;
import com.timevale.forward.facade.api.result.PersonVO;
import com.timevale.forward.facade.api.result.TeamMemberVO;
import com.timevale.forward.model.enums.BizDemandStatusEnum;
import com.timevale.forward.model.enums.BugOnlineStatusEnum;
import com.timevale.forward.model.enums.BugStatusEnum;
import com.timevale.forward.model.enums.PersonTypeEnum;
import com.timevale.forward.model.enums.ProductDemandStatusEnum;
import com.timevale.forward.service.component.BugOfflineComponent;
import com.timevale.forward.service.component.BugOnlineComponent;
import com.timevale.forward.service.component.PersonComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.PersonCopier;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.forward.service.observer.event.BizDemandToCopiedMsgEvent;
import com.timevale.forward.service.observer.event.BizResignTransferEvent;
import com.timevale.forward.service.observer.event.BugOfflineResignTransferEvent;
import com.timevale.forward.service.observer.event.BugOnlineResignTransferEvent;
import com.timevale.forward.service.observer.event.PdResignTransferEvent;
import com.timevale.forward.service.observer.event.ProductDemandToCopiedMsgEvent;
import com.timevale.forward.service.observer.publisher.MessageEventPublisher;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.security.facade.response.BaseInfoResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.util.Lists;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Slf4j
@RestService
public class PersonServiceImpl implements PersonService {
    @Resource
    private PersonComponent personComponent;
    @Resource
    private PersonMapper personMapper;
    @Resource
    private InnerUserPersonClient innerUserPersonClient;
    @Resource
    private ProductDemandMapper productDemandMapper;
    @Resource
    private BizDemandMapper bizDemandMapper;
    @Resource
    private BizDemandService bizDemandService;
    @Resource
    private ProductDemandService productDemandService;
    @Resource
    private BugOnlineMapper bugOnlineMapper;
    @Resource
    private BugOfflineMapper bugOfflineMapper;
    @Resource
    private BugOnlineComponent bugOnlineComponent;
    @Resource
    private BugOfflineComponent bugOfflineComponent;

    @Resource
    private MessageEventPublisher messageEventPublisher;

    @Override
    public BaseResult<Boolean> addRecipients(RecipientAddReq recipientAddReq) {
        List<PersonAddReq> recipients = recipientAddReq.getRecipients();
        Integer type = recipientAddReq.getType();
        Long mainId = recipientAddReq.getMainId();
        // 抄送人
        personComponent.update(recipients, mainId, type);
        if (CollectionUtils.isNotEmpty(recipients)) {
            List<String> copiers = recipients.stream().map(PersonAddReq::getUserId).collect(Collectors.toList());
            if (PersonTypeEnum.PRODUCT_DEMAND_CC.equals(type)) {
                ProductDemandDO productDemand = productDemandMapper.get(mainId);
                messageEventPublisher.publish(new ProductDemandToCopiedMsgEvent(
                        this,
                        productDemand.getId(),
                        productDemand.getCreateMan(),
                        copiers,
                        productDemand.getName()
                ));
            }
            if (PersonTypeEnum.BIZ_DEMAND_CC.equals(type)) {
                BizDemandDO bizDemandDO = bizDemandMapper.get(mainId);
                messageEventPublisher.publish(new BizDemandToCopiedMsgEvent(
                        this,
                        bizDemandDO.getId(),
                        bizDemandDO.getSubmitMan(),
                        copiers,
                        bizDemandDO.getName()
                ));
            }
        }
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<List<TeamMemberVO>> getTeamMembers(Long projectId) {
        List<PersonDO> personDOList = personMapper.get(Lists.newArrayList(projectId), PersonTypeEnum.PROJECT_MEMBER.getCode());
        if (CollectionUtils.isEmpty(personDOList)) {
            return BaseResult.success(Lists.emptyList());
        }
        List<String> accounts = personDOList.stream().map(PersonDO::getUserId).collect(Collectors.toList());
        //在职员工
        List<BaseInfoResponse> personByAccountNew = innerUserPersonClient.getPersonByAccountNew(accounts);
        List<TeamMemberVO> list = personByAccountNew.stream().map(a -> {
            TeamMemberVO teamMemberVO = new TeamMemberVO();
            teamMemberVO.setUserId(a.getAccount());
            teamMemberVO.setUserName(a.getAlias() + CommonConstant.JOIN_LINE + a.getName());
            teamMemberVO.setQuited(Integer.valueOf(1).equals(a.getStatus()));
            return teamMemberVO;
        }).filter(e -> StringUtils.isNotEmpty(e.getUserId())).collect(Collectors.toList());
        return BaseResult.success(list);
    }

    @Override
    public BaseResult<List<PersonVO>> getLastCopior() {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        List<ProductDemandDO> productDemandDOList = productDemandMapper.getByOwnerId(userInfo.getId());
        if (CollectionUtils.isEmpty(productDemandDOList)) {
            return BaseResult.success(Lists.emptyList());
        }

        List<Long> pids = productDemandDOList.stream().map(ProductDemandDO::getId).collect(Collectors.toList());
        List<PersonDO> personDOList = personMapper.get(pids, PersonTypeEnum.PRODUCT_DEMAND_CC.getCode());
        if (CollectionUtils.isEmpty(personDOList)) {
            return BaseResult.success(Lists.emptyList());
        }
        List<String> accounts = personDOList.stream().map(PersonDO::getUserId).collect(Collectors.toList());
        List<String> onJobAccounts = innerUserPersonClient.batchGetStaffs(accounts, false);
        personDOList.sort(Comparator.comparing(PersonDO::getCreateDate).reversed());
        List<PersonDO> resultList = new ArrayList<>();
        List<String> distinct = new ArrayList<>();
        personDOList.forEach(a -> {
            //返回前10个在职的抄送人
            if (resultList.size() < 10 && !distinct.contains(a.getUserId()) && onJobAccounts.contains(a.getUserId())) {
                resultList.add(a);
                distinct.add(a.getUserId());
            }
        });
        List<PersonVO> personVOList = PersonCopier.INSTANCE.transform(resultList);
        return BaseResult.success(personVOList);
    }

    @Override
    public BaseResult<Void> resignNotice(String account) {
        log.info("[PersonServiceImpl.resignNotice]account={}", account);
        if (StrUtil.isEmpty(account)) {
            return BaseResult.success();
        }

        // 查询个人信息
        BaseInfoResponse selfInfo = innerUserPersonClient.getSelfInfo(account, true);
        if (selfInfo == null) {
            log.error("[PersonServiceImpl.resignNotice] 无法查询到离职人员的信息，account: {}", account);
            return BaseResult.success();
        }

        // 查询上级信息
        BaseInfoResponse managerInfo = Optional.of(selfInfo)
                .map(BaseInfoResponse::getManagerAccount)
                .map(manager -> innerUserPersonClient.getSelfInfo(manager, false))
                .orElse(null);
        if (managerInfo == null) {
            log.error("[PersonServiceImpl.resignNotice] 无法查询到离职人员上级的信息，无法转交需求，account: {}", account);
            return BaseResult.success();
        }
        log.info("[PersonServiceImpl.resignNotice]manager={}", managerInfo.getAccount());

        // 业务需求转交
        {
            List<BizDemandDO> bds = bizDemandMapper.getByReceiveManId(account);
            List<Long> bdIds = bds.stream()
                    .filter(e -> BizDemandStatusEnum.unfinished(e.getStatus()))
                    .map(BaseDO::getId)
                    .collect(Collectors.toList());

            if (CollUtil.isNotEmpty(bdIds)) {
                BatchTransferReq req = new BatchTransferReq()
                        .setType(0)
                        .setReceiveMan(managerInfo.getAlias() + "-" + managerInfo.getName())
                        .setReceiveManId(managerInfo.getAccount())
                        .setIdList(bdIds);
                bizDemandService.bizDemandBatchTransferReceiveMan(req);
                new BizResignTransferEvent(this,
                        bdIds.size(),
                        selfInfo.getAlias() + "-" + selfInfo.getName(),
                        managerInfo.getAccount())
                        .send();
            }
        }

        // 产品需求转交
        {
            List<ProductDemandDO> pds = productDemandMapper.getByOwnerId(account);
            List<Long> pdIds = pds.stream()
                    .filter(e -> ProductDemandStatusEnum.unfinished(e.getStatus()))
                    .map(BaseDO::getId)
                    .collect(Collectors.toList());

            if (CollUtil.isNotEmpty(pdIds)) {
                BatchTransferReq req = new BatchTransferReq()
                        .setType(0)
                        .setReceiveMan(managerInfo.getAlias() + "-" + managerInfo.getName())
                        .setReceiveManId(managerInfo.getAccount())
                        .setIdList(pdIds);
                productDemandService.productDemandBatchTransferReceiveMan(req);
                new PdResignTransferEvent(this,
                        pdIds.size(),
                        selfInfo.getAlias() + "-" + selfInfo.getName(),
                        managerInfo.getAccount())
                        .send();
            }
        }

        boolean bugOnlineTransfer = false;
        // 线上bug经办人转交
        {
            List<BugOnlineDO> bugs = bugOnlineMapper.getByOperatorId(account);
            List<Long> bugIds = bugs.stream()
                    .filter(e -> BugOnlineStatusEnum.unfinished(e.getStatus()))
                    .map(BaseDO::getId)
                    .collect(Collectors.toList());
            if (CollUtil.isNotEmpty(bugIds)) {
                bugOnlineTransfer = true;
                bugOnlineComponent.transferOperator(bugIds, managerInfo.getAlias() + "-" + managerInfo.getName(), managerInfo.getAccount());
            }
        }
        // 线上bug提出人转交
        {
            List<BugOnlineDO> bugs = bugOnlineMapper.getByProposerId(account);
            List<Long> bugIds = bugs.stream()
                    .filter(e -> BugOnlineStatusEnum.unfinished(e.getStatus()))
                    .map(BaseDO::getId)
                    .collect(Collectors.toList());
            if (CollUtil.isNotEmpty(bugIds)) {
                bugOnlineComponent.transferProposer(bugIds, managerInfo.getAlias() + "-" + managerInfo.getName(), managerInfo.getAccount());
                bugOnlineTransfer = true;
            }
        }
        if (bugOnlineTransfer) {
            new BugOnlineResignTransferEvent(
                    this,
                    selfInfo.getAlias() + "-" + selfInfo.getName(),
                    managerInfo.getAccount()
            ).send();
        }

        boolean bugOfflineTransfer = false;
        // 线下bug经办人转交
        {
            List<BugOfflineDO> bugs = bugOfflineMapper.getByOperatorId(account);
            List<Long> bugIds = bugs.stream()
                    .filter(e -> BugStatusEnum.unfinished(e.getStatus()))
                    .map(BaseDO::getId)
                    .collect(Collectors.toList());
            if (CollUtil.isNotEmpty(bugIds)) {
                bugOfflineTransfer = true;
                bugOfflineComponent.transferOperator(bugIds, managerInfo.getAlias() + "-" + managerInfo.getName(), managerInfo.getAccount());
            }
        }
        // 线下bug提出人转交
        {
            List<BugOfflineDO> bugs = bugOfflineMapper.getByProposerId(account);
            List<Long> bugIds = bugs.stream()
                    .filter(e -> BugStatusEnum.unfinished(e.getStatus()))
                    .map(BaseDO::getId)
                    .collect(Collectors.toList());
            if (CollUtil.isNotEmpty(bugIds)) {
                bugOfflineTransfer = true;
                bugOfflineComponent.transferProposer(bugIds, managerInfo.getAlias() + "-" + managerInfo.getName(), managerInfo.getAccount());
            }
        }
        // 线下bug转交通知
        if (bugOfflineTransfer) {
            new BugOfflineResignTransferEvent(
                    this,
                    selfInfo.getAlias() + "-" + selfInfo.getName(),
                    managerInfo.getAccount()
            ).send();
        }

        return BaseResult.success();
    }

}
