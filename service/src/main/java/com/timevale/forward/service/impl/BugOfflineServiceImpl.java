package com.timevale.forward.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.BugOfflineListCondition;
import com.timevale.forward.dal.condition.PersonListCondition;
import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.facade.api.client.BugOfflineService;
import com.timevale.forward.facade.api.query.BugOfflineQueryList;
import com.timevale.forward.facade.api.request.*;
import com.timevale.forward.facade.api.result.*;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.service.component.FileComponent;
import com.timevale.forward.service.component.PersonComponent;
import com.timevale.forward.service.component.TaskComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.*;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.forward.service.observer.event.BugOfflineAddMsg;
import com.timevale.forward.service.observer.event.BugOfflineUpdateMsg;
import com.timevale.forward.service.observer.publisher.MessageEventPublisher;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import com.timevale.security.facade.request.AccountRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.assertj.core.util.Lists;
import org.assertj.core.util.Sets;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Slf4j
@RestService
public class BugOfflineServiceImpl implements BugOfflineService {

    @Resource
    MessageEventPublisher messageEventPublisher;
    @Resource
    private TaskComponent taskComponent;
    @Resource
    private InnerUserPersonClient innerUserPersonClient;
    @Resource
    private PersonMapper personMapper;
    @Resource
    private ProductLineMapper productLineMapper;
    @Resource
    private FileComponent fileComponent;
    @Resource
    private PersonComponent personComponent;
    @Resource
    private ProjectMapper projectMapper;
    @Resource
    private BugOfflineMapper bugOfflineMapper;
    @Resource
    private BugLogMapper bugLogMapper;

    @Resource
    private FileMapper fileMapper;

    @Resource
    private BizDomainMapper bizDomainMapper;

    @Resource
    private CommentMapper commentMapper;


    @Override
    public BaseResult<PageQueryResult<BugOfflineVO>> list(BugOfflineQueryList bugOfflineQueryList) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        // 转换查询条件
        BugOfflineListCondition condition = BugOfflineCopier.INSTANCE.convert(bugOfflineQueryList);

        // 标志是否有对应数据
        boolean resultIsEmpty = false;
        // 根据tabs添加不同的效果
        String ascription = bugOfflineQueryList.getAscription();
        if (AscriptionEnum.CURRENT_USER.toString().equals(ascription)) {
            condition.setProposerIds(Lists.newArrayList(userInfo.getId()));
        } else if (AscriptionEnum.RECEIVE.toString().equals(ascription)) {
            condition.setOperatorIds(Lists.newArrayList(userInfo.getId()));
        } else if (AscriptionEnum.COPIER.toString().equals(ascription)) {
            condition.setCopier(userInfo.getId());
        } else {
            List<String> teamMemberIdList = innerUserPersonClient.getAllMyStaffWithSelf(userInfo.getId(), true);
            if (AscriptionEnum.TEAM_SUBMIT.toString().equals(ascription)) {
                Set<String> createIdSet = Sets.newHashSet(condition.getProposerIds());
                if (!createIdSet.isEmpty()) {
                    teamMemberIdList = teamMemberIdList.stream().filter(createIdSet::contains).collect(Collectors.toList());
                    resultIsEmpty = teamMemberIdList.isEmpty();
                }
            } else if (AscriptionEnum.TEAM_RECEIVE.toString().equals(ascription)) {
                Set<String> createIdSet = Sets.newHashSet(condition.getOperatorIds());
                if (!createIdSet.isEmpty()) {
                    teamMemberIdList = teamMemberIdList.stream().filter(createIdSet::contains).collect(Collectors.toList());
                    resultIsEmpty = teamMemberIdList.isEmpty();
                }
            }
        }
        if (resultIsEmpty) {
            return BaseResult.success(ResultUtil.pageEmpty());
        }

        // 开始分页
        PageHelper.startPage(bugOfflineQueryList.pageNum, bugOfflineQueryList.pageSize, CommonConstant.DEFAULT_ORDER_BY);

        // 查询并转换
        List<BugOfflineDO> bugOfflineDOList = Lists.newArrayList();
        List<BugOfflineVO> bugOfflineVOList = bugOfflineDOList.stream().map(BugOfflineCopier.INSTANCE::convert).collect(Collectors.toList());

        // 信息填充
        bugOfflineVOList.forEach(e -> {
            e.setStatusName(BugStatusEnum.getTextByCode(e.getStatus()));
            e.setPriorityName(PriorityEnum.getTextChineseByCode(e.getPriority()));
            e.setSourceName(BugSourceEnum.getTextByCode(e.getSource()));
            e.setBelongName(BugBelongEnum.getTextByCode(e.getBelong()));
            e.setEnvName(BugBelongEnum.getTextByCode(e.getEnv()));
        });

