package com.timevale.forward.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.ProjectAcceptanceListCondition;
import com.timevale.forward.dal.dao.ProjectAcceptanceMapper;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.entity.ProjectAcceptanceDO;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.facade.api.client.ProjectAcceptanceService;
import com.timevale.forward.facade.api.query.ProjectAcceptanceQueryList;
import com.timevale.forward.facade.api.request.PersonAddReq;
import com.timevale.forward.facade.api.request.ProjectAcceptanceAddReq;
import com.timevale.forward.facade.api.request.ProjectAcceptanceModifyReq;
import com.timevale.forward.facade.api.result.ProjectAcceptanceVO;
import com.timevale.forward.model.enums.FlowStatusEnum;
import com.timevale.forward.model.enums.YesOrNoEnum;
import com.timevale.forward.service.component.SqlOrderComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.ProjectAcceptanceCopier;
import com.timevale.forward.service.observer.event.*;
import com.timevale.forward.service.observer.publisher.MessageEventPublisher;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.assertj.core.util.Lists;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Slf4j
@RestService
public class ProjectAcceptanceServiceImpl implements ProjectAcceptanceService {


    @Resource
    private ProjectAcceptanceMapper projectAcceptanceMapper;

    @Resource
    private MessageEventPublisher messageEventPublisher;

    @Resource
    private ProjectMapper projectMapper;

    @Resource
    private SqlOrderComponent sqlOrderComponent;


    @Override
    public BaseResult<List<ProjectAcceptanceVO>> acceptList(Long projectId) {
        log.info("项目验收列表,参数:{}", projectId);
        List<Integer> status = Lists.newArrayList(FlowStatusEnum.AUDITING.getCode(), FlowStatusEnum.COMPLETE.getCode(), FlowStatusEnum.REJECT.getCode());
        ProjectAcceptanceListCondition c = ProjectAcceptanceListCondition.builder().status(status).projectId(projectId).build();

        List<ProjectAcceptanceDO> list = projectAcceptanceMapper.list(c);
        Map<String, List<ProjectAcceptanceDO>> groupMap = list.stream().collect(Collectors.groupingBy(ProjectAcceptanceDO::getAcceptorId));
        List<ProjectAcceptanceDO> result=new ArrayList<>();
        //同一验收人取最新时间数据
        groupMap.forEach((k,v)->{
            List<ProjectAcceptanceDO> order = v.stream().sorted(Comparator.comparing(ProjectAcceptanceDO::getCreateDate).reversed()).collect(Collectors.toList());
            result.add(order.get(0));
        });

        List<ProjectAcceptanceVO> projectAcceptanceVOList = ProjectAcceptanceCopier.INSTANCE.convert(result);

        return BaseResult.success(projectAcceptanceVOList);
    }

