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
import com.timevale.forward.model.enums.FileTypeEnum;
import com.timevale.forward.model.enums.ProjectNodeEnum;
import com.timevale.forward.service.component.FileComponent;
import com.timevale.forward.service.copy.FileCopier;
import com.timevale.forward.service.copy.TestBillCopier;
import com.timevale.forward.service.observer.event.BillTestMsgEvent;
import com.timevale.forward.service.observer.publisher.MessageEventPublisher;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.base.util.CollectionUtils;
import com.timevale.mandarin.common.annotation.RestService;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
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

    @Override
    public BaseResult<CreateTestBillVO> addTestBill(Long projectId) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        String alias = userInfo.getAlias();

        CreateTestBillVO createTestBillVO = new CreateTestBillVO();

        List<ProjectNodeDO> projectNodeDOList = projectNodeMapper.get(projectId);
        if (CollectionUtils.isNotEmpty(projectNodeDOList)) {
            //获取提测的计划时间
            Date planDate = projectNodeDOList.stream().filter(e -> e.getName()
                    .equals(ProjectNodeEnum.SUBMIT_TEST.getProjectNodeName())).map(ProjectNodeDO::getPlanDate)
                    .collect(Collectors.toList()).get(0);

            //该项目对应的提测单
            TestBillDO testBillDO = testBillMapper.selectByProjectId(projectId);

            //提测计划时间
            createTestBillVO.setPlanDate(planDate);
            //提测人
            createTestBillVO.setSubmitTestMan(alias);
            //此项目是否有提测单
            if (testBillDO != null) {
                createTestBillVO.setIsHaveSubmitTest(true);
            } else {
                createTestBillVO.setIsHaveSubmitTest(false);
            }
        }

        return BaseResult.success(createTestBillVO);
    }

    @Override
    public BaseResult<Boolean> submitTestBill(TestBillAddReq testBillAddReq) {
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
//        testBillDO.setCreateMan(alias);
//        testBillDO.setCreateManId(id);

        //提交提测单
        testBillMapper.submitTestBill(testBillDO);

        //通知提测单接收人
        messageEventPublisher.publish(
                new BillTestMsgEvent(
                        this,
                        "望轩",
                        "wangxuan"
                )
        );

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<TestBillVO> getTestBill(Long projectId, Integer type) {
        TestBillDO testBillDO = testBillMapper.selectByProjectId(projectId);
        TestBillVO testBillVO = TestBillCopier.INSTANCE.convert(testBillDO);

        //提测单主题
        ProjectDO projectDO = projectMapper.get(projectId);
        if (testBillVO != null) {
            testBillVO.setSubmitTestName(projectDO.getName() + "提测单");
            testBillVO.setProjectManager(projectDO.getPmName());
            //附件集合
            List<FileDO> fileDOList = fileMapper.select(projectId, type);
            List<FileVO> fileVOList = FileCopier.INSTANCE.transform(fileDOList);
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
                if (compare < 0) {
                    testBillVO.setIsDelay(true);
                    Integer delayDay = (int) DateUtil.between(planDate, actualDate, DateUnit.DAY);
                    testBillVO.setDelayDay(delayDay);
                    testBillVO.setDelayDay(delayDay);
                }
            } else {
                testBillVO.setIsDelay(false);
            }

            //提测人
            testBillVO.setTestBillMan(testBillDO.getCreateMan());

        }

        return BaseResult.success(testBillVO);
    }

    @Override
    public BaseResult<Boolean> submitSmokeTesting(TestBillModifyReq testBillModifyReq) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        String alias = userInfo.getAlias();
        String id = userInfo.getId();

        TestBillDO testBillDO = TestBillCopier.INSTANCE.change(testBillModifyReq);
        testBillDO.setModifyMan(alias);
        testBillDO.setModifyManId(id);

        //更新提测单
        testBillMapper.submitSmokeTesting(testBillDO);

        FileDO fileDO = new FileDO();
        fileDO.setIsDeleted(true);
        fileDO.setModifyMan(alias);
        fileDO.setModifyManId(id);
        fileDO.setAttacheId(testBillModifyReq.getProjectId());
        fileDO.setType(FileTypeEnum.TEST_BILL_CASE.getCode());
        //删除文件表中的原有信息
        fileMapper.update(fileDO);

        List<FileAddReq> fileAddReqList = testBillModifyReq.getList();
        if (CollectionUtils.isNotEmpty(fileAddReqList)) {
            //向文件表中插入新的数据
            fileComponent.add(fileAddReqList, testBillModifyReq.getProjectId(), FileTypeEnum.TEST_BILL_CASE.getCode());
        }

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> modifyTestMan(TestBillModifyReq testBillModifyReq) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        String alias = userInfo.getAlias();
        String id = userInfo.getId();

        TestBillDO testBillDO = TestBillCopier.INSTANCE.change(testBillModifyReq);
        testBillDO.setModifyMan(alias);
        testBillDO.setModifyManId(id);

        return BaseResult.success(testBillMapper.modifyTestMan(testBillDO));
    }

    @Override
    public BaseResult<Boolean> selfTestPass(TestBillModifyReq testBillModifyReq) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        String alias = userInfo.getAlias();
        String id = userInfo.getId();

        TestBillDO testBillDO = TestBillCopier.INSTANCE.change(testBillModifyReq);
        testBillDO.setModifyMan(alias);
        testBillDO.setModifyManId(id);

        //更新提测表信息
        testBillMapper.selfTestPass(testBillDO);

        List<FileAddReq> fileAddReqList = testBillModifyReq.getList();
        if (CollectionUtils.isNotEmpty(fileAddReqList)) {
            //往文件表中插入信息
            fileComponent.add(fileAddReqList, testBillModifyReq.getProjectId(), FileTypeEnum.TEST_BILL_PASS.getCode());
        }

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> submitTestPass(TestBillModifyReq testBillModifyReq) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        String alias = userInfo.getAlias();
        String id = userInfo.getId();

        TestBillDO testBillDO = TestBillCopier.INSTANCE.change(testBillModifyReq);
        testBillDO.setModifyMan(alias);
        testBillDO.setModifyManId(id);

        //更新提测表信息
        testBillMapper.submitTestPass(testBillDO);

        //更新项目节点表
        projectNodeMapper.updateSubmitTestActualDate(testBillModifyReq.getProjectId(), testBillModifyReq.getActualDate());

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> submitTestBack(TestBillModifyReq testBillModifyReq) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        String alias = userInfo.getAlias();
        String id = userInfo.getId();

        TestBillDO testBillDO = TestBillCopier.INSTANCE.change(testBillModifyReq);
        testBillDO.setModifyMan(alias);
        testBillDO.setModifyManId(id);

        //更新提测表信息
        testBillMapper.submitTestBack(testBillDO);

        return BaseResult.success(true);
    }
}


























