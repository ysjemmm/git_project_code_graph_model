package com.timevale.forward.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import com.timevale.crm.sdk.common.entity.AccountInfo;
import com.timevale.crm.sdk.common.utils.SessionLocalUtil;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.ManDayMapper;
import com.timevale.forward.dal.dao.ProductDemandMapper;
import com.timevale.forward.dal.dao.ProjectFlowMapper;
import com.timevale.forward.dal.dao.TestBillMapper;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.facade.api.client.ProjectDocumentService;
import com.timevale.forward.facade.api.query.ProductDemandDocumentQueryList;
import com.timevale.forward.facade.api.request.ProjectDocumentCheckReq;
import com.timevale.forward.facade.api.request.ProjectDocumentReq;
import com.timevale.forward.facade.api.result.*;
import com.timevale.forward.model.enums.FileTypeEnum;
import com.timevale.forward.model.enums.ProjectDocumentTypeEnum;
import com.timevale.forward.model.enums.ProjectNodeEnum;
import com.timevale.forward.service.component.FileComponent;
import com.timevale.forward.service.component.ProjectDocumentComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.*;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.mandarin.base.util.AssertUtil;
import com.timevale.mandarin.base.util.StringUtils;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import org.apache.commons.collections.CollectionUtils;
import org.assertj.core.util.Lists;

import javax.annotation.Resource;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
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
        if (projectDocument.getType() >= ProjectDocumentTypeEnum.SET_UP.getCode()) {
            if (StringUtils.isBlank(req.getDocName())) {
                req.setDocName(ProjectDocumentTypeEnum.getTextByCode(projectDocument.getType()));
            }
            // 内部项目需要添加项目名称校验
            projectDocumentComponent.list(req.getProjectId())
                    .stream().filter(p -> p.getDocName() != null &&
                            Objects.equals(p.getDocName(), req.getDocName()) &&
                            Objects.equals(p.getStage(), req.getStage()))
                    .findFirst().ifPresent(p ->
                            AssertUtil.checkState(Objects.equals(p.getId(), id),
                                    "该文档名称已经存在，请修改文档名称"));
        }
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
    public BaseResult<List<ProjectDocumentVO>> listDocuments(Long projectId) {
        List<ProjectDocument> documents = projectDocumentComponent.list(projectId);
        List<FileDO> files = fileComponent.select(documents.stream().map(ProjectDocument::getId)
                .collect(Collectors.toList()), FileTypeEnum.PROJECT_DOCUMENT.getCode());
        List<FileVO> fileVOList = FileCopier.INSTANCE.transform(files);
        ImmutableListMultimap<Long, FileVO> fileByAttachId = Multimaps.index(fileVOList, FileVO::getAttacheId);
        List<ProjectDocumentVO> documentVOList = ProjectDocumentCopier.INSTANCE.do2Vo(documents);
        for (ProjectDocumentVO projectDocumentVO : documentVOList) {
            projectDocumentVO.setFiles(fileByAttachId.get(projectDocumentVO.getId()));
        }
        return BaseResult.success(documentVOList);
    }

    @Override
    public BaseResult<List<String>> checkDocBeforeRelease(ProjectDocumentCheckReq checkReq) {
        List<ProjectNodeDO> projectNodeDOList = ProjectNodeCopier.INSTANCE.convert(checkReq.getProjectNodes());
        List<String> result = projectDocumentComponent.docNeedFillIn(checkReq.getId(), projectNodeDOList, checkReq.getType());

        List<ManDayDO> manDayDOList = manDayMapper.getByProjectId(checkReq.getId());
        if (CollectionUtils.isEmpty(manDayDOList)) {
            result.add("人天明细");
        }
        return BaseResult.success(result);
    }

    @Override
    public BaseResult<Void> deleteDocument(Long projectDocumentId) {
        projectDocumentComponent.deleteDocument(projectDocumentId);
        return BaseResult.success();
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