    @Override
    public BaseResult<PageQueryResult<ProjectAcceptanceVO>> history(ProjectAcceptanceQueryList query) {
        log.info("项目验收历史,参数:{}", query);
        ProjectAcceptanceListCondition c = ProjectAcceptanceListCondition.builder().projectId(query.getProjectId()).build();

        String collation = sqlOrderComponent.build(query.getOrderFiled(), query.getOrderCollation());
        PageHelper.startPage(query.getPageNum(), query.getPageSize(), collation);
        List<ProjectAcceptanceDO> list = projectAcceptanceMapper.list(c);

        List<ProjectAcceptanceVO> projectAcceptanceVOList = ProjectAcceptanceCopier.INSTANCE.convert(list);

        PageInfo<ProjectAcceptanceDO> pageInfo = new PageInfo<>(list);
        PageQueryResult<ProjectAcceptanceVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(projectAcceptanceVOList);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);
        return BaseResult.success(pageQueryResult);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> add(ProjectAcceptanceAddReq req) {
        log.info("项目验收发起,参数:{}", req);
        ProjectDO projectDO = projectMapper.get(req.getProjectId());
        if(!YesOrNoEnum.YES.getCode().equals(projectDO.getIsAcceptance())){
            throw new BaseBizRuntimeException("项目处于无需验收中,不能发起项目验收,请修改后重试");
        }
        ProjectAcceptanceListCondition c = ProjectAcceptanceListCondition.builder().projectId(req.getProjectId()).build();
        List<ProjectAcceptanceDO> list = projectAcceptanceMapper.list(c);

        List<String> allAcceptorIds = list.stream().map(ProjectAcceptanceDO::getAcceptorId).collect(Collectors.toList());
        List<String> auditAcceptorIds = list.stream().filter(a -> FlowStatusEnum.AUDITING.getCode().equals(a.getStatus())).map(ProjectAcceptanceDO::getAcceptorId).collect(Collectors.toList());

        List<PersonAddReq> acceptors = req.getAcceptors();
        Set<String> newAcceptorIds = acceptors.stream().map(PersonAddReq::getUserId).collect(Collectors.toSet());
        newAcceptorIds.addAll(allAcceptorIds);

        if (newAcceptorIds.size() > 20) {
            throw new BaseBizRuntimeException("该项目累计验收人员已超过20人,请修改后重试");
        }

        //正在验收中的人员无需再次发起验收
        List<PersonAddReq> filter = acceptors.stream().filter(a -> !auditAcceptorIds.contains(a.getUserId())).collect(Collectors.toList());
        List<ProjectAcceptanceDO> projectAcceptanceDOList = filter.stream().map(a -> {
            ProjectAcceptanceDO o = new ProjectAcceptanceDO();
            o.setProjectId(req.getProjectId());
            o.setAcceptor(a.getUserName());
            o.setAcceptorId(a.getUserId());
            return o;
        }).collect(Collectors.toList());

        if(CollectionUtils.isEmpty(projectAcceptanceDOList)){
            return BaseResult.success(true);
        }
        projectAcceptanceMapper.batchInsert(projectAcceptanceDOList);


        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        String operator = userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName();

        List<String> filterIds = filter.stream().map(PersonAddReq::getUserId).collect(Collectors.toList());
        messageEventPublisher.publish(new ProjectAcceptanceStartMsgEvent(
                this,
                operator,
                projectDO.getId(),
                projectDO.getName(),
                filterIds
        ));
        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> accept(ProjectAcceptanceModifyReq req) {
        log.info("项目验收通过,参数:{}", req);
        acceptOrUnAccept(req, true);
        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> unAccept(ProjectAcceptanceModifyReq req) {
        log.info("项目验收不通过,参数:{}", req);
        acceptOrUnAccept(req, false);
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> remind(Long id) {
        log.info("项目验收催办,参数:{}", id);
        ProjectAcceptanceDO projectAcceptanceDO = projectAcceptanceMapper.get(id);
        if (!FlowStatusEnum.AUDITING.getCode().equals(projectAcceptanceDO.getStatus())) {
            throw new BaseBizRuntimeException("状态不是验收中,不能进行催办操作,请刷新后重试");
        }
        ProjectDO projectDO = projectMapper.get(projectAcceptanceDO.getProjectId());
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        String operator = userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName();
        messageEventPublisher.publish(new ProjectAcceptanceRemindMsgEvent(
                this,
                operator,
                projectDO.getId(),
                projectDO.getName(),
                Lists.newArrayList(projectAcceptanceDO.getAcceptorId())
        ));
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> revoke(Long id) {
        log.info("项目验收撤回,参数:{}", id);
        ProjectAcceptanceDO projectAcceptanceDO = projectAcceptanceMapper.get(id);
        if (!FlowStatusEnum.AUDITING.getCode().equals(projectAcceptanceDO.getStatus())) {
            throw new BaseBizRuntimeException("状态不是验收中,不能进行撤回操作,请刷新后重试");
        }
        projectAcceptanceDO.setStatus(FlowStatusEnum.WITHDRAW.getCode());
        projectAcceptanceMapper.update(projectAcceptanceDO);

        ProjectDO projectDO = projectMapper.get(projectAcceptanceDO.getProjectId());
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        String operator = userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName();
        messageEventPublisher.publish(new ProjectAcceptanceRevokeMsgEvent(
                this,
                operator,
                projectDO.getId(),
                projectDO.getName(),
                Lists.newArrayList(projectAcceptanceDO.getAcceptorId())
        ));
        return BaseResult.success(true);
    }

    private void acceptOrUnAccept(ProjectAcceptanceModifyReq req, boolean accept) {
        ProjectAcceptanceDO projectAcceptanceDO = projectAcceptanceMapper.get(req.getId());
        if (!FlowStatusEnum.AUDITING.getCode().equals(projectAcceptanceDO.getStatus())) {
            throw new BaseBizRuntimeException("状态不是验收中,不能进行验收操作,请刷新后重试");
        }
        ProjectAcceptanceDO o = new ProjectAcceptanceDO();
        o.setId(req.getId());
        o.setDesc(req.getDesc());
        o.setAcceptDate(new Date());
        o.setStatus(accept ? FlowStatusEnum.COMPLETE.getCode() : FlowStatusEnum.REJECT.getCode());
        projectAcceptanceMapper.update(o);

        ProjectDO projectDO = projectMapper.get(projectAcceptanceDO.getProjectId());
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        String operator = userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName();
        if (accept) {
            messageEventPublisher.publish(new ProjectAcceptanceAcceptMsgEvent(
                    this,
                    operator,
                    projectDO.getId(),
                    projectDO.getName(),
                    Lists.newArrayList(projectAcceptanceDO.getCreateManId())
            ));
        } else {
            messageEventPublisher.publish(new ProjectAcceptanceUnAcceptMsgEvent(
                    this,
                    operator,
                    projectDO.getId(),
                    projectDO.getName(),
                    Lists.newArrayList(projectAcceptanceDO.getCreateManId())
            ));
        }
    }
}
