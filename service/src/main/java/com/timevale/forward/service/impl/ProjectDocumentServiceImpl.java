package com.timevale.forward.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import com.timevale.crm.sdk.common.entity.AccountInfo;
import com.timevale.crm.sdk.common.utils.SessionLocalUtil;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.facade.api.client.ProjectDocumentService;
import com.timevale.forward.facade.api.query.ProductDemandDocumentQueryList;
import com.timevale.forward.facade.api.request.ProjectDocumentCheckReq;
import com.timevale.forward.facade.api.request.ProjectDocumentReq;
import com.timevale.forward.facade.api.result.*;
import com.timevale.forward.model.enums.FileTypeEnum;
import com.timevale.forward.model.enums.ProjectNodeEnum;
import com.timevale.forward.model.enums.ProjectTypeEnum;
import com.timevale.forward.service.component.FileComponent;
import com.timevale.forward.service.component.ProjectDocumentComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.*;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.util.Lists;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@RestService
public class ProjectDocumentServiceImpl implements ProjectDocumentService {

    @Resource
    private ProductDemandMapper productDemandMapper;
    @Resource
    private FileComponent fileComponent;
    @Resource
    private TestBillMapper testBillMapper;
    @Resource
    private ProjectFlowMapper projectFlowMapper;
    @Resource
    private ProjectDocumentComponent projectDocumentComponent;

    @Resource
    private ProjectMapper projectMapper;

    @Resource
    private ProjectNodeMapper projectNodeMapper;

    @Resource
    private ManDayMapper manDayMapper;


    @Override
    public BaseResult<PageQueryResult<ProductDemandDocumentVO>> queryProductDemandDocuments(ProductDemandDocumentQueryList query) {

        PageHelper.startPage(query.getPageNum(), query.getPageSize(), CommonConstant.DEFAULT_ORDER_BY);
        List<ProductDemandDO> productDemands = productDemandMapper.selectByProjectId(query.getProjectId());
        PageInfo<ProductDemandDO> pageInfo = new PageInfo<>(productDemands);

        List<ProductDemandDocumentVO> documents = ProductDemandCopier.INSTANCE.convertToDocuments(productDemands);

        List<FileDO> files = fileComponent.select(documents.stream().map(ProductDemandDocumentVO::getId)
                        .collect(Collectors.toList()),
                FileTypeEnum.PRODUCT_DEMAND.getCode());
        List<FileVO> fileVOList = FileCopier.INSTANCE.transform(files);
        ListMultimap<Long, FileVO> fileByAttachId = Multimaps.index(fileVOList, FileVO::getAttacheId);
        for (ProductDemandDocumentVO document : documents) {
            document.setFiles(fileByAttachId.get(document.getId()));
        }
        PageQueryResult<ProductDemandDocumentVO> res = new PageQueryResult<>();
        res.setResultList(documents);
        ResultUtil.fillPageInfo(res, pageInfo);

        return BaseResult.success(res);
    }

    @Override
    public BaseResult<ProjectFlowDocumentVO> queryUEDDocument(Long projectId) {
        return BaseResult.success(queryFlowDocument(projectId, ProjectNodeEnum.UED_AUDIT.getCode()));
    }

    @Override
    public BaseResult<ProjectFlowDocumentVO> queryTechnicalDocument(Long projectId) {
        return BaseResult.success(queryFlowDocument(projectId, ProjectNodeEnum.TECHNICAL_DETAIL_REVIEW.getCode()));
    }

    @Override
    public BaseResult<TestBillDocumentVO> queryTestBillDocument(Long projectId) {
        TestBillDO testBill = testBillMapper.selectByProjectId(projectId);
        if (testBill == null) {
            return BaseResult.success(null);
        }
        TestBillDocumentVO document = TestBillCopier.INSTANCE.convert2Doc(testBill);
        List<FileDO> files = fileComponent.select(testBill.getProjectId(), FileTypeEnum.TEST_BILL_CASE.getCode());
        document.setFiles(FileCopier.INSTANCE.transform(files));
        return BaseResult.success(document);
    }

    @Override
    public BaseResult<ProjectDocumentVO> queryDocument(Long projectId, Integer type) {
        ProjectDocument projectDocument = projectDocumentComponent.getByProjectId(projectId, type);
        if (Objects.isNull(projectDocument)) {
            return BaseResult.success();
        }

        ProjectDocumentVO projectDocumentVO = ProjectDocumentCopier.INSTANCE.do2Vo(projectDocument);

        List<FileDO> fileDOList = fileComponent.select(projectDocument.getId(), FileTypeEnum.PROJECT_DOCUMENT.getCode());
        List<FileVO> fileVOList = FileCopier.INSTANCE.transform(fileDOList);
        projectDocumentVO.setFiles(fileVOList);

        return BaseResult.success(projectDocumentVO);
    }

