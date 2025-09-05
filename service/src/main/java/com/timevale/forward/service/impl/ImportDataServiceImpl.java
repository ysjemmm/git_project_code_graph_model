package com.timevale.forward.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.ProductDemandMapper;
import com.timevale.forward.dal.dao.ProductLineMapper;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.entity.ProductDemandDO;
import com.timevale.forward.dal.entity.ProductLineDO;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.facade.api.client.ImportDataService;
import com.timevale.forward.facade.api.client.ProductDemandService;
import com.timevale.forward.facade.api.client.ProjectEvaluateService;
import com.timevale.forward.facade.api.client.ProjectService;
import com.timevale.forward.facade.api.client.TaskService;
import com.timevale.forward.facade.api.query.ProjectProductDemandQueryList;
import com.timevale.forward.facade.api.request.ElapsedTimeQueryReq;
import com.timevale.forward.facade.api.request.EvaluateReq;
import com.timevale.forward.facade.api.request.PersonAddReq;
import com.timevale.forward.facade.api.request.ProductDemandAddReq;
import com.timevale.forward.facade.api.request.ProjectAddReq;
import com.timevale.forward.facade.api.request.ProjectConclusionReq;
import com.timevale.forward.facade.api.request.ProjectEvaluateReq;
import com.timevale.forward.facade.api.request.ProjectModifyReq;
import com.timevale.forward.facade.api.request.ProjectNodeAddReq;
import com.timevale.forward.facade.api.request.ProjectProductDemandLinkReq;
import com.timevale.forward.facade.api.request.TaskAddReq;
import com.timevale.forward.facade.api.result.ProductDemandVO;
import com.timevale.forward.facade.api.result.ProjectDetailVO;
import com.timevale.forward.facade.api.result.ProjectNodeVO;
import com.timevale.forward.model.enums.LinkOrUnLinkEnum;
import com.timevale.forward.model.enums.ProjectStatusEnum;
import com.timevale.forward.service.utils.file.FileUtil;
import com.timevale.forward.service.utils.file.ImportDataUtil;
import com.timevale.forward.service.utils.file.PinyinConverter;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.TriConsumer;
import org.assertj.core.util.Lists;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @auther: yuhua
 * @date: 2025/7/31 15:27
 * @description: 导入数据
 */
@Slf4j
@RestService
public class ImportDataServiceImpl implements ImportDataService {

    @Resource
    private ProjectService projectService;

    @Resource
    private TaskService taskService;

    @Resource
    private ProjectMapper projectMapper;

    @Resource
    private ProductDemandService productDemandService;

    @Resource
    private ProductDemandMapper productDemandMapper;

    @Resource
    private ProductLineMapper productLineMapper;

    @Resource
    private ProjectEvaluateService evaluateService;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    private static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    private static final String[] importProjectDataHeader = {"项目名称", "产品线", "项目类型（PBG:1;1-N:2;职能后台:3）", "项目性质（产品研发:0;技术优化:1;日常迭代:2）",
            "项目等级（S:20;A:30;B:40）", "计划开始时间", "计划结束时间", "优先级（P0:0;P1:10;P2:20;P3:30）", "项目id", "项目经理", "产品经理", "项目负责人", "项目描述", "项目成员",
            "是否需要在发布平台发布", "是否需要项目验收", "是否有项目目标"};

    private static final String[] importDemandDataHeader = {"需求主题", "产品线", "优先级（P0:0;P1:10;P2:20;P3:30）", "产品需求类型（新增功能:0;功能迭代:1;体验优化:2;技术需求:3;安全需求:4;埋点需求:5;数据需求:6）", "产品需求负责人", "预期排期时间", "所属项目", "需求描述"};

    private static final String[] importTaskDataHeader = {"任务名称", "任务状态", "产品线", "所属项目", "任务类型（0其他，1调研，2详细设计，3测试用例设计，4开发，5集测开发，6code review，7测试，8线下bug修复，9发布，10线上bug修复，11支撑，12产品设计）", "任务执行人", "计划开始时间", "计划完成时间", "实际开始时间", "实际完成时间", "任务描述", "关联产品需求", "是否为任务执行人创建代办"};

