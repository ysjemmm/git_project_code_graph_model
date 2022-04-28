package com.timevale.forward.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.ProjectRiskMapper;
import com.timevale.forward.dal.dao.TaskMapper;
import com.timevale.forward.dal.dto.HomePageRiskWarningDTO;
import com.timevale.forward.dal.dto.HomePageRiskWarningSubmitTestDTO;
import com.timevale.forward.dal.dto.HomePageRiskWarningTaskDTO;
import com.timevale.forward.dal.entity.ProjectRiskDO;
import com.timevale.forward.facade.api.client.ProjectRiskService;
import com.timevale.forward.facade.api.query.ProjectRiskQueryList;
import com.timevale.forward.facade.api.request.ProjectRiskAddReq;
import com.timevale.forward.facade.api.request.ProjectRiskModifyReq;
import com.timevale.forward.facade.api.result.ProjectRiskVO;
import com.timevale.forward.model.enums.ProjectRiskSignEnum;
import com.timevale.forward.model.enums.ProjectRiskStatusEnum;
import com.timevale.forward.model.enums.ProjectRiskTypeEnum;
import com.timevale.forward.service.component.HomePageRiskWarningComponent;
import com.timevale.forward.service.component.HomePageRiskWarningSubmitTestComponent;
import com.timevale.forward.service.component.HomePageRiskWarningTaskComponent;
import com.timevale.forward.service.component.ProjectRiskExplanationComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.ProjectRiskCopier;
import com.timevale.forward.service.integration.http.ElapsedTimeClient;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.aop.LogPoint;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@LogPoint
@RestService
public class ProjectRiskServiceImpl implements ProjectRiskService {

    @Resource
    ProjectRiskMapper projectRiskMapper;

    @Resource
    ElapsedTimeClient elapsedTimeClient;

    @Resource
    TaskMapper taskMapper;

    @Resource
    ProjectRiskExplanationComponent projectRiskExplanationComponent;

    @Resource
    HomePageRiskWarningComponent riskWarningComponent;

    @Resource
    HomePageRiskWarningSubmitTestComponent riskWarningSubmitTestComponent;

