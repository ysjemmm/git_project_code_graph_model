package com.timevale.forward.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.LabelCategoryBizDomainMapper;
import com.timevale.forward.dal.dao.LabelCategoryMapper;
import com.timevale.forward.dal.dao.LabelMapper;
import com.timevale.forward.dal.dao.ProductDemandGroupItemMapper;
import com.timevale.forward.dal.dao.ProductDemandGroupMapper;
import com.timevale.forward.dal.dao.ProductDemandMapper;
import com.timevale.forward.dal.dao.ProductLineMapper;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.entity.LabelCategoryBizDomainDO;
import com.timevale.forward.dal.entity.LabelCategoryDO;
import com.timevale.forward.dal.entity.LabelDO;
import com.timevale.forward.dal.entity.ProductDemandDO;
import com.timevale.forward.dal.entity.ProductDemandGroupDO;
import com.timevale.forward.dal.entity.ProductDemandGroupItemDO;
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
import com.timevale.forward.model.enums.ProductDemandStatusEnum;
import com.timevale.forward.model.enums.ProjectStatusEnum;
import com.timevale.forward.service.component.BizLabelComponent;
import com.timevale.forward.service.utils.file.FileUtil;
import com.timevale.forward.service.utils.file.ImportDataUtil;
import com.timevale.forward.service.utils.file.PinyinConverter;
import com.timevale.forward.service.utils.position.PositionUtil;
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
import java.util.concurrent.CompletableFuture;
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

    @Resource
    private LabelCategoryMapper labelCategoryMapper;

    @Resource
    private LabelCategoryBizDomainMapper labelCategoryBizDomainMapper;

    @Resource
    private LabelMapper labelMapper;

    @Resource
    private ProductDemandGroupMapper productDemandGroupMapper;

    @Resource
    private ProductDemandGroupItemMapper productDemandGroupItemMapper;

    @Resource
    private BizLabelComponent bizLabelComponent;

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

    // ==================== 需求规划Excel导入 ====================

    /**
     * 业务域集id，固定为26
     */
    private static Long BIZ_DOMAIN_GROUP_ID = 26L;
    private static String PRODUCT_LINE_NAME = "国际站";

    /**
     * 需求状态映射：Excel中的状态文本 -> 系统ProductDemandStatusEnum
     */
    private static final Map<String, Integer> STATUS_MAPPING = new HashMap<String, Integer>() {{
        put("未开始", ProductDemandStatusEnum.WAITING.getCode());
        put("等待排期", ProductDemandStatusEnum.WAITING.getCode());
        put("发模拟", ProductDemandStatusEnum.DEV_COMPLETED.getCode());
        put("研发完成", ProductDemandStatusEnum.DEV_COMPLETED.getCode());
        put("已完成", ProductDemandStatusEnum.ONLINE.getCode());
        put("完成上线", ProductDemandStatusEnum.ONLINE.getCode());
        put("已上线", ProductDemandStatusEnum.ONLINE.getCode());
        put("研发中", ProductDemandStatusEnum.DEVELOPING.getCode());
        put("研发进行", ProductDemandStatusEnum.DEVELOPING.getCode());
        put("开发中", ProductDemandStatusEnum.DEVELOPING.getCode());
        put("已列项目", ProductDemandStatusEnum.INCLUDED.getCode());
        put("项目进行", ProductDemandStatusEnum.PROGRESS.getCode());
        put("已暂停", ProductDemandStatusEnum.SUSPEND.getCode());
        put("需求暂停", ProductDemandStatusEnum.SUSPEND.getCode());
        put("已作废", ProductDemandStatusEnum.INVALID.getCode());
        put("需求作废", ProductDemandStatusEnum.INVALID.getCode());
        put("项目暂停", ProductDemandStatusEnum.PJ_SUSPEND.getCode());
    }};

    @Override
    public void importDemandPlanData(Long bizDomainGroupId, String productLineName, MultipartFile file, HttpServletResponse response) {
        try {
            // 立即返回成功响应
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":true,\"message\":\"导入任务已提交，正在后台处理\"}");
            response.getWriter().flush();
            
            // 使用CompletableFuture异步执行导入任务
            CompletableFuture.runAsync(() -> {
                try {
                    if (bizDomainGroupId != null) {
                        BIZ_DOMAIN_GROUP_ID = bizDomainGroupId;
                    }
                    if (StringUtils.isNotBlank(productLineName)) {
                        PRODUCT_LINE_NAME = productLineName;
                    }
                    doImportDemandPlanData(file, null);
                    log.info("需求规划数据导入任务执行完成");
                } catch (Exception e) {
                    log.warn("异步导入需求规划数据异常", e);
                }
            });
        } catch (Exception e) {
            log.warn("提交导入需求规划数据任务异常", e);
            throw new BaseBizRuntimeException("提交导入任务异常：" + e.getMessage());
        }
    }

    private void doImportDemandPlanData(MultipartFile file, HttpServletResponse response) {
        // 1. 读取Excel数据
        List<Map<Integer, String>> allData = new ArrayList<>();
        List<String> headerList = new ArrayList<>();

        EasyExcel.read(getInputStream(file), new AnalysisEventListener<Map<Integer, String>>() {
            @Override
            public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
                headMap.forEach((k, v) -> headerList.add(v != null ? v.trim() : ""));
            }

            @Override
            public void invoke(Map<Integer, String> data, AnalysisContext context) {
                allData.add(data);
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {
                log.info("Excel解析完成，共{}条数据", allData.size());
            }
        }).sheet().doRead();

        if (allData.isEmpty()) {
            throw new BaseBizRuntimeException("Excel文件数据为空");
        }

        // 2. 解析列索引
        Map<String, Integer> colIndex = new HashMap<>();
        for (int i = 0; i < headerList.size(); i++) {
            colIndex.put(headerList.get(i), i);
        }

        // 需要的标签类别列
        String[] labelCategoryColumns = {"模块", "ePaaS", "UED", "来源"};
        // 3. 创建标签类别和标签
        Map<String, Long> labelNameToIdMap = createLabelsFromExcel(allData, colIndex, labelCategoryColumns);

        // 4. 创建需求规划组并导入需求
        List<String> failedRecords = new ArrayList<>();
        failedRecords.add("行号,需求名称,失败原因");
        int successCount = 0;
        int failCount = 0;

        // 按分组列+排期列组合分组
        Integer groupColIdx = colIndex.get("分组");
        Integer scheduleColIdx = colIndex.get("排期");

        // 收集所有分组名称（排期+分组组合）
        Map<String, Long> groupNameToIdMap = new HashMap<>();

        for (int i = 0; i < allData.size(); i++) {
            Map<Integer, String> row = allData.get(i);
            try {
                String demandName = getCellValue(row, colIndex.get("需求名称"));
                if (demandName == null) {
                    demandName = getCellValue(row, colIndex.get("需求"));
                }
                if (demandName == null) {
                    // 尝试第一列
                    demandName = getCellValue(row, 0);
                }
                if (demandName == null || demandName.isEmpty()) {
                    continue;
                }

                String schedule = scheduleColIdx != null ? getCellValue(row, scheduleColIdx) : null;
                String group = groupColIdx != null ? getCellValue(row, groupColIdx) : null;

                // 构建分组名称
                String groupName = buildGroupName(schedule, group);

                // 获取或创建分组
                Long groupId = null;
                if (groupName != null) {
                    groupId = groupNameToIdMap.computeIfAbsent(groupName, name -> getOrCreateDemandGroup(name));
                }

                // 创建需求
                importSingleDemand(row, colIndex, demandName, groupId, labelNameToIdMap, labelCategoryColumns);
                successCount++;
            } catch (Exception e) {
                String demandName = getCellValue(row, colIndex.containsKey("需求名称") ? colIndex.get("需求名称") : 0);
                String errorMsg = e.getMessage() != null ? e.getMessage().replaceAll("[\\r\\n]+", " ") : "未知错误";
                failedRecords.add((i + 2) + "," + demandName + "," + errorMsg);
                failCount++;
                log.warn("导入需求规划数据失败，行号: {}, 错误: {}", i + 2, e.getMessage());
            }
        }

        log.info("需求规划数据导入完成，成功: {}条，失败: {}条", successCount, failCount);

        if (failedRecords.size() > 1 && response != null) {
            generateFailedDataFile(failedRecords, response, "failed_demand_plan_data.csv");
        } else if (failedRecords.size() > 1) {
            log.warn("导入失败记录：\n{}", String.join("\n", failedRecords));
        }
    }

    /**
     * 从Excel数据中创建标签类别和标签
     */
    private Map<String, Long> createLabelsFromExcel(List<Map<Integer, String>> allData, Map<String, Integer> colIndex, String[] labelCategoryColumns) {
        Map<String, Long> labelNameToIdMap = new HashMap<>();

        for (String categoryName : labelCategoryColumns) {
            Integer colIdx = colIndex.get(categoryName);
            if (colIdx == null) {
                log.info("Excel中不存在列: {}, 跳过", categoryName);
                continue;
            }

            // 收集该列所有去重的值
            Set<String> uniqueValues = new HashSet<>();
            for (Map<Integer, String> row : allData) {
                String value = getCellValue(row, colIdx);
                if (value != null && !value.isEmpty()) {
                    uniqueValues.add(value);
                }
            }

            if (uniqueValues.isEmpty()) {
                continue;
            }

            // 获取或创建标签类别
            Long categoryId = getOrCreateLabelCategory(categoryName);

            // 获取该类别下已有的标签
            List<LabelDO> existingLabels = labelMapper.getByCategoryIds(Arrays.asList(categoryId), false);
            Map<String, Long> existingLabelMap = existingLabels.stream()
                    .collect(Collectors.toMap(LabelDO::getName, LabelDO::getId, (v1, v2) -> v1));

            // 创建不存在的标签
            List<String> newLabelNames = uniqueValues.stream()
                    .filter(name -> !existingLabelMap.containsKey(name))
                    .collect(Collectors.toList());

            if (!newLabelNames.isEmpty()) {
                List<LabelDO> newLabels = newLabelNames.stream().map(name -> {
                    LabelDO labelDO = new LabelDO();
                    labelDO.setLabelCategoryId(categoryId);
                    labelDO.setName(name);
                    return labelDO;
                }).collect(Collectors.toList());
                labelMapper.batchInsert(newLabels);

                // 重新查询获取id
                List<LabelDO> allLabels = labelMapper.getByCategoryIds(Arrays.asList(categoryId), false);
                allLabels.forEach(label -> existingLabelMap.put(label.getName(), label.getId()));
            }

            // 将所有标签名->id放入map，key加上类别前缀避免冲突
            existingLabelMap.forEach((name, id) -> labelNameToIdMap.put(categoryName + ":" + name, id));
            log.info("标签类别[{}]处理完成，共{}个标签", categoryName, existingLabelMap.size());
        }

        return labelNameToIdMap;
    }

    /**
     * 获取或创建标签类别
     */
    private Long getOrCreateLabelCategory(String categoryName) {
        List<LabelCategoryDO> existing = labelCategoryMapper.getByName(categoryName);
        // 查找type包含11（产品需求）的类别
        Optional<LabelCategoryDO> matched = existing.stream()
                .filter(c -> c.getType() != null && c.getType().contains("11"))
                .findFirst();

        if (matched.isPresent()) {
            return matched.get().getId();
        }

        // 创建新的标签类别
        LabelCategoryDO labelCategoryDO = new LabelCategoryDO();
        labelCategoryDO.setName(categoryName);
        labelCategoryDO.setType("[11]"); // 产品需求类型
        labelCategoryDO.setMarkMan("[\"雨桦-杨军辉\"]");
        labelCategoryDO.setMarkManId("[\"yuhua\"]");
        labelCategoryDO.setProtection(0);
        labelCategoryMapper.insert(labelCategoryDO);

        // 关联业务域：插入label_category_biz_domain关联表数据
        LabelCategoryBizDomainDO lcbdDO = new LabelCategoryBizDomainDO();
        lcbdDO.setLabelCategoryId(labelCategoryDO.getId());
        lcbdDO.setBizDomainId(BIZ_DOMAIN_GROUP_ID); // 业务域id为52
        labelCategoryBizDomainMapper.batchInsert(Arrays.asList(lcbdDO));

        log.info("创建标签类别: {}, id: {}, 关联业务域id: {}", categoryName, labelCategoryDO.getId(), BIZ_DOMAIN_GROUP_ID);
        return labelCategoryDO.getId();
    }

    /**
     * 构建分组名称
     */
    private String buildGroupName(String schedule, String group) {
        if ((schedule == null || schedule.isEmpty()) && (group == null || group.isEmpty())) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        if (schedule != null && !schedule.isEmpty()) {
            sb.append(schedule);
        }
        if (group != null && !group.isEmpty()) {
            if (sb.length() > 0) {
                sb.append("-");
            }
            sb.append(group);
        }
        return sb.toString();
    }

    /**
     * 获取或创建需求规划组
     */
    private Long getOrCreateDemandGroup(String groupName) {
        // 查询是否已存在
        ProductDemandGroupDO existing = productDemandGroupMapper.getByBizDomainGroupIdAndName(BIZ_DOMAIN_GROUP_ID, groupName);
        if (existing != null) {
            return existing.getId();
        }

        // 创建新的需求规划组
        ProductDemandGroupDO groupDO = new ProductDemandGroupDO();
        groupDO.setName(groupName);
        groupDO.setBizDomainGroupId(BIZ_DOMAIN_GROUP_ID);
        groupDO.setPosition(PositionUtil.generate(BIZ_DOMAIN_GROUP_ID.toString(), System.currentTimeMillis()));
        groupDO.setVersion(0L);
        groupDO.setOwner("system");
        groupDO.setOwnerId("system");
        groupDO.setIsActive(true);
        productDemandGroupMapper.insert(groupDO);
        log.info("创建需求规划组: {}, id: {}", groupName, groupDO.getId());
        return groupDO.getId();
    }

    /**
     * 导入单条需求
     */
    private void importSingleDemand(Map<Integer, String> row, Map<String, Integer> colIndex,
                                     String demandName, Long groupId,
                                     Map<String, Long> labelNameToIdMap, String[] labelCategoryColumns) {
        // 需求名称截断处理
        String desc = null;
        if (demandName.length() > 64) {
            desc = demandName;
            demandName = demandName.substring(0, 64);
        }

        // 查询需求是否已存在
        ProductDemandDO existingDemand = productDemandMapper.getByName(demandName);
        if (existingDemand != null) {
            // 需求已存在，直接关联到分组
            if (groupId != null) {
                addDemandToGroup(existingDemand.getId(), groupId);
            }
            // 打标签
            addLabelsForDemand(existingDemand.getId(), row, colIndex, labelNameToIdMap, labelCategoryColumns);
            return;
        }

        // 获取状态
        String statusText = getCellValue(row, colIndex.get("状态"));
        Integer status = ProductDemandStatusEnum.WAITING.getCode();
        if (statusText != null && !statusText.isEmpty()) {
            status = STATUS_MAPPING.getOrDefault(statusText.trim(), ProductDemandStatusEnum.WAITING.getCode());
        }

        // 获取优先级
        String priorityText = getCellValue(row, colIndex.get("优先级"));
        Integer priority = parsePriority(priorityText);

        // 获取产品线
        Long productLineId = null;
        if (PRODUCT_LINE_NAME != null && !PRODUCT_LINE_NAME.isEmpty()) {
            ProductLineDO productLineDO = productLineMapper.selectByName(PRODUCT_LINE_NAME);
            if (productLineDO != null) {
                productLineId = productLineDO.getId();
            }
        }

        // 获取负责人
        String ownerName = getCellValue(row, colIndex.get("创建人"));
        if (ownerName == null || ownerName.isEmpty()) {
            ownerName = getCellValue(row, colIndex.get("产品负责人"));
        }

        // 解析预期排期时间（从"排期"列）
        Date expectScheduleTime = parseScheduleDate(getCellValue(row, colIndex.get("排期")));

        // 创建需求DO
        ProductDemandDO productDemandDO = new ProductDemandDO();
        productDemandDO.setName(demandName);
        productDemandDO.setDesc(desc);
        productDemandDO.setStatus(status);
        productDemandDO.setPriority(priority != null ? priority : 20); // 默认P2
        productDemandDO.setProductLineId(productLineId);
        productDemandDO.setType("[1]"); // 默认功能迭代
        productDemandDO.setExpectScheduleTime(expectScheduleTime);

        if (ownerName != null && !ownerName.isEmpty()) {
            String[] parts = ownerName.split("-");
            String name = parts.length >= 2 ? parts[0] + "-" + parts[1] : parts[0];
            productDemandDO.setOwner(name);
            productDemandDO.setOwnerId(PinyinConverter.toPinyin(parts[0]));
        }

        productDemandMapper.insert(productDemandDO);
        log.info("创建需求: {}, id: {}, 预期排期: {}", demandName, productDemandDO.getId(), expectScheduleTime);

        // 关联到分组
        if (groupId != null) {
            addDemandToGroup(productDemandDO.getId(), groupId);
        }

        // 打标签
        addLabelsForDemand(productDemandDO.getId(), row, colIndex, labelNameToIdMap, labelCategoryColumns);
    }

    /**
     * 将需求添加到分组
     */
    private void addDemandToGroup(Long demandId, Long groupId) {
        // 检查是否已在分组中
        ProductDemandGroupItemDO existing = productDemandGroupItemMapper.getByGroupIdAndDemandId(groupId, demandId);
        if (existing != null) {
            return;
        }

        // 检查是否已在其他分组中
        ProductDemandGroupItemDO existInOther = productDemandGroupItemMapper.getByDemandId(demandId);
        if (existInOther != null) {
            log.info("需求{}已在分组{}中，跳过添加到分组{}", demandId, existInOther.getProductDemandGroupId(), groupId);
            return;
        }

        ProductDemandGroupItemDO itemDO = new ProductDemandGroupItemDO();
        itemDO.setProductDemandGroupId(groupId);
        itemDO.setProductDemandId(demandId);
        itemDO.setPosition(PositionUtil.generate(groupId.toString(), System.currentTimeMillis()));
        itemDO.setVersion(0L);
        itemDO.setIsActive(true);
        productDemandGroupItemMapper.insert(itemDO);
    }

    /**
     * 为需求打标签
     */
    private void addLabelsForDemand(Long demandId, Map<Integer, String> row, Map<String, Integer> colIndex,
                                     Map<String, Long> labelNameToIdMap, String[] labelCategoryColumns) {
        List<Long> labelIds = new ArrayList<>();
        for (String categoryName : labelCategoryColumns) {
            Integer colIdx = colIndex.get(categoryName);
            if (colIdx == null) continue;
            String value = getCellValue(row, colIdx);
            if (value != null && !value.isEmpty()) {
                Long labelId = labelNameToIdMap.get(categoryName + ":" + value);
                if (labelId != null) {
                    labelIds.add(labelId);
                }
            }
        }
        if (!labelIds.isEmpty()) {
            bizLabelComponent.addLabel(demandId, labelIds, 11); // 11=产品需求
        }
    }

    /**
     * 解析排期日期
     * 格式：260604 -> 2026-06-04
     * 待定或空值 -> 当前时间
     */
    private Date parseScheduleDate(String scheduleText) {
        if (scheduleText == null || scheduleText.isEmpty() || "待定".equals(scheduleText.trim())) {
            return new Date();
        }

        scheduleText = scheduleText.trim();
        
        // 如果已经是标准日期格式，尝试解析
        if (scheduleText.contains("-") || scheduleText.contains("/")) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                return sdf.parse(scheduleText.replace("/", "-"));
            } catch (Exception e) {
                log.warn("解析日期失败: {}, 使用当前时间", scheduleText);
                return new Date();
            }
        }

        // 解析 260604 格式
        if (scheduleText.length() == 6 && scheduleText.matches("\\d{6}")) {
            try {
                String year = "20" + scheduleText.substring(0, 2);
                String month = scheduleText.substring(2, 4);
                String day = scheduleText.substring(4, 6);
                String dateStr = year + "-" + month + "-" + day;
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                return sdf.parse(dateStr);
            } catch (Exception e) {
                log.warn("解析日期失败: {}, 使用当前时间", scheduleText);
                return new Date();
            }
        }

        // 其他格式，返回当前时间
        log.warn("无法识别的日期格式: {}, 使用当前时间", scheduleText);
        return new Date();
    }

    /**
     * 解析优先级
     */
    private Integer parsePriority(String priorityText) {
        if (priorityText == null || priorityText.isEmpty()) {
            return null;
        }
        priorityText = priorityText.trim().toUpperCase();
        if (priorityText.contains("P0") || priorityText.equals("0")) return 0;
        if (priorityText.contains("P1") || priorityText.equals("10")) return 10;
        if (priorityText.contains("P2") || priorityText.equals("20")) return 20;
        if (priorityText.contains("P3") || priorityText.equals("30")) return 30;
        return null;
    }

    /**
     * 获取单元格值
     */
    private String getCellValue(Map<Integer, String> row, Integer colIdx) {
        if (colIdx == null || row == null) return null;
        String value = row.get(colIdx);
        return value != null ? value.trim() : null;
    }

    private InputStream getInputStream(MultipartFile file) {
        try {
            return file.getInputStream();
        } catch (IOException e) {
            throw new BaseBizRuntimeException("读取文件失败：" + e.getMessage());
        }
    }
}