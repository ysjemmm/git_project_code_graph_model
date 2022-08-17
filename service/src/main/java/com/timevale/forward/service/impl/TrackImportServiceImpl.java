package com.timevale.forward.service.impl;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.EasyExcelFactory;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.fastjson.JSON;
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
import com.timevale.forward.service.excel.track.map.ClassifyRow;
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
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
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
        List<BizDomainDO> bizDomainDOList = bizDomainMapper.selectAllBizDomain();
        List<ProductLineDO> productLineDOList = productLineMapper.selectAllProductLine();
        List<ModelDO> modelDOList = modelMapper.selectAllModel();
        List<TrackMapDO> trackMapDOList = trackMapMapper.selectAllTrackMap();

        Map<Long, List<ProductLineDO>> plg = productLineDOList.stream().collect(Collectors.groupingBy(ProductLineDO::getBizDomainId));
        Map<Long, List<ModelDO>> mdg = modelDOList.stream().collect(Collectors.groupingBy(ModelDO::getProductLineId));
        Map<Long, List<TrackMapDO>> pg = trackMapDOList.stream().filter(e -> e.getLevel() == 4).collect(Collectors.groupingBy(TrackMapDO::getParentId));
        Map<Long, List<TrackMapDO>> elg = trackMapDOList.stream().filter(e -> e.getLevel() == 5).collect(Collectors.groupingBy(TrackMapDO::getParentId));

        List<ClassifyData> subBizList = new ArrayList<>();
        ClassifyData root = new ClassifyData();

        for (BizDomainDO biz : bizDomainDOList) {
            ClassifyData bizData = new ClassifyData();
            List<ClassifyData> subPlList = new ArrayList<>();

            List<ProductLineDO> pls = plg.get(biz.getId());
            if (CollectionUtil.isNotEmpty(pls)) {
                for (ProductLineDO pl : pls) {
                    ClassifyData plData = new ClassifyData();
                    List<ClassifyData> subMdList = new ArrayList<>();

                    List<ModelDO> mds = mdg.get(pl.getId());
                    if (CollectionUtil.isNotEmpty(mds)) {
                        for (ModelDO md : mds) {
                            ClassifyData mdData = new ClassifyData();
                            List<ClassifyData> subPList = new ArrayList<>();

                            List<TrackMapDO> ps = pg.get(md.getId());
                            if (CollectionUtil.isNotEmpty(ps)) {
                                for (TrackMapDO p : ps) {
                                    ClassifyData pData = new ClassifyData();
                                    List<ClassifyData> subElList = new ArrayList<>();

                                    List<TrackMapDO> els = elg.get(p.getId());
                                    if (CollectionUtil.isNotEmpty(els)) {
                                        for (TrackMapDO el : els) {
                                            ClassifyData elData = new ClassifyData();
                                            elData.setId(el.getId());
                                            elData.setLevel(5);
                                            elData.setName(el.getName());
                                            elData.setSize(1);
                                            subElList.add(elData);
                                        }
                                    }
                                    pData.setId(p.getId());
                                    pData.setLevel(4);
                                    pData.setName(p.getName());
                                    pData.setSubClassifyData(subElList);
                                    pData.setSize(Math.max(1, subElList.size()));
                                    subPList.add(pData);
                                }
                            }
                            mdData.setId(md.getId());
                            mdData.setLevel(3);
                            mdData.setName(md.getName());
                            mdData.setSubClassifyData(subPList);
                            mdData.setSize(Math.max(1, subPList.stream().mapToInt(ClassifyData::getSize).sum()));
                            subMdList.add(mdData);
                        }
                    }
                    plData.setId(pl.getId());
                    plData.setLevel(2);
                    plData.setName(pl.getName());
                    plData.setSubClassifyData(subMdList);
                    plData.setSize(Math.max(1, subMdList.stream().mapToInt(ClassifyData::getSize).sum()));
                    subPlList.add(plData);
                }
            }
            bizData.setId(biz.getId());
            bizData.setLevel(1);
            bizData.setName(biz.getName());
            bizData.setSubClassifyData(subPlList);
            bizData.setSize(Math.max(1, subPlList.stream().mapToInt(ClassifyData::getSize).sum()));
            subBizList.add(bizData);
        }
        root.setId(0L);
        root.setLevel(1);
        root.setName("root");
        root.setSize(Math.max(1, subBizList.stream().mapToInt(ClassifyData::getSize).sum()));
        root.setSubClassifyData(subBizList);

        List<ClassifyRow> classifyRowList = new ArrayList<>();


        FileDownloadDTO info = FileUtil.getFileDownloadInfo(templateFileId, envUtils.getEnv());
        if (info == null || StrUtil.isEmpty(info.getDownloadUrl())) {
            throw new BaseBizRuntimeException("模板文件不存在");
        }
        TrackImportLogFileVO result = TrackImportLogCopier.INSTANCE.convert(info);
        return BaseResult.success(result);
    }

    private void dfs(ClassifyData data, List<ClassifyRow> rowList, ClassifyRow row) throws IOException, InvalidFormatException {
        Integer level = data.getLevel();

        Workbook workbook = WorkbookFactory.create(new File("/test.xlsx"));
        Sheet sheet = workbook.getSheetAt(1);


        // 赋值
        Field[] fields = ReflectUtil.getFields(ClassifyRow.class);
        for (Field field : fields) {
            int index = field.getAnnotation(ExcelProperty.class).index();
            if (index == level - 1) {
                try {
                    field.set(row, data.getName());
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        List<ClassifyData> subList = data.getSubClassifyData();
        if (CollectionUtil.isEmpty(subList)) {
            rowList.add(row);
        } else {
            for (int i = 0; i < subList.size(); i++) {
                if (i == 0) {
                    dfs(subList.get(i), rowList, row);
                } else {
                    dfs(subList.get(i), rowList, new ClassifyRow());
                }
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
