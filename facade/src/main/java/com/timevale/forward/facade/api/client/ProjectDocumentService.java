package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.query.ProductDemandDocumentQueryList;
import com.timevale.forward.facade.api.request.ProjectDocumentCheckReq;
import com.timevale.forward.facade.api.request.ProjectDocumentReq;
import com.timevale.forward.facade.api.result.ProductDemandDocumentVO;
import com.timevale.forward.facade.api.result.ProjectDocumentVO;
import com.timevale.forward.facade.api.result.ProjectFlowDocumentVO;
import com.timevale.forward.facade.api.result.TestBillDocumentVO;
import com.timevale.mandarin.common.annotation.RestClient;
import com.timevale.mandarin.common.result.PageQueryResult;

import java.util.List;

/**
 * 项目文档查询接口
 * @author jingchun
 * created on 2022.07.18
 */
@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface ProjectDocumentService {

    /**
     * 查询产品需求文档列表
     * @param query 项目id
     * @return 产品需求文档列表
     */
    BaseResult<PageQueryResult<ProductDemandDocumentVO>> queryProductDemandDocuments(ProductDemandDocumentQueryList query);

    /**
     * 查询UED文档
     * @param projectId 项目id
     * @return UED文档
     */
    BaseResult<ProjectFlowDocumentVO> queryUEDDocument(Long projectId);

    /**
     * 查询详设文档
     * @param projectId 项目id
     * @return 详设文档对象
     */
    BaseResult<ProjectFlowDocumentVO> queryTechnicalDocument(Long projectId);

    /**
     * 查询测试文档
     * @param projectId 项目id
     * @return 测试文档对象
     */
    BaseResult<TestBillDocumentVO> queryTestBillDocument(Long projectId);

    /**
     * 查询文档
     * @param projectId 项目id
     * @return 测试文档对象
     */
    BaseResult<ProjectDocumentVO> queryDocument(Long projectId, Integer type);

    /**
     * 保存文档
     */
    BaseResult<Void> saveDocument(ProjectDocumentReq req);

    /**
     * 查询项目下所有文档列表
     * @param projectId 项目id
     * @return 项目下所有文档
     */
    BaseResult<List<ProjectDocumentVO>> listDocuments(Long projectId);

    /**
     * 发布前检查
     * @param checkReq checkReq
     * @return 异常信息
     */
    BaseResult<List<String>> checkDocBeforeRelease(ProjectDocumentCheckReq checkReq);


}
