package com.timevale.forward.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.timevale.crm.sdk.common.entity.integration.dto.FileDownloadDTO;
import com.timevale.crm.sdk.common.utils.file.FileUtil;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.facade.api.client.TrackImportService;
import com.timevale.forward.facade.api.query.TaskImportLogQueryList;
import com.timevale.forward.facade.api.request.TrackImportReq;
import com.timevale.forward.facade.api.result.TrackImportLogFileVO;
import com.timevale.forward.facade.api.result.TrackImportLogListVO;
import com.timevale.forward.facade.api.result.TrackImportProgressVO;
import com.timevale.forward.model.enums.TrackImportLogResultEnum;
import com.timevale.forward.model.enums.TrackImportLogStatusEnum;
import com.timevale.forward.service.component.TrackImportComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.TrackImportLogCopier;
import com.timevale.forward.service.excel.track.map.ClassifyData;
import com.timevale.forward.service.utils.EnvUtils;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.aop.LogPoint;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.base.util.AssertUtil;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author by YangXu
 * @date 2022/08/08 17:18
 */
@Slf4j
@LogPoint
@RestService
public class TrackImportServiceImpl implements TrackImportService {

    @Resource
    private EnvUtils envUtils;

    @Resource
    private BizDomainMapper bizDomainMapper;
    @Resource
    private ProductLineMapper productLineMapper;
    @Resource
    private ModelMapper modelMapper;
    @Resource
    private TrackMapMapper trackMapMapper;

    @Resource
    private TrackImportLogMapper trackImportLogMapper;
    @Resource
    private TrackImportComponent trackImportComponent;

    @Value("${templateFileId:d3a98af8ea754d11ae27d50b563d9c1f}")
    private String templateFileId;

    @Override
    public BaseResult<Boolean> importEvent(TrackImportReq trackImportReq) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        String userId = userInfo.getId();

        // 判断当前是否有导入任务
        Integer progress = trackImportComponent.getProgress(userId);
        AssertUtil.checkState(progress == null, "当前已有导入任务");

        // 初始化状态
        trackImportComponent.deleteAllStatus(userId);
        trackImportComponent.deleteExceptionMessage(userId);

