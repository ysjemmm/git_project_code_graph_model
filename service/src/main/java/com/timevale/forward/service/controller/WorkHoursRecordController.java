package com.timevale.forward.service.controller;

import com.timevale.forward.facade.api.client.WorkHoursRecordService;
import com.timevale.forward.facade.api.query.OverviewWorkHoursQueryList;
import com.timevale.forward.facade.api.query.TaskExecutorWorkHoursQueryList;
import com.timevale.forward.facade.api.query.WorkHoursRecordQueryList;
import com.timevale.forward.facade.api.request.WorkHoursRecordAddReq;
import com.timevale.forward.facade.api.request.WorkHoursRecordBatchAddReq;
import com.timevale.forward.facade.api.request.WorkHoursRecordModifyReq;
import com.timevale.forward.facade.api.request.WorkHoursRecordQueryReq;
import com.timevale.forward.facade.api.result.RegisterWorkHoursTaskVO;
import com.timevale.forward.facade.api.result.WorkHoursOverviewVO;
import com.timevale.forward.facade.api.result.WorkHoursProgressVO;
import com.timevale.forward.facade.api.result.WorkHoursRecordVO;
import com.timevale.forward.facade.api.result.WorkHoursRemainVO;
import com.timevale.forward.facade.api.result.WorkbenchesWorkHoursVO;
import com.timevale.forward.model.req.CommonIdInput;
import com.timevale.forward.service.utils.ResultUtils;
import com.timevale.mandarin.common.result.BusinessResult;
import com.timevale.mandarin.common.result.PageQueryResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

/**
 * @auther: yuhua
 * @date: 2025/7/4 17:00
 * @description:
 */
@RestController
@Api(tags = "工时记录管理")
@Slf4j
@RequestMapping("/forward/workHours")
public class WorkHoursRecordController {

    @Resource
    private WorkHoursRecordService workHoursRecordService;

    @ApiOperation("工作台任务工时概览")
    @PostMapping("/overview")
    public BusinessResult<PageQueryResult<WorkHoursOverviewVO>> overview(@Validated @RequestBody OverviewWorkHoursQueryList query) {
        return ResultUtils.result(workHoursRecordService.overview(query));
    }

    @ApiOperation("工作台任务工时列表")
    @PostMapping("/workbenches")
    public BusinessResult<PageQueryResult<WorkbenchesWorkHoursVO>> workbenches(@RequestBody TaskExecutorWorkHoursQueryList query) {
        return ResultUtils.result(workHoursRecordService.workbenches(query));
    }

    @ApiOperation("待填报工时任务列表")
    @GetMapping("/waitRegTaskList")
    public BusinessResult<List<RegisterWorkHoursTaskVO>> waitRegisterTaskList(
            @ApiParam(value = "用户id") @RequestParam(value = "userId", required = false) String userId,
            @ApiParam(value = "日期") @RequestParam(value = "dateStr", required = false) String dateStr) {
        return ResultUtils.result(workHoursRecordService.waitRegisterTaskList(userId, dateStr));
    }

    @ApiOperation("工时记录列表")
    @PostMapping("/list")
    public BusinessResult<PageQueryResult<WorkHoursRecordVO>> list(@RequestBody WorkHoursRecordQueryList workHoursRecordQueryList) {
        return ResultUtils.result(workHoursRecordService.list(workHoursRecordQueryList));
    }

    @ApiOperation("工时记录新增")
    @PostMapping("/add")
    public BusinessResult<Long> add(@RequestBody @Valid WorkHoursRecordAddReq workHoursRecordAddReq) {
        return ResultUtils.result(workHoursRecordService.add(workHoursRecordAddReq));
    }

    @ApiOperation("剩余工时信息")
    @PostMapping("/remainInfo")
    public BusinessResult<WorkHoursRemainVO> remainInfo(@RequestBody @Valid WorkHoursRecordQueryReq workHoursRecordQueryReq) {
        return ResultUtils.result(workHoursRecordService.remainInfo(workHoursRecordQueryReq));
    }

    @ApiOperation("工时进度信息")
    @PostMapping("/progressInfo")
    public BusinessResult<WorkHoursProgressVO> progressInfo(@RequestBody @Valid WorkHoursRecordQueryReq workHoursRecordQueryReq) {
        return ResultUtils.result(workHoursRecordService.progressInfo(workHoursRecordQueryReq));
    }

    @ApiOperation("删除")
    @PostMapping("/delete")
    public BusinessResult<Boolean> delete(@RequestBody @Valid CommonIdInput<Long> id) {
        return ResultUtils.result(workHoursRecordService.delete(id.getId()));
    }

    @ApiOperation("工时记录修改")
    @PostMapping("/modify")
    public BusinessResult<Boolean> modify(@RequestBody @Valid WorkHoursRecordModifyReq workHoursRecordModifyReq) {
        return ResultUtils.result(workHoursRecordService.modify(workHoursRecordModifyReq));
    }

    @ApiOperation("工时记录详情")
    @GetMapping("/get")
    public BusinessResult<WorkHoursRecordVO> get(@RequestParam Long id) {
        return ResultUtils.result(workHoursRecordService.get(id));
    }

    @ApiOperation("工时记录批量新增")
    @PostMapping("/batchAdd")
    public BusinessResult<Boolean> batchAdd(@RequestBody @Valid WorkHoursRecordBatchAddReq workHoursRecordBatchAddReq) {
        return ResultUtils.result(workHoursRecordService.batchAdd(workHoursRecordBatchAddReq));
    }
}
