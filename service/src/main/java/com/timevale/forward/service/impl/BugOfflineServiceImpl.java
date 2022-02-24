package com.timevale.forward.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.BugOfflineListCondition;
import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.entity.BugLogDO;
import com.timevale.forward.dal.entity.BugOfflineDO;
import com.timevale.forward.dal.entity.ProductLineDO;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.facade.api.client.BugOfflineService;
import com.timevale.forward.facade.api.query.BugOfflineQueryList;
import com.timevale.forward.facade.api.request.*;
import com.timevale.forward.facade.api.result.BugLogVO;
import com.timevale.forward.facade.api.result.BugOfflineDetailVO;
import com.timevale.forward.facade.api.result.BugOfflineVO;
import com.timevale.forward.facade.api.result.ProductLineVO;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.service.component.FileComponent;
import com.timevale.forward.service.component.PersonComponent;
import com.timevale.forward.service.component.TaskComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.BugLogCopier;
import com.timevale.forward.service.copy.BugOfflineCopier;
import com.timevale.forward.service.copy.ProductLineCopier;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.forward.service.observer.event.BugOfflineAddMsg;
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
    MessageEventPublisher messageEventPublisher;

    @Resource
    private BugLogMapper bugLogMapper;


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
        }else if(AscriptionEnum.RECEIVE.toString().equals(ascription)){
            condition.setOperatorIds(Lists.newArrayList(userInfo.getId()));
        }else if(AscriptionEnum.COPIER.toString().equals(ascription)){
            condition.setCopier(userInfo.getId());
        }else{
            List<String> teamMemberIdList = innerUserPersonClient.getAllMyStaffWithSelf(userInfo.getId(), true);
            if(AscriptionEnum.TEAM_SUBMIT.toString().equals(ascription)){
                Set<String> createIdSet = Sets.newHashSet(condition.getProposerIds());
                if(!createIdSet.isEmpty()){
                    teamMemberIdList = teamMemberIdList.stream().filter(createIdSet::contains).collect(Collectors.toList());
                    resultIsEmpty = teamMemberIdList.isEmpty();
                }
            }else if(AscriptionEnum.TEAM_RECEIVE.toString().equals(ascription)){
                Set<String> createIdSet = Sets.newHashSet(condition.getOperatorIds());
                if(!createIdSet.isEmpty()){
                    teamMemberIdList = teamMemberIdList.stream().filter(createIdSet::contains).collect(Collectors.toList());
                    resultIsEmpty = teamMemberIdList.isEmpty();
                }
            }
        }
        if(resultIsEmpty){
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

        return BaseResult.success(ResultUtil.pageEmpty());
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> add(BugOfflineAddReq bugOfflineAddReq) {
        //1.接收表单参数,状态为:bug打开,经办人所选用户,上一阶段经办人为bug提出人,bug数据入库
        BugOfflineDO bugOfflineDO = BugOfflineCopier.INSTANCE.convert(bugOfflineAddReq);
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
        log.info("线下bug修改接收参数:{}", bugOfflineModifyReq);
        //1.接收表单参数,状态不变,经办人为所选用户,上一阶段经办人不变,bug数据入库

        //2.更新附件数据

        //3.更新抄送人数据

        //5.修改经办人消息通知
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
        //1.验证操作人是否是经办人或其上级,状态是否是bug打开

        //2.查找bug数据

        //3.赋值:经办人不变,上一阶段经办人不变(bug提出人),状态变成待修复

        //4.更新bug数据

        //5.bug日志表记录状态变更
        return BaseResult.success(true);
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
        if(bugOfflineDO == null){
            throw new BaseBizRuntimeException("您要查询的线下bug不存在。");
        }

        //查询bug的日志信息
        List<BugLogDO> bugLogDOList = bugLogMapper.selectByBugOfflineId(id);

        //转化线下bug
        BugOfflineDetailVO bugOfflineDetailVO = BugOfflineCopier.INSTANCE.transform(bugOfflineDO);

        //如果bug日志不为空，转化bug日志然后给线下bug赋值
        if(CollectionUtils.isNotEmpty(bugLogDOList)){
            List<BugLogVO> bugLogVOList = bugLogDOList.stream().map(BugLogCopier.INSTANCE::convert).collect(Collectors.toList());
            bugOfflineDetailVO.setBugLogVOList(bugLogVOList);
        }

        //给线下bug的项目名称赋值
        ProjectDO projectDO = projectMapper.get(bugOfflineDO.getProjectId());
        if(projectDO != null){
            bugOfflineDetailVO.setProjectName(projectDO.getName());
        }

        //给线下bug的产品线赋值
        ProductLineDO productLineDO = productLineMapper.selectById(bugOfflineDO.getProductLineId());
        if(productLineDO != null){
            ProductLineVO productLineVO = ProductLineCopier.INSTANCE.convert(productLineDO);
            bugOfflineDetailVO.setProductLineVO(productLineVO);
        }

        //给线下bug的状态赋值
        String statusName = BugStatusEnum.getTextByCode(bugOfflineDO.getStatus());
        bugOfflineDetailVO.setStatusName(statusName);

        //给线下bug的优先级赋值



        return BaseResult.success(bugOfflineDetailVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> delete(Long id) {
        return BaseResult.success(true);
    }
}