        // 开始导入
        trackImportComponent.setProgress(0,userId);
        trackImportComponent.setImportResult(TrackImportLogResultEnum.LOADING.getCode(), userId);
        trackImportComponent.importEvent(trackImportReq, userInfo);
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> cancel() {
        String userId = LocalSessionUtils.getUserInfo().getId();

        Integer progress = trackImportComponent.getProgress(userId);
        AssertUtil.checkState(progress != null, "取消导入失败，没有正在进行中的导入任务");
        AssertUtil.checkState(progress.compareTo(100) < 0, "取消导入失败，导入任务已完成");

        Integer importResult = trackImportComponent.getImportResult(userId);
        AssertUtil.checkState(TrackImportLogResultEnum.LOADING.getCode().equals(importResult), "取消导入失败，导入任务已完成");

        trackImportComponent.setCancelTag(userId);
        trackImportComponent.deleteImportStatus(userId);
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> cleanImportStatus(String userId) {
        trackImportComponent.deleteAllStatus(userId);
        trackImportComponent.deleteExceptionMessage(userId);
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<String> getAllImportStatus(String userId) {
        Integer progress = trackImportComponent.getProgress(userId);
        Boolean cancelTag = trackImportComponent.getCancelTag(userId);
        Integer importResult = trackImportComponent.getImportResult(userId);
        String message = trackImportComponent.getExceptionMessage(userId);
        String s = "progress：" + progress + ", cancelTag：" + cancelTag + ", importResult：" + importResult + ", message: " + message;
        return BaseResult.success(s);
    }

    @Override
    public BaseResult<TrackImportProgressVO> progress() {
        String userId = LocalSessionUtils.getUserInfo().getId();
        TrackImportProgressVO trackImportProgressVO = new TrackImportProgressVO();

        String message = trackImportComponent.getExceptionMessage(userId);
        if (StrUtil.isNotEmpty(message)) {
            trackImportComponent.deleteExceptionMessage(userId);
            trackImportProgressVO.setMessage(message);
            trackImportProgressVO.setResult(TrackImportLogResultEnum.FAILURE.getCode());
            return BaseResult.success(trackImportProgressVO);
        }

        Integer progress = trackImportComponent.getProgress(userId);
        if (progress != null) {
            trackImportProgressVO.setProgress(progress);
            trackImportProgressVO.setResult(TrackImportLogResultEnum.LOADING.getCode());
            return BaseResult.success(trackImportProgressVO);
        }

        Integer result = trackImportComponent.getImportResult(userId);
        if (result != null) {
            // 删除过气状态
            trackImportComponent.deleteAllStatus(userId);
            TrackImportLogDO trackImportLogDO = trackImportLogMapper.selectCreateLatest(userId);
            trackImportProgressVO = TrackImportLogCopier.INSTANCE.transfer(trackImportLogDO);

            // 失败文件信息
            if (TrackImportLogResultEnum.FAILURE.getCode().equals(trackImportLogDO.getResult())) {
                FileDownloadDTO fileDownloadInfo = FileUtil.getFileDownloadInfo(trackImportLogDO.getFileId(), envUtils.getEnv());
                TrackImportLogFileVO fileVO = TrackImportLogCopier.INSTANCE.convert(fileDownloadInfo);
                trackImportProgressVO.setImportLogFileVO(fileVO);
            }
            return BaseResult.success(trackImportProgressVO);
        }

        // 无导入事件
        trackImportProgressVO.setResult(TrackImportLogResultEnum.NOTING.getCode());
        return BaseResult.success(trackImportProgressVO);
    }

    @Override
    public BaseResult<TrackImportLogFileVO> template() {
        log.info("[template]:读取埋点数据");
        List<BizDomainDO> bizDomainDOList = bizDomainMapper.selectAllBizDomain();
        List<ProductLineDO> productLineDOList = productLineMapper.selectAllProductLine();
        List<ModelDO> modelDOList = modelMapper.selectAllModel();
        List<TrackMapDO> trackMapDOList = trackMapMapper.selectAllTrackMap();
        List<TrackMapDO> pageDOList = trackMapDOList.stream().filter(e -> e.getLevel() == 4).collect(Collectors.toList());
        List<TrackMapDO> elementDOList = trackMapDOList.stream().filter(e -> e.getLevel() == 5).collect(Collectors.toList());

        log.info("[template]:埋点数据转换");
        List<List<ClassifyData>> dataList = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            dataList.add(new ArrayList<>());
        }
        for (BizDomainDO e : bizDomainDOList) {
            ClassifyData data = new ClassifyData();
            data.setLevel(1);
            data.setId(e.getId());
            data.setName(e.getName());
            data.setParentId(0L);
            data.setSubClassifyData(new ArrayList<>());
            dataList.get(0).add(data);
        }
        for (ProductLineDO e : productLineDOList) {
            ClassifyData data = new ClassifyData();
            data.setLevel(2);
            data.setId(e.getId());
            data.setName(e.getName());
            data.setParentId(e.getBizDomainId());
            data.setSubClassifyData(new ArrayList<>());
            dataList.get(1).add(data);
        }
        for (ModelDO e : modelDOList) {
            ClassifyData data = new ClassifyData();
            data.setLevel(3);
            data.setId(e.getId());
            data.setName(e.getName());
            data.setParentId(e.getProductLineId());
            data.setSubClassifyData(new ArrayList<>());
            dataList.get(2).add(data);
        }
        for (TrackMapDO e : pageDOList) {
            ClassifyData data = new ClassifyData();
            data.setLevel(4);
            data.setId(e.getId());
            data.setName(e.getName());
            data.setParentId(e.getParentId());
            data.setSubClassifyData(new ArrayList<>());
            dataList.get(3).add(data);
        }
        for (TrackMapDO e : elementDOList) {
            ClassifyData data = new ClassifyData();
            data.setLevel(5);
            data.setId(e.getId());
            data.setName(e.getName());
            data.setParentId(e.getParentId());
            data.setSubClassifyData(new ArrayList<>());
            dataList.get(4).add(data);
        }
        List<Map<Long,List<ClassifyData>>> dataGroup = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            List<ClassifyData> list = dataList.get(i);
            dataGroup.add(list.stream().collect(Collectors.groupingBy(ClassifyData::getParentId)));
        }

        log.info("[template]:埋点数据关联关系梳理");
        ClassifyData root = new ClassifyData();
        root.setId(0L);
        root.setLevel(0);
        dfs(dataGroup, root);

        File template;
        try {
            template = File.createTempFile("产研系统_新增埋点事件导入模板", ".xlsx");
            template.deleteOnExit();
        } catch (IOException e) {
            log.error("[template]:模板文件临时文件创建失败");
            throw new BaseBizRuntimeException("模板文件创建失败");
        }

        // 埋点地图写入
        ClassPathResource resource = new ClassPathResource("TRACK-TEMPLATE.xlsx");
        try(InputStream ins = resource.getInputStream();) {
            // 写出埋点地图
            OutputStream ous = Files.newOutputStream(template.toPath());
            Workbook workbook = WorkbookFactory.create(ins);
            Sheet sheet = workbook.getSheet("查看埋点分类");
            if (sheet == null) {
                sheet = workbook.createSheet("查看埋点分类");
                for (int i = 0; i < 5; i++) {
                    sheet.setColumnWidth(i, 3840);
                }
            }
            dfs(root, 3, -1, workbook, sheet);
            workbook.setActiveSheet(0);
            workbook.write(ous);
        } catch (IOException | InvalidFormatException e) {
            log.error("[template]:模板文件文件埋点地图写入失败");
            throw new BaseBizRuntimeException("模板文件创建失败");
        }

        // 结果
        TrackImportLogFileVO result = new TrackImportLogFileVO();

        // 上传文件
        try {
            InputStream ins = Files.newInputStream(template.toPath());
            FileDownloadDTO info = FileUtil.uploadFileToOSS(ins, "产研系统_新增埋点事件导入模板.xlsx", envUtils.getEnv());
            if (info == null || StrUtil.isEmpty(info.getDownloadUrl())) {
                throw new BaseBizRuntimeException("模板文件创建失败");
            }

            // 结果转化
            result = TrackImportLogCopier.INSTANCE.convert(info);
        } catch (IOException e) {
            log.error("[template]:文件上传失败");
            throw new BaseBizRuntimeException("模板文件创建失败");
        }

        return BaseResult.success(result);
    }

