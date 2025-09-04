package com.timevale.forward.service.controller;

import com.timevale.forward.facade.api.client.ImportDataService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

/**
 * @auther: yuhua
 * @date: 2025/7/4 17:00
 * @description:
 */
@RestController
@Api(tags = "历史数据导入")
@Slf4j
@RequestMapping("/forward/import")
public class ImportDataController {

    @Resource
    private ImportDataService importDataService;

    @ApiOperation("下载模版")
    @GetMapping("/downloadTemplate")
    public void downloadTemplate(@RequestParam("type") Integer type, HttpServletResponse response) {
        importDataService.downloadTemplate(type, response);
    }

    @ApiOperation("导入项目数据")
    @PostMapping("/importProjectData")
    public void importProjectData(@ApiParam(value = "项目文件") @RequestParam("file") MultipartFile file, HttpServletResponse response) {
        importDataService.importProjectData(file, response);
    }

    @ApiOperation("导入需求")
    @PostMapping("/importDemandData")
    public void importDemandData(@ApiParam(value = "需求文件") @RequestParam("file") MultipartFile file, HttpServletResponse response) {
        importDataService.importDemandData(file, response);
    }

    @ApiOperation("导入项目任务")
    @PostMapping("/importTaskData")
    public void importTaskData(@ApiParam(value = "任务文件") @RequestParam("file") MultipartFile file, HttpServletResponse response) {
        importDataService.importTaskData(file, response);
    }

    @ApiOperation("更新项目节点")
    @PostMapping("/updateProjectNode")
    public void updateProjectNode(@ApiParam(value = "任务文件") @RequestParam("file") MultipartFile file, HttpServletResponse response) {
        importDataService.updateProjectNode(file, response);
    }
}