    private static final String[] updateProjectNodeDataHeader = {"项目id"};

    @Override
    public void downloadTemplate(Integer type, HttpServletResponse response) {
        String downloadName;
        String filename;

        switch (type) {
            case 0:
                downloadName = "项目导入数据模版.csv";
                filename = "ProjectTemplate.csv";
                break;
            case 1:
                downloadName = "需求导入数据模版.csv";
                filename = "DemandTemplate.csv";
                break;
            case 2:
                downloadName = "任务导入数据模版.csv";
                filename = "TaskTemplate.csv";
                break;
            default:
                throw new IllegalArgumentException("Invalid template type: " + type);
        }

        try (InputStream inputStream = this.getClass().getResourceAsStream("/data/template/" + filename);
             OutputStream outputStream = response.getOutputStream()) {

            FileUtil.templateDownload(response, inputStream, downloadName, outputStream);
        } catch (Exception e) {
            log.error("File download error: " + downloadName, e);
            throw new BaseBizRuntimeException("导入数据模版下载异常：" + downloadName);
        }
    }

    @Override
    public void importProjectData(MultipartFile file, HttpServletResponse response) {
        try {
            importDataTemplate(file, response, importProjectDataHeader, this::addProjectData, "failed_project_data.csv");
        } catch (Exception e) {
            throw new BaseBizRuntimeException(e.getMessage());
        }
    }

    @Override
    public void importDemandData(MultipartFile file, HttpServletResponse response) {
        try {
            importDataTemplate(file, response, importDemandDataHeader, this::addDemandData, "failed_demand_data.csv");
        } catch (Exception e) {
            throw new BaseBizRuntimeException(e.getMessage());
        }
    }

    @Override
    public void importTaskData(MultipartFile file, HttpServletResponse response) {
        try {
            importDataTemplate(file, response, importTaskDataHeader, this::addTaskData, "failed_task_data.csv");
        } catch (Exception e) {
            throw new BaseBizRuntimeException(e.getMessage());
        }
    }

    @Override
    public void updateProjectNode(MultipartFile file, HttpServletResponse response) {
        try {
            importDataTemplate(file, response, updateProjectNodeDataHeader, this::updateProjectNodeData, "failed_update_project_node_data.csv");
        } catch (Exception e) {
            throw new BaseBizRuntimeException(e.getMessage());
        }
    }

    // 新增通用导入方法
    private void importDataTemplate(MultipartFile file, HttpServletResponse response, String[] expectedHeader, TriConsumer<Integer, int[], String> dataProcessor, String failedFileName) {
        ImportDataUtil.validateFile(file);
        List<String> failedRecords = new ArrayList<>();

        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = bufferedReader.readLine();
            if (headerLine == null) {
                throw new BaseBizRuntimeException("文件为空");
            }

            failedRecords.add(headerLine + ",失败原因");
            int failCount = 0;
            int successCount = 0;

            List<String> verifiedFileHeader = ImportDataUtil.getVerifiedFileHeader(new BufferedReader(new StringReader(headerLine)), expectedHeader);
            int[] newIndex = ImportDataUtil.getArrayOfPositionsForVerificationFile(verifiedFileHeader, expectedHeader);
            int size = verifiedFileHeader.size();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
                // 跳过表头
                reader.readLine();
                String line;
                while ((line = reader.readLine()) != null) {
                    try {
                        dataProcessor.accept(size, newIndex, line);
                        successCount++;
                    } catch (Exception e) {
                        // 将错误消息压缩成一行，替换掉换行符
                        String errorMessage = e.getMessage().replaceAll("[\\r\\n]+", " ");
                        failedRecords.add(line + "," + errorMessage);
                        failCount++;
                        log.warn("导入数据失败，数据行: {}, 错误: {}", line, e.getMessage());
                    }
                }
            }

            log.info("数据导入完成，成功: {}条，失败: {}条", successCount, failCount);