        // 返回分页数据
        PageInfo<BugOfflineDO> pageInfo = new PageInfo<>(bugOfflineDOList);
        PageQueryResult<BugOfflineVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(bugOfflineVOList);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);

        return BaseResult.success(pageQueryResult);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> add(BugOfflineAddReq bugOfflineAddReq) {
        //1.接收表单参数,状态为:bug打开,经办人所选用户,上一阶段经办人为bug提出人,bug数据入库
        BugOfflineDO bugOfflineDO = BugOfflineCopier.INSTANCE.convert(bugOfflineAddReq);
        bugOfflineDO.setOpenCount(1);
        bugOfflineDO.setStatus(BugStatusEnum.OPEN.getCode());
        bugOfflineMapper.insert(bugOfflineDO);

        //2.若存在附件,附件数据入库
        List<FileAddReq> files = bugOfflineAddReq.getFiles();
        fileComponent.add(files, bugOfflineDO.getId(), FileTypeEnum.BUG_OFFLINE.getCode());

        //3.若存在抄送人,抄送人数据入库
        List<PersonAddReq> recipients = bugOfflineAddReq.getRecipients();
        personComponent.add(recipients, bugOfflineDO.getId(), PersonTypeEnum.BUG_OFFLINE_CC.getCode());

        //4.bug日志表记录一条新增数据


        //5.消息通知
        messageEventPublisher.publish(new BugOfflineAddMsg(
                this,
                bugOfflineDO.getId(),
                bugOfflineDO.getCreateMan(),
                bugOfflineDO.getOperatorId(),
                bugOfflineDO.getName()
        ));
        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> modify(BugOfflineModifyReq bugOfflineModifyReq) {
        BugOfflineDO oldBugOfflineDO = bugOfflineMapper.selectById(bugOfflineModifyReq.getId());
        if (oldBugOfflineDO == null) {
            throw new BaseBizRuntimeException("该线下bug不存在");
        }

        //1.接收表单参数,状态不变,经办人为所选用户,上一阶段经办人不变,bug数据入库
        BugOfflineDO newBugOfflineDO = BugOfflineCopier.INSTANCE.convert(bugOfflineModifyReq);
        bugOfflineMapper.update(newBugOfflineDO);

        //2.更新附件数据
        List<FileAddReq> fileIdList = bugOfflineModifyReq.getFiles();
        if (!CollectionUtils.isEmpty(fileIdList)) {
            fileComponent.update(fileIdList, bugOfflineModifyReq.getId(), FileTypeEnum.BUG_OFFLINE.getCode());
        }

        //3.更新抄送人数据
        List<PersonAddReq> recipientInfoList = bugOfflineModifyReq.getRecipients();
        if (!CollectionUtils.isEmpty(recipientInfoList)) {
            personComponent.update(recipientInfoList, bugOfflineModifyReq.getId(), PersonTypeEnum.BUG_OFFLINE_CC.getCode());
        }

        //5.修改经办人消息通知
        messageEventPublisher.publish(new BugOfflineUpdateMsg(
                this,
                newBugOfflineDO.getId(),
                newBugOfflineDO.getOperatorId(),
                newBugOfflineDO.getName(),
                BugStatusEnum.getTextByCode(newBugOfflineDO.getStatus())
        ));
        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> transfer(BugOfflineTransferReq bugOfflineTransferReq) {
        //1.验证操作人是否是经办人或其上级,状态是否是bug打开,待修复,延期修复,待确认,待验收

        //2.查找bug数据

        //3.赋值:经办人为表单选择人,上一阶段经办人不变,状态不变

        //4.更新bug数据

        //5.消息通知
        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> unHandle(Long id) {
        //1.验证操作人是否是经办人或其上级,状态是否是bug打开

        //2.查找bug数据

        //3.赋值:经办人为bug提出人,上一阶段经办人赋值为新增bug时经办人,状态变成待确认

        //4.更新bug数据

        //5.bug日志表记录状态变更

        //5.消息通知
        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> agree(Long id) {
        //1.验证操作人是否是经办人或其上级,状态是否是待确认

        //2.查找bug数据

        //3.赋值:状态变成关闭,经办人与上一阶段经办人不变,

        //4.更新bug数据

        //5.bug日志表记录状态变更
        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> reject(Long id) {
        //1.验证操作人是否是经办人或其上级,状态是否是待确认

        //2.查找bug数据

        //3.赋值:经办人为不用修复时上一阶段经办人,上一阶段经办人赋值为本次经办人(经办人与上一阶段经办人交换位置),状态变成bug打开

        //4.更新bug数据

        //5.bug日志表记录状态变更
        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> delayHandle(Long id) {
        //1.验证操作人是否是经办人或其上级,状态是否是bug打开或待修复

        //2.查找bug数据

        //3.赋值:经办人为bug提出人,上一阶段经办人赋值为新增bug时经办人,状态变成延期修复,取消关联项目

        //4.更新bug数据

        //5.bug日志表记录状态变更
        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> doHandle(Long id) {
        log.info("确认修复接收参数{}", id);
        //得到当前线下bug
        BugOfflineDO bugOfflineDO = bugOfflineMapper.selectById(id);

        //判断当前操作人是否有权限
        Boolean result = IsPermission(bugOfflineDO.getOperatorId());
        if (!result) {
            throw new BaseBizRuntimeException("您没有操作权限");
        }

        //线下bug的状态变更为"待修复"
        bugOfflineDO.setStatus(1);
        bugOfflineMapper.update(bugOfflineDO);

        BugLogDO bugLogDO = new BugLogDO();
        bugLogDO.setAction("确认修复");
        bugLogDO.setOldValue("bug打开");
        bugLogDO.setNewValue("待修复");
        bugLogDO.setMainId(id);
        bugLogDO.setType(0);
        bugLogDO.setBugName("线下bug");

        //往bug日志表中插入数据
        bugLogMapper.insert(bugLogDO);

        return BaseResult.success(true);
    }

    Boolean IsPermission(String personId) {
        //得到当前操作人账户
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        String account = userInfo.getId();

        //得到经办人所有上级
        AccountRequest accountRequest = new AccountRequest();
        accountRequest.setAccount(personId);
        Set<String> higherLevels = innerUserPersonClient.getAllSuperiorByAccount(accountRequest).getData();
        //把当前经办人添加到当前经办人上级的Set集合中
        higherLevels.add(personId);

        //判断当前操作人账户是否有权限
        if (!higherLevels.contains(account)) {
            return false;
        }

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> passSelf(Long id) {
        //1.验证操作人是否是经办人或其上级,状态是否是待修复

        //2.查找bug数据

        //3.赋值:经办人为bug提出人,上一阶段经办人赋值为确认修复时经办人,状态变成待验收

        //4.更新bug数据

        //5.bug日志表记录状态变更

        //5.消息通知
        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> accepted(Long id) {
        //1.验证操作人是否是经办人或其上级,状态是否是待验收

        //2.查找bug数据

        //3.赋值:经办人和上一阶段经办人不变(自测通过时的数据),状态变成完成

        //4.更新bug数据

        //5.bug日志表记录状态变更
        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> acceptFailed(Long id) {
        //1.验证操作人是否是经办人或其上级,状态是否是待验收

        //2.查找bug数据

        //3.赋值:经办人为自测通过时上一阶段经办人,上一阶段经办人为本次经办人(经办人与上一阶段经办人交换位置),状态变成bug打开,bug打回次数+1

        //4.更新bug数据

        //5.bug日志表记录状态变更
        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> reopen(Long id) {
        //1.验证操作人是否是提出人或其上级,状态是否是延期修复,完成或关闭

        //2.查找bug数据

        //3.赋值:经办人为bug接收人(验收通过或同意时记录的上一阶段操作人),上一阶段经办人为本次经办人(经办人与上一阶段经办人交换位置),状态变成bug打开

        //4.完成状态下点击重新打开，bug打回次数+1,完成后重新打开次数+1,更新bug数据

        //5.bug日志表记录状态变更

        //5.消息通知

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<BugOfflineDetailVO> get(Long id) {
        log.info("查看线下bug详情接收参数:{}", id);
        //校验线下bug是否存在
        BugOfflineDO bugOfflineDO = bugOfflineMapper.selectById(id);
        if (bugOfflineDO == null) {
            throw new BaseBizRuntimeException("您要查询的线下bug不存在。");
        }

        //查询bug的日志信息
        List<BugLogDO> bugLogDOList = bugLogMapper.selectByBugOfflineId(id);

        //转化线下bug
        BugOfflineDetailVO bugOfflineDetailVO = BugOfflineCopier.INSTANCE.transform(bugOfflineDO);

        //如果bug日志不为空，转化bug日志然后给线下bug赋值
        if (CollectionUtils.isNotEmpty(bugLogDOList)) {
            List<BugLogVO> bugLogVOList = bugLogDOList.stream().map(BugLogCopier.INSTANCE::convert).collect(Collectors.toList());
            //给bug日志的内容变更类型名字赋值
            bugLogVOList.forEach(bugLogVO -> {
                bugLogVO.setTypeName(BugLogTypeEnum.getTextByCode(bugLogVO.getType()));
            });
            bugOfflineDetailVO.setBugLogVOList(bugLogVOList);

        }


        //给线下bug的项目名称赋值
        ProjectDO projectDO = projectMapper.get(bugOfflineDO.getProjectId());
        if (projectDO != null) {
            bugOfflineDetailVO.setProjectName(projectDO.getName());
        }

        //给线下bug的产品线赋值,给线下bug的业务域赋值
        ProductLineDO productLineDO = productLineMapper.selectById(bugOfflineDO.getProductLineId());
        if (productLineDO != null) {
            ProductLineVO productLineVO = ProductLineCopier.INSTANCE.convert(productLineDO);
            bugOfflineDetailVO.setProductLineVO(productLineVO);
            BizDomainDO bizDomainDO = bizDomainMapper.selectById(productLineDO.getBizDomainId());
            //如果业务域不为空，赋值给线下bug的相关属性
            if (bizDomainDO != null) {
                BizDomainVO bizDomainVO = BizDomainCopier.INSTANCE.convert(bizDomainDO);
                bugOfflineDetailVO.setBizDomainVO(bizDomainVO);
            }
        }

        //给线下bug的状态赋值
        String statusName = BugStatusEnum.getTextByCode(bugOfflineDO.getStatus());
        bugOfflineDetailVO.setStatusName(statusName);

        //给线下bug的优先级赋值
        String priorityName = BugPriorityEnum.getTextByCode(bugOfflineDO.getPriority());
        bugOfflineDetailVO.setPriorityName(priorityName);

        //给线下bug的来源赋值
        String sourceName = BugSourceEnum.getTextByCode(bugOfflineDO.getSource());
        bugOfflineDetailVO.setSourceName(sourceName);

        //给线下bug的所属端赋值
        String belongName = BugBelongEnum.getTextByCode(bugOfflineDO.getBelong());
        bugOfflineDetailVO.setBelongName(belongName);

        //给线下爱bug的环境赋值
        String envName = BugEnvEnum.getTextByCode(bugOfflineDO.getEnv());
        bugOfflineDetailVO.setEnvName(envName);

        //给线下bug的浮现频率赋值
        String frequencyName = BugFrequencyEnum.getTextByCode(bugOfflineDO.getFrequency());
        bugOfflineDetailVO.setFrequencyName(frequencyName);

        //给线下bug的不用修复原因赋值
        String reason = BugNoFixReasonEnum.getTextByCode(bugOfflineDO.getUnHandleReason());
        bugOfflineDetailVO.setUnhandleReasonName(reason);

        //给线下bug的附件集合赋值
        List<FileDO> fileDOList = fileMapper.select(id, FileTypeEnum.BUG_OFFLINE.getCode());
        if (CollectionUtils.isNotEmpty(fileDOList)) {
            List<FileVO> files = fileDOList.stream().map(FileCopier.INSTANCE::change).collect(Collectors.toList());
            bugOfflineDetailVO.setFiles(files);
        }

        //给线下bug的抄送人赋值
        List<PersonDO> personDOList = personMapper.select(PersonListCondition.builder()
                .mainId(id)
                .type(50)
                .build());
        if (CollectionUtils.isNotEmpty(personDOList)) {
            List<PersonVO> personVOList = personDOList.stream().map(PersonCopier.INSTANCE::change).collect(Collectors.toList());
            bugOfflineDetailVO.setRecipientInfoList(personVOList);
        }

        //给bug相关的评论赋值
        List<CommentDO> commentDOList = commentMapper.select(id, CommentTypeEnum.BUG.getCode());
        if (CollectionUtils.isNotEmpty(commentDOList)) {
            List<CommentVO> commentVOList = commentDOList.stream().map(CommentCopier.INSTANCE::change).collect(Collectors.toList());
            bugOfflineDetailVO.setCommentVOList(commentVOList);
        }

        return BaseResult.success(bugOfflineDetailVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> delete(Long id) {
        log.info("删除线下bug接收参数:{}", id);
        //删除线下bug表中的数据
        bugOfflineMapper.deleteById(id);

        //删除bug日志表中的数据
        bugLogMapper.deleteByBugOfflineId(id);

        //删除抄送人表person中的数据
        PersonDO personDO = new PersonDO();
        personDO.setMainId(id);
        personDO.setType(PersonTypeEnum.BUG_OFFLINE_CC.getCode());
        personDO.setIsDeleted(true);
        personMapper.update(personDO);

        //删除评论数据
        commentMapper.deleteByToIdAndType(id, CommentTypeEnum.BUG.getCode());

        //删除附件数据
        FileDO fileDO = new FileDO();
        fileDO.setIsDeleted(true);
        fileDO.setAttacheId(id);
        fileDO.setType(FileTypeEnum.BUG_OFFLINE.getCode());
        fileMapper.update(fileDO);

        return BaseResult.success(true);
    }
}

























