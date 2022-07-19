package com.timevale.forward.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.ProductDemandMapper;
import com.timevale.forward.dal.entity.FileDO;
import com.timevale.forward.dal.entity.ProductDemandDO;
import com.timevale.forward.facade.api.client.ProjectDocumentService;
import com.timevale.forward.facade.api.query.ProductDemandDocumentQueryList;
import com.timevale.forward.facade.api.result.FileVO;
import com.timevale.forward.facade.api.result.ProductDemandDocumentVO;
import com.timevale.forward.facade.api.result.ProjectFlowDocumentVO;
import com.timevale.forward.facade.api.result.TestBillDocumentVO;
import com.timevale.forward.model.enums.FileTypeEnum;
import com.timevale.forward.service.component.FileComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.FileCopier;
import com.timevale.forward.service.copy.ProductDemandCopier;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@RestService
public class ProjectDocumentServiceImpl implements ProjectDocumentService {

    @Resource
    private ProductDemandMapper productDemandMapper;
    @Resource
    private FileComponent fileComponent;


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
        ListMultimap<Long, FileVO> fileByAttachId = Multimaps.index(fileVOList, FileVO::getAttachId);
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
        return null;
    }

    @Override
    public BaseResult<ProjectFlowDocumentVO> queryTechnicalDocument(Long projectId) {
        return null;
    }

    @Override
    public BaseResult<TestBillDocumentVO> queryTestBillDocument(Long projectId) {
        return null;
    }
}