            if (failedRecords.size() > 1) {
                generateFailedDataFile(failedRecords, response, failedFileName);
            }

        } catch (Exception e) {
            throw new BaseBizRuntimeException(e.getMessage());
        }
    }

    @NotNull
    private void addDemandData(int actualLength, int[] newIndex, String line) {
        String[] data = ImportDataUtil.splitLineData(line, actualLength);
        ProductDemandAddReq demandAddReq = new ProductDemandAddReq();
        String demandName = data[newIndex[0]];
        if (StringUtils.isEmpty(demandName)) {
            return;
        }
        demandAddReq.setName(demandName);

        String productLineName = data[newIndex[1]];
        ProductLineDO productLineDO = new ProductLineDO();
        if (StringUtils.isNotBlank(productLineName)) {
            productLineDO = productLineMapper.selectByName(productLineName);
            if (productLineDO != null) {
                demandAddReq.setProductLineId(productLineDO.getId());
            }
        }

        demandAddReq.setPriority(Integer.valueOf(data[newIndex[2]]));
        List<Integer> types = Arrays.stream(data[newIndex[3]].split(",")).map(Integer::valueOf).collect(Collectors.toList());
        demandAddReq.setTypes(types);

        // 处理负责人
        String owner;
        String[] partsStr = data[newIndex[4]].split("-");
        if (partsStr.length >= 2) {
            owner = partsStr[0] + "-" + partsStr[1];
        } else {
            owner = partsStr[0];
        }
        if (StringUtils.isNotEmpty(owner)) {
            demandAddReq.setDemandOwner(new PersonAddReq(owner, PinyinConverter.toPinyin(owner.split("-")[0])));
        } else {
            demandAddReq.setDemandOwner(new PersonAddReq(productLineDO.getOwner(), productLineDO.getOwnerId()));
        }

        // 处理日期
        try {
            if (StringUtils.isNotBlank(data[newIndex[5]])) {
                Date date = DATE_FORMAT.parse(data[newIndex[5]].trim());
                demandAddReq.setExpectScheduleTime(date);
            }
        } catch (ParseException e) {
            throw new BaseBizRuntimeException("需求排期日期格式错误: " + data[newIndex[5]]);
        }

        String projectName = data[newIndex[6]];
        if (StringUtils.isNotBlank(projectName) && projectName.contains(",")) {
            projectName = projectName.split(",")[0];
        }
        ProjectDO projectDO = projectMapper.getByName(projectName);
        if (projectDO != null) {
            demandAddReq.setProjectId(projectDO.getId());
        }
        demandAddReq.setDesc(data[newIndex[7]]);

        // 必填项校验
        if (StringUtils.isBlank(demandAddReq.getName())) {
            throw new BaseBizRuntimeException("需求名称不能为空");
        }
        if (demandAddReq.getProductLineId() == null) {
            throw new BaseBizRuntimeException("产品线不能为空");
        }
        if (demandAddReq.getPriority() == null) {
            throw new BaseBizRuntimeException("优先级不能为空");
        }
        if (CollUtil.isEmpty(demandAddReq.getTypes())) {
            throw new BaseBizRuntimeException("产品需求类型不能为空");
        }
        if (demandAddReq.getExpectScheduleTime() == null) {
            throw new BaseBizRuntimeException("需求排期不能为空");
        }
        if (demandAddReq.getDemandOwner() == null) {
            throw new BaseBizRuntimeException("产品需求负责人不能为空");
        }

        productDemandService.add(demandAddReq);
    }

    @NotNull
    private void addTaskData(int actualLength, int[] newIndex, String line) {
        String[] data = ImportDataUtil.splitLineData(line, actualLength);
        TaskAddReq taskAddReq = new TaskAddReq();
        taskAddReq.setName(data[newIndex[0]]);

        String productLineName = data[newIndex[2]];
        if (StringUtils.isNotBlank(productLineName)) {
            ProductLineDO productLineDO = productLineMapper.selectByName(productLineName);
            taskAddReq.setProductLineId(productLineDO.getId());
        }

        ProjectDO projectDO = new ProjectDO();
        String projectName = data[newIndex[3]];
        if (StringUtils.isNotBlank(projectName)) {
            projectDO = projectMapper.getByName(projectName);
            if (projectDO != null) {
                taskAddReq.setProjectId(projectDO.getId());
            }
        }
        taskAddReq.setType(Integer.valueOf(data[newIndex[4]]));

        // 处理执行人
        List<String> userNames = Arrays.asList(data[newIndex[5]].split(","));
        if (CollUtil.isNotEmpty(userNames)) {
            List<String> collect = userNames.stream().filter(StringUtils::isNotBlank).map(e -> e.split("-")).map(e -> e[0] + "-" + e[1]).collect(Collectors.toList());
            List<PersonAddReq> personAddReqs = collect.stream().map(e -> new PersonAddReq(e, PinyinConverter.toPinyin(e.split("-")[0]))).collect(Collectors.toList());
            taskAddReq.setExecutors(personAddReqs);
        }

        // 处理日期时间，如果只包含日期则添加默认时间09:00
        try {
            if (StringUtils.isNotBlank(data[newIndex[6]])) {
                String dateTimeStr = data[newIndex[6]].trim();
                Date date;
                // 判断是否只包含日期（没有时间部分）
                if (dateTimeStr.matches("\\d{4}-\\d{2}-\\d{2}") || dateTimeStr.matches("\\d{4}/\\d{2}/\\d{2}")) {
                    // 只有日期，添加默认时间09:00
                    dateTimeStr += " 09:00";
                }
                date = DATE_TIME_FORMAT.parse(dateTimeStr);
                taskAddReq.setPlanStartDate(date);
            }
        } catch (ParseException e) {
            throw new BaseBizRuntimeException("计划开始时间日期格式错误: " + data[newIndex[6]]);
        }

        // 处理日期时间，如果只包含日期则添加默认时间18:30
        try {
            if (StringUtils.isNotBlank(data[newIndex[7]])) {
                String dateTimeStr = data[newIndex[7]].trim();
                Date date;
                // 判断是否只包含日期（没有时间部分）
                if (dateTimeStr.matches("\\d{4}-\\d{2}-\\d{2}") || dateTimeStr.matches("\\d{4}/\\d{2}/\\d{2}")) {
                    // 只有日期，添加默认时间18:30
                    dateTimeStr += " 18:30";
                }
                date = DATE_TIME_FORMAT.parse(dateTimeStr);
                taskAddReq.setPlanEndDate(date);
            }
        } catch (ParseException e) {
            throw new BaseBizRuntimeException("计划结束时间日期格式错误: " + data[newIndex[7]]);
        }

        // 处理日期时间，如果只包含日期则添加默认时间09:00
        try {
            if (StringUtils.isNotBlank(data[newIndex[8]])) {
                String dateTimeStr = data[newIndex[8]].trim();
                Date date;
                // 判断是否只包含日期（没有时间部分）
                if (dateTimeStr.matches("\\d{4}-\\d{2}-\\d{2}") || dateTimeStr.matches("\\d{4}/\\d{2}/\\d{2}")) {
                    // 只有日期，添加默认时间09:00
                    dateTimeStr += " 09:00";
                }
                date = DATE_TIME_FORMAT.parse(dateTimeStr);
                taskAddReq.setActualStartDate(date);
            }
        } catch (ParseException e) {
            throw new BaseBizRuntimeException("实际开始时间日期格式错误: " + data[newIndex[8]]);
        }

        // 处理日期时间，如果只包含日期则添加默认时间18:30
        try {
            if (StringUtils.isNotBlank(data[newIndex[9]])) {
                String dateTimeStr = data[newIndex[9]].trim();
                if (dateTimeStr.contains(",")) {
                    dateTimeStr = dateTimeStr.split(",")[0];
                }
                Date date;
                // 判断是否只包含日期（没有时间部分）
                if (dateTimeStr.matches("\\d{4}-\\d{2}-\\d{2}") || dateTimeStr.matches("\\d{4}/\\d{2}/\\d{2}")) {
                    // 只有日期，添加默认时间18:30
                    dateTimeStr += " 18:30";
                }
                date = DATE_TIME_FORMAT.parse(dateTimeStr);
                taskAddReq.setActualEndDate(date);
            }
        } catch (ParseException e) {
            throw new BaseBizRuntimeException("实际结束时间日期格式错误: " + data[newIndex[9]]);
        }
        ElapsedTimeQueryReq elapsedTimeQueryReq = new ElapsedTimeQueryReq();
        elapsedTimeQueryReq.setStartTime(taskAddReq.getPlanStartDate());
        elapsedTimeQueryReq.setEndTime(taskAddReq.getPlanEndDate());
        BigDecimal planUseTime = Optional.of(taskService.getElapsedTime(elapsedTimeQueryReq)).map(BaseResult::getData).orElse(BigDecimal.ZERO);
        taskAddReq.setPlanUseTime(planUseTime);

        String taskStatus = data[newIndex[1]];
        if ("已完成".equals(taskStatus)) {
            if (taskAddReq.getActualStartDate() == null) {
                taskAddReq.setActualStartDate(taskAddReq.getPlanStartDate());
            }
            if (taskAddReq.getActualEndDate() == null) {
                taskAddReq.setActualEndDate(taskAddReq.getPlanEndDate());
            }
        }

        taskAddReq.setDesc(data[newIndex[10]]);

        // 关联产品需求
        List<String> productNames = Arrays.asList(data[newIndex[11]].split(","));
        List<Long> productDemandIds = new ArrayList<>(productNames.size());
        for (String productName : productNames) {
            ProductDemandDO productDemandDO = productDemandMapper.getByName(productName);
            if (productDemandDO != null) {
                productDemandIds.add(productDemandDO.getId());
            }
        }

        ProjectProductDemandQueryList query = new ProjectProductDemandQueryList();
        query.setProjectId(projectDO.getId());
        query.setPageNum(1);
        query.setPageSize(1000);
        List<ProductDemandVO> productDemandVOS = Optional.of(projectService.linkProductDemandList(query)).map(BaseResult::getData).map(PageQueryResult::getResultList).get();
        List<Long> demandIds = productDemandVOS.stream().map(ProductDemandVO::getId).collect(Collectors.toList());

        List<Long> list = productDemandIds.stream().filter(e -> !demandIds.contains(e)).collect(Collectors.toList());
        if (CollUtil.isNotEmpty(list)) {
            ProjectProductDemandLinkReq req = new ProjectProductDemandLinkReq();
            req.setProjectId(projectDO.getId());
            req.setType(LinkOrUnLinkEnum.LINK.getCode());
            req.setProductDemandIds(list);
            // 批量关联
            projectService.linkOrUnLinkProductDemand(req);
        }

        // 关联产品需求
        taskAddReq.setProductDemandIds(productDemandIds);

        taskAddReq.setTodo(Boolean.valueOf(data[newIndex[12]]));

        // 必填项校验
        if (StringUtils.isBlank(taskAddReq.getName())) {
            throw new BaseBizRuntimeException("任务名称不能为空");
        }
        if (taskAddReq.getProductLineId() == null) {
            throw new BaseBizRuntimeException("产品线不能为空");
        }
        if (taskAddReq.getProjectId() == null) {
            throw new BaseBizRuntimeException("所属项目不能为空");
        }
        if (taskAddReq.getType() == null) {
            throw new BaseBizRuntimeException("项目类型不能为空");
        }
        if (taskAddReq.getPlanStartDate() == null) {
            throw new BaseBizRuntimeException("计划开始时间不能为空");
        }
        if (taskAddReq.getPlanEndDate() == null) {
            throw new BaseBizRuntimeException("计划结束时间不能为空");
        }
        if (CollUtil.isEmpty(taskAddReq.getExecutors())) {
            throw new BaseBizRuntimeException("任务执行人不能为空");
        }

        taskService.add(taskAddReq);
    }

    /**
     * 生成失败数据文件
     *
     * @param failedRecords 失败记录列表
     * @param response      HttpServletResponse对象
     * @param filename      文件名
     */
    private void generateFailedDataFile(List<String> failedRecords, HttpServletResponse response, String filename) {
        try {
            FileUtil.downloadFailedData(failedRecords, response, filename);
        } catch (IOException e) {
            log.error("生成失败数据文件异常", e);
        }
    }

    @NotNull
    private void addProjectData(int actualLength, int[] newIndex, String line) {
        String[] data = ImportDataUtil.splitLineData(line, actualLength);
        String projectName = data[newIndex[0]];
        if (StringUtils.isBlank(projectName)) {
            throw new BaseBizRuntimeException("项目名称不能为空");
        }
        String projectIdStr = data[newIndex[8]];
        if (StringUtils.isNotBlank(projectIdStr)) {
            ProjectDO projectDO = projectMapper.get(Long.valueOf(projectIdStr));
            if (Objects.nonNull(projectDO)) {
                projectDO.setName(projectName);
                projectMapper.update(projectDO);
                return;
            }
        }
        ProjectAddReq projectAddReq = new ProjectAddReq();
        projectAddReq.setName(projectName);
        List<String> productLines = Arrays.asList(data[newIndex[1]].split(","));
        List<Long> productLineIds = productLineMapper.selectByProductLineNames(productLines).stream().map(ProductLineDO::getId).collect(Collectors.toList());
        projectAddReq.setProductLineIds(productLineIds);
        projectAddReq.setKind(Integer.valueOf(data[newIndex[2]]));
        projectAddReq.setType(Integer.valueOf(data[newIndex[3]]));
        projectAddReq.setLevel(Integer.valueOf(data[newIndex[4]]));
        try {
            if (StringUtils.isNotBlank(data[newIndex[5]])) {
                Date date = DATE_FORMAT.parse(data[newIndex[5]].trim());
                projectAddReq.setPlanStartDate(date);
            }
        } catch (ParseException e) {
            throw new BaseBizRuntimeException("计划开始时间日期格式错误: " + data[newIndex[5]]);
        }
        try {
            if (StringUtils.isNotBlank(data[newIndex[6]])) {
                Date date = DATE_FORMAT.parse(data[newIndex[6]].trim());
                projectAddReq.setPlanEndDate(date);
            }
        } catch (ParseException e) {
            throw new BaseBizRuntimeException("计划结束时间日期格式错误: " + data[newIndex[6]]);
        }
        projectAddReq.setPriority(Integer.valueOf(data[newIndex[7]]));

        Set<String> userNames = new HashSet<>();
        // 项目经理
        String pm = data[newIndex[9]];
        userNames.add(pm);
        // 产品经理
        List<String> pds = Arrays.asList(data[newIndex[10]].split(","));
        userNames.addAll(pds);
        // 项目负责人
        String principal;
        String[] partsStr = data[newIndex[11]].split("-");
        if (partsStr.length >= 2) {
            principal = partsStr[0] + "-" + partsStr[1];
        } else {
            principal = partsStr[0];
        }
        userNames.add(principal);
        // 项目成员
        List<String> members = Arrays.asList(data[newIndex[12]].split(","));
        // 项目成员
        List<String> teamMembers = members.stream()
                .map(e -> {
                    String[] parts = e.split("-");
                    if (parts.length >= 2) {
                        return parts[0] + "-" + parts[1];
                    } else {
                        return e;
                    }
                })
                .collect(Collectors.toList());
        userNames.addAll(teamMembers);
        // 获取人员信息
        Map<String, PersonAddReq> personAddReqMap = new HashMap<>(userNames.size());
        for (String userName : userNames) {
            String pinyin = PinyinConverter.toPinyin(userName.split("-")[0]);
            personAddReqMap.put(userName, new PersonAddReq(userName, pinyin));
        }
        // 项目经理赋值
        projectAddReq.setPm(personAddReqMap.getOrDefault(pm, null));
        List<PersonAddReq> pdsAddList = pds.stream().map(e -> personAddReqMap.getOrDefault(e, null)).collect(Collectors.toList());
        // 产品经理赋值
        projectAddReq.setPds(pdsAddList);
        // 项目负责人赋值
        projectAddReq.setPrincipal(personAddReqMap.getOrDefault(principal, null));
        List<PersonAddReq> teamMembersAddList = teamMembers.stream().map(e -> personAddReqMap.getOrDefault(e, null)).collect(Collectors.toList());
        // 项目团队成员赋值
        projectAddReq.setTeamMembers(teamMembersAddList);

        projectAddReq.setDesc(data[newIndex[12]]);

        projectAddReq.setIsPlatformPublish(Integer.valueOf(data[newIndex[14]]));
        projectAddReq.setIsAcceptance(Integer.valueOf(data[newIndex[15]]));
        projectAddReq.setIsWithGoal(Integer.valueOf(data[newIndex[16]]));

        projectAddReq.setWorkHoursNotify(0);

        // 必填项校验
        if (StringUtils.isBlank(projectAddReq.getName())) {
            throw new BaseBizRuntimeException("项目名称不能为空");
        }
        if (CollUtil.isEmpty(projectAddReq.getProductLineIds())) {
            throw new BaseBizRuntimeException("产品线不能为空");
        }
        if (projectAddReq.getKind() == null) {
            throw new BaseBizRuntimeException("项目类型不能为空");
        }
        if (projectAddReq.getType() == null) {
            throw new BaseBizRuntimeException("项目性质不能为空");
        }
        if (projectAddReq.getLevel() == null) {
            throw new BaseBizRuntimeException("项目等级不能为空");
        }
        if (projectAddReq.getPlanStartDate() == null) {
            throw new BaseBizRuntimeException("计划开始时间不能为空");
        }
        if (projectAddReq.getPlanEndDate() == null) {
            throw new BaseBizRuntimeException("计划结束时间不能为空");
        }
        if (projectAddReq.getPriority() == null) {
            throw new BaseBizRuntimeException("优先级不能为空");
        }
        if (projectAddReq.getPm() == null) {
            throw new BaseBizRuntimeException("项目经理不能为空");
        }
        if (CollUtil.isEmpty(projectAddReq.getPds())) {
            throw new BaseBizRuntimeException("产品经理不能为空");
        }
        if (projectAddReq.getPrincipal() == null) {
            throw new BaseBizRuntimeException("项目负责人不能为空");
        }
        if (CollUtil.isEmpty(projectAddReq.getTeamMembers())) {
            throw new BaseBizRuntimeException("项目成员不能为空");
        }

        projectService.add(projectAddReq);
    }

    private void updateProjectNodeData(int actualLength, int[] newIndex, String line) {
        String[] data = ImportDataUtil.splitLineData(line, actualLength);
        if (StringUtils.isEmpty(data[newIndex[0]])) {
            return;
        }
        Long projectId = Long.valueOf(data[newIndex[0]]);

        ProjectDetailVO projectDetailVO = Optional.ofNullable(projectService.get(projectId)).map(BaseResult::getData).get();

        ProjectModifyReq projectModifyReq = new ProjectModifyReq();
        projectModifyReq.setId(projectId);
        projectModifyReq.setName(projectDetailVO.getName());
        projectModifyReq.setDelayType(-1);
        projectModifyReq.setProductLineIds(projectDetailVO.getProductLineVO().stream().map(e -> e.getId()).collect(Collectors.toList()));
        projectModifyReq.setKind(projectDetailVO.getKind());
        projectModifyReq.setType(projectDetailVO.getType());
        projectModifyReq.setPriority(projectDetailVO.getPriority());
        projectModifyReq.setPlanStartDate(projectDetailVO.getPlanStartDate());
        projectModifyReq.setPlanEndDate(projectDetailVO.getPlanEndDate());
        projectModifyReq.setPm(new PersonAddReq(projectDetailVO.getPmName(), projectDetailVO.getPmId()));
        projectModifyReq.setPds(projectDetailVO.getPd().stream().map(e -> new PersonAddReq(e.getUserName(), e.getUserId())).collect(Collectors.toList()));
        projectModifyReq.setSr(new PersonAddReq(projectDetailVO.getSr(), projectDetailVO.getSrId()));
        projectModifyReq.setPrincipal(new PersonAddReq(projectDetailVO.getPrincipal(), projectDetailVO.getPrincipalId()));
        projectModifyReq.setTeamMembers(projectDetailVO.getTeamMember().stream().map(e -> new PersonAddReq(e.getUserName(), e.getUserId())).collect(Collectors.toList()));
        projectModifyReq.setOtnPrincipal(new PersonAddReq(projectDetailVO.getOtnPrincipal(), projectDetailVO.getOtnPrincipalId()));
        projectModifyReq.setDesc(projectDetailVO.getDesc());
        projectModifyReq.setSuspendReason(projectDetailVO.getSuspendReason());
        projectModifyReq.setInvalidReason(projectDetailVO.getInvalidReason());
        projectModifyReq.setIsAcceptance(projectDetailVO.getIsAcceptance());
        projectModifyReq.setIsWithGoal(projectDetailVO.getIsWithGoal());
        projectModifyReq.setUnWriteReason(projectDetailVO.getUnWriteReason());
        projectModifyReq.setWorkHoursNotify(projectDetailVO.getWorkHoursNotify());
        projectModifyReq.setIsPlatformPublish(projectDetailVO.getIsPlatformPublish());
        projectModifyReq.setResourceAssessment(projectDetailVO.getResourceAssessment());
        projectModifyReq.setProjectGoals(new ArrayList<>());
        projectModifyReq.setDelayType(-1);
        projectModifyReq.setLevel(projectDetailVO.getLevel());

        List<ProjectNodeAddReq> projectNodeAddReqList = Lists.newArrayList();
        for (ProjectNodeVO projectNodeVO : projectDetailVO.getProjectNodes()) {
            ProjectNodeAddReq projectNodeAddReq = new ProjectNodeAddReq();
            projectNodeAddReq.setName(projectNodeVO.getName());
            projectNodeAddReq.setPlanDate(projectDetailVO.getPlanStartDate());
            if ("发布正式".equals(projectNodeVO.getName())) {
                projectNodeAddReq.setActualDate(projectDetailVO.getPlanEndDate());
            } else {
                projectNodeAddReq.setActualDate(projectDetailVO.getPlanStartDate());
            }
            projectNodeAddReqList.add(projectNodeAddReq);
        }
        projectModifyReq.setProjectNodes(projectNodeAddReqList);
        projectService.modify(projectModifyReq);

        ProjectEvaluateReq projectEvaluateReq = getProjectEvaluateReq(projectId);
        evaluateService.evaluateUpdate(projectEvaluateReq);

        ProjectConclusionReq req = new ProjectConclusionReq();
        req.setProjectId(projectId);
        req.setTargetStatus(ProjectStatusEnum.CONCLUSION.getCode());
        req.setIsImport(true);
        projectService.conclusion(req);
    }

    private ProjectEvaluateReq getProjectEvaluateReq(Long projectId) {
        List<EvaluateReq> evaluateReqList = Lists.newArrayList();
        ProjectEvaluateReq projectEvaluateReq = new ProjectEvaluateReq();
        EvaluateReq evaluateReq1 = new EvaluateReq();
        evaluateReq1.setProjectId(projectId);
        evaluateReq1.setEvaluateDimensionId(1L);
        evaluateReq1.setScores(new BigDecimal(5));

        EvaluateReq evaluateReq2 = new EvaluateReq();
        evaluateReq2.setProjectId(projectId);
        evaluateReq2.setEvaluateDimensionId(2L);
        evaluateReq2.setScores(new BigDecimal(5));

        evaluateReqList.add(evaluateReq1);
        evaluateReqList.add(evaluateReq2);
        projectEvaluateReq.setEvaluateReqList(evaluateReqList);
        return projectEvaluateReq;
    }
}