    @Resource
    HomePageRiskWarningTaskComponent riskWarningTaskComponent;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> add(ProjectRiskAddReq projectRiskAddReq) {
        ProjectRiskDO riskDO = ProjectRiskCopier.INSTANCE.convert(projectRiskAddReq);
        // 类型为其它
        riskDO.setType(ProjectRiskTypeEnum.OTHER.getCode());
        riskDO.setSign("");
        riskDO.setMainId(0L);
        projectRiskMapper.insert(riskDO);

        // 添加项目说明
        projectRiskExplanationComponent.add(riskDO.getId(), projectRiskAddReq.getExplanation());

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> modify(ProjectRiskModifyReq projectRiskModifyReq) {
        ProjectRiskDO riskDO = ProjectRiskCopier.INSTANCE.convert(projectRiskModifyReq);
        projectRiskMapper.update(riskDO);

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<ProjectRiskVO> get(Long projectRiskId) {
        ProjectRiskDO riskDO = projectRiskMapper.selectById(projectRiskId);
        if(riskDO == null){
            throw new BaseBizRuntimeException("该项目风险不存在");
        }

        ProjectRiskVO riskVO = ProjectRiskCopier.INSTANCE.convert(riskDO);

        // 枚举填充
        riskVO.setTypeName(ProjectRiskTypeEnum.getTextByCode(riskVO.getType()));
        riskVO.setStatusName(ProjectRiskStatusEnum.getTextByCode(riskVO.getStatus()));

        return BaseResult.success(riskVO);
    }

    @Override
    public BaseResult<PageQueryResult<ProjectRiskVO>> list(ProjectRiskQueryList projectRiskQueryList) {
        // 开始分页
        PageHelper.startPage(projectRiskQueryList.pageNum, projectRiskQueryList.pageSize, CommonConstant.CREATE_ORDER_BY);

        List<ProjectRiskDO> riskDOList = projectRiskMapper.selectByProjectId(projectRiskQueryList.getProjectId());
        List<ProjectRiskVO> riskVOList = riskDOList.stream().map(ProjectRiskCopier.INSTANCE::convert).collect(Collectors.toList());

        // 枚举描述
        for (ProjectRiskVO e : riskVOList) {
            e.setTypeName(ProjectRiskTypeEnum.getTextByCode(e.getType()));
            e.setStatusName(ProjectRiskStatusEnum.getTextByCode(e.getStatus()));
        }

        // 返回分页数据
        PageInfo<ProjectRiskDO> pageInfo = new PageInfo<>(riskDOList);
        PageQueryResult<ProjectRiskVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(riskVOList);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);

        return BaseResult.success(pageQueryResult);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> sync() {
        // 数据分发端
        List<HomePageRiskWarningDTO> nodeDTOList = riskWarningComponent.getRiskWarningAll();
        List<HomePageRiskWarningTaskDTO> taskDTOList = riskWarningTaskComponent.getRiskWarningTaskAll();
        List<HomePageRiskWarningSubmitTestDTO> testDTOList = riskWarningSubmitTestComponent.getRiskWarningSubmitTestAll();

        // sql 端
        List<ProjectRiskDO> riskDOList = projectRiskMapper.selectByStatus(ProjectRiskStatusEnum.PENDING.getCode());

        // 唯一id
        Map<String, Object> newRiskMap = new HashMap<>();
        newRiskMap.putAll(taskDTOList.stream().collect(Collectors.toMap(e -> e.getRiskType() + "-" + e.getProjectId() + "-" + e.getMainId(), Function.identity())));
        newRiskMap.putAll(testDTOList.stream().collect(Collectors.toMap(e -> e.getRiskType() + "-" + e.getProjectId() + "-" + e.getMainId(), Function.identity())));
        newRiskMap.putAll(nodeDTOList.stream().collect(Collectors.toMap(e -> e.getRiskType() + "-" + e.getProjectId() + "-" + e.getNodeName(), Function.identity())));

        Map<String, ProjectRiskDO> oldRiskMap = riskDOList.stream().collect(Collectors.toMap(e -> e.getType() + "-" + e.getProjectId() + "-" + e.getMainId(), Function.identity()));

        // 判断去重，更新状态
        List<ProjectRiskDO> completeList = new ArrayList<>();
        oldRiskMap.forEach((k, v) -> {
            // 如果还能查询到，则风险状态不变
            if(newRiskMap.containsKey(k)){
               return;
            }

            // 如果查询不到，则说明风险已经处理
            ProjectRiskDO riskDO = new ProjectRiskDO();
            riskDO.setId(v.getId());
            riskDO.setStatus(ProjectRiskStatusEnum.COMPLETE.getCode());
            riskDO.setSign(updateRiskSign(v));
        });

        // 更新完成处理的风险
        if(CollectionUtils.isNotEmpty(completeList)){
            projectRiskMapper.batchUpdate(completeList);
        }

        // 增加新风险，同时添加风险说明
        List<ProjectRiskDO> addRiskDOList = new ArrayList<>();
        newRiskMap.forEach((k, v) -> {
            if(!oldRiskMap.containsKey(k)){
                addRiskDOList.add(syncAdd(v));
            }
        });
        projectRiskMapper.batchInsert(addRiskDOList);

        return BaseResult.success(true);
    }


    private ProjectRiskDO syncAdd(Object object){
        ProjectRiskDO riskDO = new ProjectRiskDO();
        riskDO.setStatus(ProjectRiskStatusEnum.PENDING.getCode());

        if(object instanceof HomePageRiskWarningDTO){
            HomePageRiskWarningDTO nodeRisk = (HomePageRiskWarningDTO) object;
            riskDO.setProjectId(nodeRisk.getProjectId());
            riskDO.setMainId(nodeRisk.getMainId());
            riskDO.setType(nodeRisk.getRiskType());
            riskDO.setName(nodeRisk.getNodeName());
            riskDO.setSign(String.format(ProjectRiskSignEnum.SUBMIT_FAILURE.getText(), nodeRisk.getOverdueDay()));

        }else if(object instanceof HomePageRiskWarningTaskDTO){
            HomePageRiskWarningTaskDTO taskRisk = (HomePageRiskWarningTaskDTO) object;
            riskDO.setProjectId(taskRisk.getProjectId());
            riskDO.setMainId(taskRisk.getMainId());
            riskDO.setType(taskRisk.getRiskType());
            riskDO.setName(taskRisk.getTaskName());

            double totalHour = Double.parseDouble(taskRisk.getOverdueTime());
            int day = (int)totalHour / 24;
            double hour = totalHour - 24 * day;
            riskDO.setSign(String.format(ProjectRiskSignEnum.SUBMIT_FAILURE.getText(), day, hour));

        }else if(object instanceof HomePageRiskWarningSubmitTestDTO){
            HomePageRiskWarningSubmitTestDTO testRisk = (HomePageRiskWarningSubmitTestDTO) object;
            riskDO.setProjectId(testRisk.getProjectId());
            riskDO.setMainId(testRisk.getMainId());
            riskDO.setType(testRisk.getRiskType());
            riskDO.setName(testRisk.getTestBillName());
            riskDO.setSign(ProjectRiskSignEnum.SUBMIT_FAILURE.getText());
        }

        return riskDO;
    }

    public String updateRiskSign(ProjectRiskDO riskDO){
        String riskSign = "";

        // 任务、节点 计算逾期时间
        if(ProjectRiskTypeEnum.TASK_OVERDUE.getCode().equals(riskDO.getType())){


        }else if(ProjectRiskTypeEnum.PROJECT_NODE_OVERDUE.getCode().equals(riskDO.getType())
                || ProjectRiskTypeEnum.PROJECT_NODE_ENTRY_OVERDUE.getCode().equals(riskDO.getType())){

        }

        return riskSign;
    }

}