    private void dfs(List<Map<Long,List<ClassifyData>>> dataGroup, ClassifyData data) {
        Long id = data.getId();
        Integer level = data.getLevel();
        List<ClassifyData> subDataList = new ArrayList<>();

        if (dataGroup.size() > level) {
            List<ClassifyData> list = dataGroup.get(level).get(id);
            if (CollectionUtil.isNotEmpty(list)) {
                for (ClassifyData e : list) {
                    dfs(dataGroup, e);
                    subDataList.add(e);
                }
            }
        }
        data.setSubClassifyData(subDataList);
        data.setSize(Math.max(1, subDataList.stream().mapToInt(ClassifyData::getSize).sum()));
    }

    private void dfs(ClassifyData data, Integer rowIndex, Integer cellIndex, Workbook workbook, Sheet sheet) {
        Integer level = data.getLevel();
        if (level > 0) {
            // 获取单元格
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                row = sheet.createRow(rowIndex);
            }
            Cell cell = row.getCell(cellIndex);
            if (cell == null) {
                cell = row.createCell(cellIndex);
            }

            // 配置值
            cell.setCellValue(data.getName());
            CellStyle cellStyle = workbook.createCellStyle();
            cellStyle.cloneStyleFrom(cell.getCellStyle());
            cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            cell.setCellStyle(cellStyle);

            // 合并列
            Integer size = data.getSize();
            if (size > 1) {
                CellRangeAddress address = new CellRangeAddress(rowIndex, rowIndex + size - 1, cellIndex, cellIndex);
                sheet.addMergedRegion(address);
            }
        }
        List<ClassifyData> subDataList = data.getSubClassifyData();
        if (CollectionUtil.isNotEmpty(subDataList)) {
            for (ClassifyData e : subDataList) {
                dfs(e, rowIndex, cellIndex + 1, workbook, sheet);
                rowIndex += e.getSize();
            }
        }
    }

    @Override
    public BaseResult<PageQueryResult<TrackImportLogListVO>> log(TaskImportLogQueryList query) {
        String userId = LocalSessionUtils.getUserInfo().getId();

        PageHelper.startPage(query.pageNum, query.pageSize, CommonConstant.DEFAULT_ORDER_BY);
        List<TrackImportLogDO> trackImportLogDOList = trackImportLogMapper.selectByCreateManId(userId);

        List<TrackImportLogListVO> result = trackImportLogDOList.stream().map(TrackImportLogCopier.INSTANCE::convert).collect(Collectors.toList());
        result.forEach(e -> {
            e.setStatusName(TrackImportLogStatusEnum.getTextByCode(e.getStatus()));
            e.setResultName(TrackImportLogResultEnum.getTextByCode(e.getResult()));
        });

        // 文件信息
        for (int i = 0; i < result.size(); i++) {
            String fileId = trackImportLogDOList.get(i).getFileId();
            FileDownloadDTO fileDownloadInfo = FileUtil.getFileDownloadInfo(fileId, envUtils.getEnv());
            TrackImportLogFileVO fileInfo = TrackImportLogCopier.INSTANCE.convert(fileDownloadInfo);
            result.get(i).setFileInfo(fileInfo);
        }

        // 返回分页数据
        PageInfo<TrackImportLogDO> pageInfo = new PageInfo<>(trackImportLogDOList);
        PageQueryResult<TrackImportLogListVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(result);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);

        return BaseResult.success(pageQueryResult);
    }
}