    @Override
    public BaseResult<Void> saveDocument(ProjectDocumentReq req) {
        Long id = req.getId();
        AccountInfo account = SessionLocalUtil.getUserSession();

        ProjectDocument projectDocument = ProjectDocumentCopier.INSTANCE.req2Do(req);

        if (Objects.isNull(id)) {
            //新增
            Date current = new Date();

            projectDocument.setCreateMan(account.getAlias());
            projectDocument.setCreateManId(account.getAccount());
            projectDocument.setCreateDate(current);
            projectDocument.setModifyMan(account.getAlias() + CommonConstant.JOIN_LINE + account.getName());
            projectDocument.setModifyManId(account.getAccount());
            projectDocument.setModifyDate(current);
            projectDocument.setIsDeleted(false);

            projectDocumentComponent.insert(projectDocument);

            fileComponent.add(req.getFileList(), projectDocument.getId(), FileTypeEnum.PROJECT_DOCUMENT.getCode());
        } else {
            //更新
            projectDocument.setModifyMan(account.getAlias());
            projectDocument.setModifyManId(account.getAccount());
            projectDocument.setModifyDate(new Date());

            projectDocumentComponent.updateSelective(projectDocument);

            fileComponent.update(req.getFileList(), id, FileTypeEnum.PROJECT_DOCUMENT.getCode());
        }

        return BaseResult.success();
    }

    @Override
    public BaseResult<List<String>> checkDocBeforeRelease(ProjectDocumentCheckReq checkReq) {
        Long projectId=checkReq.getId();

        List<String> result = new ArrayList<>();
        if (!ProjectTypeEnum.OPTIMIZE.getCode().equals(checkReq.getType())) {
            ProjectDocument projectDocument = projectDocumentComponent.getByProjectId(projectId, 1);
            if (projectDocument == null) {
                result.add("产品需求文档");
            }
        }
        List<ProjectNodeDO> projectNodeDOList = ProjectNodeCopier.INSTANCE.convert(checkReq.getProjectNodes());
        List<Integer> codes = projectNodeDOList.stream()
                .filter(a -> ProjectNodeEnum.UED_AUDIT.getText().equals(a.getName())
                        || ProjectNodeEnum.TECHNICAL_DETAIL_REVIEW.getText().equals(a.getName()))
                .map(a -> ProjectNodeEnum.getCodeByName(a.getName())).collect(Collectors.toList());

        List<ProjectFlowDO> projectFlowDOList = projectFlowMapper.getByProjectId(projectId);
        List<Integer> types = projectFlowDOList.stream().map(ProjectFlowDO::getFlowType).collect(Collectors.toList());

        codes.removeAll(types);
        codes.forEach(a -> {
            if (ProjectNodeEnum.UED_AUDIT.getCode().equals(a)) {
                result.add("UED设计文档");
            } else if (ProjectNodeEnum.TECHNICAL_DETAIL_REVIEW.getCode().equals(a)) {
                result.add("详设文档");
            }
        });
        TestBillDO testBillDO = testBillMapper.selectByProjectId(projectId);
        if (testBillDO == null || StringUtils.isEmpty(testBillDO.getDocCreateManId())) {
            result.add("测试文档");
        }
        if (CollectionUtils.isNotEmpty(result)) {
            return BaseResult.success(result);
        }

        List<ManDayDO> manDayDOList = manDayMapper.getByProjectId(projectId);
        if (CollectionUtils.isEmpty(manDayDOList)) {
            result.add("人天明细");
        }
        return BaseResult.success(result);
    }

    private ProjectFlowDocumentVO queryFlowDocument(Long projectId, Integer flowType) {
        List<ProjectFlowDO> flows = projectFlowMapper.getByProjectIdAndType(projectId, flowType);
        if (flows.isEmpty()) {
            return null;
        }
        flows.sort(Comparator.comparing(ProjectFlowDO::getCreateDate));
        ProjectFlowDocumentVO document = ProjectFlowCopier.INSTANCE.convert2Document(flows.get(0));
        ProjectFlowDO last = flows.get(flows.size() - 1);
        document.setReviewUrl(last.getReviewUrl());
        document.setModifyManId(last.getDocModifyManId());
        document.setModifyMan(last.getDocModifyMan());
        document.setModifyDate(last.getDocModifyDate());
        document.setFiles(Lists.emptyList());
        return document;
    }

}
