package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.FileMapper;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.dao.ProjectNodeMapper;
import com.timevale.forward.dal.dao.TestBillMapper;
import com.timevale.forward.dal.entity.FileDO;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.dal.entity.ProjectNodeDO;
import com.timevale.forward.dal.entity.SubmitTestDO;
import com.timevale.forward.facade.api.client.SubmitTestService;
import com.timevale.forward.facade.api.request.FileAddReq;
import com.timevale.forward.facade.api.result.FileVO;
import com.timevale.forward.facade.api.result.SubmitTestVO;
import com.timevale.forward.model.enums.FileTypeEnum;
import com.timevale.forward.model.enums.ProjectNodeEnum;
import com.timevale.forward.service.copy.FileCopier;
import com.timevale.forward.service.copy.SubmitTestCopier;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.base.util.CollectionUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Date 2022/1/21 13:59
 * @Author 望轩
 */
public class SubmitTestServiceImpl implements SubmitTestService {

    @Resource
    private TestBillMapper testBillMapper;

    @Resource
    private FileMapper fileMapper;

    @Resource
    private ProjectNodeMapper projectNodeMapper;

    @Resource
    private ProjectMapper projectMapper;


    @Override
    public BaseResult<Map<String, Object>> addTestBill(Long projectId) {
        UserInfo userInfo = new UserInfo();
        String alias = userInfo.getAlias();

        //存放返回结果
        Map<String, Object> map = new HashMap<>(16);

        List<ProjectNodeDO> projectNodeDOList = projectNodeMapper.get(projectId);
        if (CollectionUtils.isNotEmpty(projectNodeDOList)) {
            //获取提测的计划时间
            Date planDate = projectNodeDOList.stream().filter(e -> e.getName()
                    .equals(ProjectNodeEnum.SUBMIT_TEST.getProjectNodeName())).map(ProjectNodeDO::getPlanDate)
                    .collect(Collectors.toList()).get(0);
            //查看该项目是否已经有提测单
            SubmitTestDO submitTestDO = testBillMapper.selectByProjectId(projectId);
            //设置提测计划时间
            map.put("planDate", planDate);
            //设置此项目是否有提测单
            map.put("isHaveSubmitTest", submitTestDO);
            //设置提测人
            map.put("submitTestMan", alias);
        }

        return BaseResult.success(map);
    }

    @Override
    public BaseResult<Boolean> submitTestBill(Long projectId, String testMan, String testManId) {
        UserInfo userInfo = new UserInfo();
        String alias = userInfo.getAlias();
        String id = userInfo.getId();

        //提交提测单
        testBillMapper.submitTestBill(projectId, testMan, testManId, alias, id);
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<SubmitTestVO> getTestBill(Long projectId, Integer type) {
        SubmitTestDO submitTestDO = testBillMapper.selectByProjectId(projectId);
        SubmitTestVO submitTestVO = SubmitTestCopier.INSTANCE.convert(submitTestDO);

        //提测单主题
        ProjectDO projectDO = projectMapper.get(projectId);
        if (submitTestVO != null) {
            submitTestVO.setSubmitTestName(projectDO.getName() + "提测单");
            submitTestVO.setProjectManager(projectDO.getPmName());
        }

        //附件集合
        List<FileDO> fileDOList = fileMapper.select(projectId, type);
        List<FileVO> fileVOList = FileCopier.INSTANCE.transform(fileDOList);
        submitTestVO.setFileVOList(fileVOList);

        //实际提测时间
        List<ProjectNodeDO> projectNodeDOList = projectNodeMapper.get(projectId).stream().filter(e -> e.getName()
                .equals(ProjectNodeEnum.SUBMIT_TEST.getProjectNodeName())).collect(Collectors.toList());
        Date actualDate = projectNodeDOList.get(0).getActualDate();
        submitTestVO.setActualDate(actualDate);
        return BaseResult.success(submitTestVO);
    }

    @Override
    public BaseResult<Boolean> submitSmokeTesting(Long projectId, String caseUrl, List<FileAddReq> list) {
        UserInfo userInfo = new UserInfo();
        String alias = userInfo.getAlias();
        String id = userInfo.getId();

        //更新提测单
        testBillMapper.submitSmokeTesting(projectId, caseUrl, alias, id);

        FileDO fileDO = new FileDO();
        fileDO.setIsDeleted(true);
        fileDO.setModifyMan(alias);
        fileDO.setModifyManId(id);
        fileDO.setAttacheId(projectId);
        fileDO.setType(FileTypeEnum.TEST_BILL_CASE.getCode());
        //删除文件表中的原有信息
        fileMapper.update(fileDO);

        List<FileDO> fileDOList = new ArrayList<>();
        //将FileAddReq转化成FileDO
        for (FileAddReq fileAddReq : list) {
            FileDO file = new FileDO();
            file.setType(FileTypeEnum.TEST_BILL_CASE.getCode());
            file.setAttacheId(projectId);
            file.setFileName(fileAddReq.getFileName());
            file.setFileId(fileAddReq.getFileId());
            fileDOList.add(file);
        }

        //向文件表中插入新的数据
        fileMapper.inserts(fileDOList);

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> modifyTestMan(Long projectId, String testMan, String testManId) {
        UserInfo userInfo = new UserInfo();
        String alias = userInfo.getAlias();
        String id = userInfo.getId();

        return BaseResult.success(testBillMapper.modifyTestMan(projectId, testMan, testManId, alias, id));
    }

    @Override
    public BaseResult<Boolean> selfTestPass(String desc, Integer progress, List<FileAddReq> list, Long attacheId) {
        UserInfo userInfo = new UserInfo();
        String alias = userInfo.getAlias();
        String id = userInfo.getId();

        //更新提测表信息
        testBillMapper.selfTestPass(attacheId, desc, progress, alias, id);

        List<FileDO> fileDOList = new ArrayList<>();
        for (FileAddReq fileAddReq : list) {
            FileDO fileDO = new FileDO();
            fileDO.setAttacheId(attacheId);
            fileDO.setType(FileTypeEnum.TEST_BILL_PASS.getCode());
            fileDO.setFileName(fileAddReq.getFileName());
            fileDO.setFileId(fileAddReq.getFileId());
            fileDOList.add(fileDO);
        }

        //往文件表中插入信息
        fileMapper.inserts(fileDOList);

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> submitTestPass(Long projectId, Integer passRate, LocalDateTime actualDate) {
        UserInfo userInfo = new UserInfo();
        String alias = userInfo.getAlias();
        String id = userInfo.getId();

        //更新提测表信息
        testBillMapper.submitTestPass(projectId, passRate, alias, id);

        //更新项目节点表
        projectNodeMapper.updateSubmitTestActualDate(projectId, actualDate);

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> submitTestBack(Long projectId, String reason) {
        UserInfo userInfo = new UserInfo();
        String alias = userInfo.getAlias();
        String id = userInfo.getId();

        //更新提测表信息
        testBillMapper.submitTestBack(projectId, reason, alias, id);

        return BaseResult.success(true);
    }
}


























