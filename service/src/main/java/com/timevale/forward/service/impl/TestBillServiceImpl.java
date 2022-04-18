package com.timevale.forward.service.impl;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.FileMapper;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.dao.ProjectNodeMapper;
import com.timevale.forward.dal.dao.TestBillMapper;
import com.timevale.forward.dal.entity.FileDO;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.dal.entity.ProjectNodeDO;
import com.timevale.forward.dal.entity.TestBillDO;
import com.timevale.forward.facade.api.client.TestBillService;
import com.timevale.forward.facade.api.request.FileAddReq;
import com.timevale.forward.facade.api.request.TestBillAddReq;
import com.timevale.forward.facade.api.request.TestBillModifyReq;
import com.timevale.forward.facade.api.result.CreateTestBillVO;
import com.timevale.forward.facade.api.result.FileVO;
import com.timevale.forward.facade.api.result.TestBillVO;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.service.component.FileComponent;
import com.timevale.forward.service.component.ProjectLogComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.FileCopier;
import com.timevale.forward.service.copy.TestBillCopier;
import com.timevale.forward.service.observer.event.*;
import com.timevale.forward.service.observer.publisher.MessageEventPublisher;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.base.util.CollectionUtils;
import com.timevale.mandarin.common.annotation.RestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Date 2022/1/21 13:59
 * @Author 望轩
 */
@Slf4j
@RestService
public class TestBillServiceImpl implements TestBillService {

    @Resource
    private TestBillMapper testBillMapper;

    @Resource
    private FileMapper fileMapper;

    @Resource
    private ProjectNodeMapper projectNodeMapper;

    @Resource
    private ProjectMapper projectMapper;

    @Resource
    private MessageEventPublisher messageEventPublisher;

    @Resource
    private FileComponent fileComponent;

    @Resource
    private ProjectLogComponent projectLogComponent;

    @Override
    public BaseResult<CreateTestBillVO> addTestBill(Long projectId) {
        log.info("提测单-创建提测单,参数:{}", projectId);

        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        String alias = userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName();

        CreateTestBillVO createTestBillVO = new CreateTestBillVO();

        List<ProjectNodeDO> projectNodeDOList = projectNodeMapper.get(projectId);
        if (CollectionUtils.isNotEmpty(projectNodeDOList)) {
            //获取提测的计划时间
            List<Date> dateList = projectNodeDOList.stream().filter(e -> e.getName()
                    .equals(ProjectNodeEnum.SUBMIT_TEST.getProjectNodeName())).map(ProjectNodeDO::getPlanDate)
                    .collect(Collectors.toList());
            if (CollectionUtils.isEmpty(dateList)) {
                throw new BaseBizRuntimeException("提测单的计划时间不能为空");
            }
            Date planDate = dateList.get(0);


            //该项目对应的提测单
            TestBillDO testBillDO = testBillMapper.selectByProjectId(projectId);

            //提测计划时间
            createTestBillVO.setPlanDate(planDate);
            //提测人
            createTestBillVO.setSubmitTestMan(alias);
            //此项目是否有提测单
            createTestBillVO.setIsHaveSubmitTest(testBillDO != null);
        }

        return BaseResult.success(createTestBillVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> submitTestBill(TestBillAddReq testBillAddReq) {
        log.info("提测单-提交提测单,参数:{}", testBillAddReq);

        //判断该项目是否已经有提测单了，有的话则显示提示信息
        TestBillDO testBill = testBillMapper.selectByProjectId(testBillAddReq.getProjectId());
        if (testBill != null) {
            throw new BaseBizRuntimeException("该项目已经有提测单了!");
        }

        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        String alias = userInfo.getAlias();
        String id = userInfo.getId();
        testBillAddReq.setAlias(alias);
        testBillAddReq.setAccount(id);

        TestBillDO testBillDO = TestBillCopier.INSTANCE.transform(testBillAddReq);

        //提交提测单
        testBillMapper.submitTestBill(testBillDO);

        //创建一个项目的提测单之后需要清空项目原本的提测节点的实际时间
        projectNodeMapper.updateSubmitTestActualDate(testBillAddReq.getProjectId(), null);

        //获取提测单名称
        ProjectDO projectDO = projectMapper.get(testBillAddReq.getProjectId());
        String testBillName = projectDO.getName() + CommonConstant.TESTBILL_SUFFIX;

        //消息接收人
        List<String> receivers = new ArrayList<>();
        receivers.add(testBillAddReq.getTestManId());

        //通知提测单接收人
        messageEventPublisher.publish(
                new BillTestCreateMsgEvent(
                        this,
                        alias,
                        receivers,
                        testBillName,
                        testBillAddReq.getProjectId()
                )
        );

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<TestBillVO> getTestBill(Long projectId) {
        log.info("提测单-提测单详情,参数:{}", projectId);

        TestBillDO testBillDO = testBillMapper.selectByProjectId(projectId);
        if (testBillDO == null) {
            throw new BaseBizRuntimeException("该项目id没有对应的提测单");
        }

        TestBillVO testBillVO = TestBillCopier.INSTANCE.convert(testBillDO);
        testBillVO.setProgressName(TestBillProgressEnum.getTextByCode(testBillDO.getProgress()));
        testBillVO.setStatusName(TestBillStatusEnum.getTextByCode(testBillDO.getStatus()));

        //提测单主题
        ProjectDO projectDO = projectMapper.get(projectId);
        testBillVO.setSubmitTestName(projectDO.getName() + CommonConstant.TESTBILL_SUFFIX);
        testBillVO.setProjectManager(projectDO.getPmName());

        //附件集合
        List<Integer> types = Arrays.asList(FileTypeEnum.TEST_BILL_CASE.getCode(), FileTypeEnum.TEST_BILL_PASS.getCode());
        List<FileVO> fileVOList = getFileByAttachIdAndTypes(projectId, types);
        testBillVO.setFileVOList(fileVOList);

        //实际提测时间
        List<ProjectNodeDO> projectNodeDOList = projectNodeMapper.get(projectId).stream().filter(e -> e.getName()
                .equals(ProjectNodeEnum.SUBMIT_TEST.getProjectNodeName())).collect(Collectors.toList());
        Date actualDate = projectNodeDOList.get(0).getActualDate();
        testBillVO.setActualDate(actualDate);

        //计划提测时间
        Date planDate = projectNodeDOList.get(0).getPlanDate();
        testBillVO.setPlanDate(planDate);

        //是否延期以及延期天数
        if (planDate != null && actualDate != null) {
            int compare = DateUtil.compare(planDate, actualDate);
            boolean result = DateUtil.isSameDay(planDate, actualDate);
            if (compare < 0 && !result) {
                testBillVO.setIsDelay(true);
                Integer delayDay = (int) DateUtil.between(planDate, actualDate, DateUnit.DAY);
                testBillVO.setDelayDay(delayDay);
            }
        } else {
            testBillVO.setIsDelay(false);
        }

        //提测人
        testBillVO.setTestBillMan(testBillDO.getCreateMan());

        //提测人id
        testBillVO.setTestBillManId(testBillDO.getCreateManId());

        return BaseResult.success(testBillVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> submitSmokeTesting(TestBillModifyReq testBillModifyReq) {
        log.info("提测单-提测冒烟用例,参数:{}", testBillModifyReq);

        TestBillDO testBillDO = TestBillCopier.INSTANCE.change(testBillModifyReq);

        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        String alias = userInfo.getAlias();

        //获取提测人
        List<String> receivers = new ArrayList<>();
        TestBillDO testBill = testBillMapper.selectByProjectId(testBillModifyReq.getProjectId());
        if (testBill != null) {
            receivers.add(testBill.getCreateManId());
        }

        //获取提测单名称
        ProjectDO projectDO = projectMapper.get(testBillModifyReq.getProjectId());
        String testBillName = projectDO.getName() + CommonConstant.TESTBILL_SUFFIX;

        //更新提测单
        testBillMapper.submitSmokeTesting(testBillDO);

        FileDO fileDO = new FileDO();
        fileDO.setIsDeleted(true);
        fileDO.setAttacheId(testBillModifyReq.getProjectId());
        fileDO.setType(FileTypeEnum.TEST_BILL_CASE.getCode());
        //删除文件表中的原有信息
        fileMapper.update(fileDO);

        List<FileAddReq> fileAddReqList = testBillModifyReq.getList();
        if (CollectionUtils.isNotEmpty(fileAddReqList)) {
            //向文件表中插入新的数据
            fileComponent.add(fileAddReqList, testBillModifyReq.getProjectId(), FileTypeEnum.TEST_BILL_CASE.getCode());
        }

        messageEventPublisher.publish(
                new BillTestSubmitSmokeMsgEvent(
                        this,
                        alias,
                        receivers,
                        testBillName,
                        testBillModifyReq.getProjectId()
                )
        );

        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> modifyTestMan(TestBillModifyReq testBillModifyReq) {
        log.info("提测单-修改测试人,参数:{}", testBillModifyReq);

        TestBillDO testBillDO = TestBillCopier.INSTANCE.change(testBillModifyReq);

        //得到项目测试人id
        String testManId = null;

        //得到项目经理的id
        ProjectDO project = projectMapper.get(testBillModifyReq.getProjectId());
        String projectManagerId = project.getPmId();

        //接收人设置成修改后的测试人和提测人
        List<String> receivers = new ArrayList<>();
        TestBillDO testBill = testBillMapper.selectByProjectId(testBillModifyReq.getProjectId());
        if (testBill != null) {
            receivers.add(testBill.getCreateManId());
            testManId = testBill.getTestManId();
        }
        receivers.add(testBillModifyReq.getTestManId());

        //如果当前登录人既不是项目经理也不是测试人，那么当前登录人没有修改权限，直接退出
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        if (!userInfo.getId().equals(testManId) && !userInfo.getId().equals(projectManagerId)) {
            return BaseResult.fail(500, "测试人已经发生了变动，您没有修改权限");
        }

        //获取提测单名称
        ProjectDO projectDO = projectMapper.get(testBillModifyReq.getProjectId());
        String testBillName = "";
        if (projectDO != null) {
            testBillName = projectDO.getName() + CommonConstant.TESTBILL_SUFFIX;
        }

        messageEventPublisher.publish(
                new BillTestModifyTestManMsgEvent(
                        this,
                        testBillName,
                        testBillModifyReq.getTestMan(),
                        receivers,
                        testBillModifyReq.getProjectId()
                )
        );

        testBillMapper.modifyTestMan(testBillDO);

        return BaseResult.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> selfTestPass(TestBillModifyReq testBillModifyReq) {
        log.info("提测单-自测通过,参数:{}", testBillModifyReq);

        TestBillDO testBillDO = TestBillCopier.INSTANCE.change(testBillModifyReq);

        //获取发起人
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        String alias = userInfo.getAlias();

        //获取提测单名称
        ProjectDO projectDO = projectMapper.get(testBillModifyReq.getProjectId());
        String testBillName = "";
        if (projectDO != null) {
            testBillName = projectDO.getName() + CommonConstant.TESTBILL_SUFFIX;
        }

        //消息接收人设置成提测单测试人
        List<String> receivers = new ArrayList<>();
        TestBillDO testBill = testBillMapper.selectByProjectId(testBillModifyReq.getProjectId());
        if (testBill != null) {
            receivers.add(testBill.getTestManId());
        }

        testBillDO.setReason(null);
        //更新提测表信息
        testBillMapper.selfTestPass(testBillDO);

        FileDO fileDO = new FileDO();
        fileDO.setIsDeleted(true);
        fileDO.setAttacheId(testBillModifyReq.getProjectId());
        fileDO.setType(FileTypeEnum.TEST_BILL_PASS.getCode());
        //删除文件表中的原有信息
        fileMapper.update(fileDO);

        List<FileAddReq> fileAddReqList = testBillModifyReq.getList();
        if (CollectionUtils.isNotEmpty(fileAddReqList)) {
            //往文件表中插入信息
            fileComponent.add(fileAddReqList, testBillModifyReq.getProjectId(), FileTypeEnum.TEST_BILL_PASS.getCode());
        }

        messageEventPublisher.publish(
                new BillTestSelfTestPassMsgEvent(
                        this,
                        alias,
                        testBillName,
                        receivers,
                        testBillModifyReq.getProjectId()
                )
        );

        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> submitTestPass(TestBillModifyReq testBillModifyReq) {
        log.info("提测单-提测通过,参数:{}", testBillModifyReq);

        TestBillDO testBillDO = TestBillCopier.INSTANCE.change(testBillModifyReq);

        //获取提测单名称
        ProjectDO projectDO = projectMapper.get(testBillModifyReq.getProjectId());
        String testBillName = "";
        if (projectDO == null) {
            throw new BaseBizRuntimeException("该提测单" + testBillDO.getId() + ",无对应项目");
        }
        testBillName = projectDO.getName() + CommonConstant.TESTBILL_SUFFIX;

        //接收人设置成提测人
        List<String> receivers = new ArrayList<>();
        TestBillDO testBill = testBillMapper.selectByProjectId(testBillModifyReq.getProjectId());
        if (testBill != null) {
            receivers.add(testBill.getCreateManId());
        }

        testBillDO.setReason(null);
        //更新提测表信息
        testBillMapper.submitTestPass(testBillDO);

        //更新项目节点表
        projectNodeMapper.updateSubmitTestActualDate(testBillModifyReq.getProjectId(), testBillModifyReq.getActualDate());
        Integer oldStatus = projectDO.getStatus();
        if (!ProjectStatusEnum.INVALID.getCode().equals(oldStatus) && !ProjectStatusEnum.RELEASED.getCode().equals(oldStatus)) {
            // 项目进入测试中
            projectDO.setStatus(ProjectStatusEnum.TESTING.getCode());
            projectMapper.update(projectDO);
        }
        projectLogComponent.addLogWhenStatusChange(oldStatus, projectDO.getStatus(), projectDO.getId(), ButtonActionEnum.TEST_PASS.getText());

        messageEventPublisher.publish(
                new BillTestSubmitTestSuccessMsgEvent(
                        this,
                        testBillName,
                        receivers,
                        testBillModifyReq.getProjectId()
                )
        );

        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> submitTestBack(TestBillModifyReq testBillModifyReq) {
        log.info("提测单-提测打回,参数:{}", testBillModifyReq);

        TestBillDO testBillDO = TestBillCopier.INSTANCE.change(testBillModifyReq);

        //获取提测单名称
        ProjectDO projectDO = projectMapper.get(testBillModifyReq.getProjectId());
        String testBillName = "";
        if (projectDO != null) {
            testBillName = projectDO.getName() + CommonConstant.TESTBILL_SUFFIX;
        }

        //接收人设置成提测人
        List<String> receivers = new ArrayList<>();
        TestBillDO testBill = testBillMapper.selectByProjectId(testBillModifyReq.getProjectId());
        if (testBill != null) {
            receivers.add(testBill.getCreateManId());
        }

        //更新提测表信息
        testBillMapper.submitTestBack(testBillDO);

        messageEventPublisher.publish(
                new BillTestSubmitTestFailMsgEvent(
                        this,
                        testBillName,
                        receivers,
                        testBillModifyReq.getProjectId()
                )
        );

        return BaseResult.success(true);
    }

    public List<FileVO> getFileByAttachIdAndTypes(Long attachId, List<Integer> types) {
        List<FileVO> fileVOList = new ArrayList<>();
        for (Integer type : types) {
            List<FileDO> fileDOList = fileMapper.select(attachId, type);
            if (CollectionUtils.isNotEmpty(fileDOList)) {
                List<FileVO> fileVOS = fileDOList.stream().map(FileCopier.INSTANCE::change).collect(Collectors.toList());
                fileVOList.addAll(fileVOS);
            }
        }
        return fileVOList;
    }
}


























