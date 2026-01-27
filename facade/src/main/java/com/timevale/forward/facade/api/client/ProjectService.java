package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.query.ProjectBizDemandQueryList;
import com.timevale.forward.facade.api.query.ProjectLinkProductDemandQueryList;
import com.timevale.forward.facade.api.query.ProjectPageQuery;
import com.timevale.forward.facade.api.query.ProjectProductDemandQueryList;
import com.timevale.forward.facade.api.query.ProjectQueryList;
import com.timevale.forward.facade.api.request.ProjectAddReq;
import com.timevale.forward.facade.api.request.ProjectAppendChildReq;
import com.timevale.forward.facade.api.request.ProjectChildListReq;
import com.timevale.forward.facade.api.request.ProjectConclusionReq;
import com.timevale.forward.facade.api.request.ProjectDateModifyReq;
import com.timevale.forward.facade.api.request.ProjectDeleteChildReq;
import com.timevale.forward.facade.api.request.ProjectInnerAddReq;
import com.timevale.forward.facade.api.request.ProjectInnerCompleteReq;
import com.timevale.forward.facade.api.request.ProjectModifyReq;
import com.timevale.forward.facade.api.request.ProjectNodeAddReq;
import com.timevale.forward.facade.api.request.ProjectProductDemandLinkReq;
import com.timevale.forward.facade.api.request.ProjectSimpleModifyReq;
import com.timevale.forward.facade.api.request.ProjectStageChangeReq;
import com.timevale.forward.facade.api.request.ProjectUnWriteReasonModifyReq;
import com.timevale.forward.facade.api.request.ProjectUpdateStatusReq;
import com.timevale.forward.facade.api.result.BizDemandVO;
import com.timevale.forward.facade.api.result.ConclusionFormVO;
import com.timevale.forward.facade.api.result.ModifyProjectCheckVO;
import com.timevale.forward.facade.api.result.ProductDemandStatusVO;
import com.timevale.forward.facade.api.result.ProductDemandVO;
import com.timevale.forward.facade.api.result.ProjectBaseVO;
import com.timevale.forward.facade.api.result.ProjectDetailVO;
import com.timevale.forward.facade.api.result.ProjectInnerDetailVO;
import com.timevale.forward.facade.api.result.ProjectProductLineVO;
import com.timevale.forward.facade.api.result.ProjectSimpleVO;
import com.timevale.forward.facade.api.result.ProjectStageConfigVO;
import com.timevale.forward.facade.api.result.ProjectTabCountVO;
import com.timevale.forward.facade.api.result.ProjectTreeVO;
import com.timevale.forward.facade.api.result.ProjectVO;
import com.timevale.forward.facade.api.result.QueryResultVO;
import com.timevale.mandarin.common.annotation.RestClient;
import com.timevale.mandarin.common.result.PageQueryResult;

import java.util.List;

/**
 * @author: xingyun
 * @create: 2021-12-13 13:44
 **/
@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface ProjectService {
    BaseResult<Void> sendWorkHourNotice(boolean isExpedite, Long projectId);

    /**
     * 查列表
     *
     * @param projectQueryList 项目信息
     * @return 列表
     */
    BaseResult<QueryResultVO<ProjectVO>> list(ProjectQueryList projectQueryList);

    /**
     * 查列表
     *
     * @param projectQueryList 项目信息
     * @return 列表
     */
    BaseResult<List<ProjectVO>> simpleList(ProjectQueryList projectQueryList);

    /**
     * 修改状态
     *
     * @param req 暂停/作废更新
     * @return 数量
     */
    BaseResult<Boolean> updateStatus(ProjectUpdateStatusReq req);

    /**
     * 开启项目
     *
     * @param projectId  项目id
     * @param enableTask 是否启用任务
     * @return 数量
     */
    BaseResult<Boolean> enable(Long projectId, Boolean enableTask);

    /**
     * 新增
     *
     * @param projectAddReq 项目信息
     * @return 数量
     */
    BaseResult<Long> add(ProjectAddReq projectAddReq);

    /**
     * 内部项目新增
     *
     * @param projectInnerAddReq 项目内部添加请求
     * @return 是否成功
     */
    BaseResult<Boolean> innerAdd(ProjectInnerAddReq projectInnerAddReq);

    /**
     * 修改
     *
     * @param projectModifyReq 项目信息
     * @return 数量
     */
    BaseResult<Boolean> modify(ProjectModifyReq projectModifyReq);

    /**
     * 简单修改
     *
     * @param projectSimpleModifyReq 项目信息
     * @return 是否成功
     */
    BaseResult<Boolean> simpleModify(ProjectSimpleModifyReq projectSimpleModifyReq);

    /**
     * 查询子项目列表
     *
     * @param projectChildListReq 分页查询参数
     * @return 子项目列表
     */
    BaseResult<PageQueryResult<ProjectVO>> listChildren(ProjectChildListReq projectChildListReq);

    /**
     * 根据项目id获取项目树
     *
     * @param projectId 项目id
     * @return 项目树结构内容
     */
    BaseResult<ProjectTreeVO> getTree(Long projectId);

    /**
     * 添加子项目列表
     */
    BaseResult<Void> appendChildren(ProjectAppendChildReq projectAppendChildReq);

    /**
     * 删除子项目
     */
    BaseResult<Void> deleteChild(ProjectDeleteChildReq projectDeleteChildReq);

    /**
     * 查询项目标签页的todo数量列表
     *
     * @param projectId 项目id
     */
    BaseResult<ProjectTabCountVO> countTabTodos(Long projectId);

    /**
     * 查看
     *
     * @param projectId 项目信息
     * @return 详情信息
     */
    BaseResult<ProjectDetailVO> get(Long projectId);

    /**
     * 查看内部项目
     *
     * @param projectId 项目信息
     * @return 详情信息
     */
    BaseResult<ProjectInnerDetailVO> getInner(Long projectId);


    /**
     * 查询满足条件的产品需求列表
     *
     * @param query 项目信息
     * @return 列表
     */
    BaseResult<PageQueryResult<ProductDemandVO>> matchProductDemandList(ProjectLinkProductDemandQueryList query);


    /**
     * 关联or取消关联
     *
     * @param productDemandLinkReq 产品需求
     */
    BaseResult<ProductDemandStatusVO> linkOrUnLinkProductDemand(ProjectProductDemandLinkReq productDemandLinkReq);


    /**
     * @param query 查询条件
     * @return 项目产品需求清单
     */
    BaseResult<PageQueryResult<ProductDemandVO>> linkProductDemandList(ProjectProductDemandQueryList query);

    /**
     * 查询关联到这个项目的业务需求列表
     *
     * @param projectBizDemandQueryList 查询条件 项目id
     */
    BaseResult<PageQueryResult<BizDemandVO>> linkBizDemandList(ProjectBizDemandQueryList projectBizDemandQueryList);

    /**
     * 产品线Id
     *
     * @param productLineId 产品id
     * @return 项目简单VO列表
     */
    BaseResult<List<ProjectBaseVO>> getProjectByProductLine(Long productLineId);

    /**
     * 修改立项日期
     *
     * @param projectDateModifyReq 修改立项日期
     * @return Boolean
     */
    BaseResult<Boolean> modifyProjectDate(ProjectDateModifyReq projectDateModifyReq);

    /**
     * 添加阶段
     */
    BaseResult<Void> addStage(ProjectStageChangeReq addStageReq);

    /**
     * 添加阶段
     */
    BaseResult<Void> deleteStage(ProjectStageChangeReq deleteStageReq);

    /**
     * 查列表
     *
     * @param name name
     * @return 列表
     */
    BaseResult<List<ProjectProductLineVO>> getByName(String name);

    /**
     * 项目文档未填写原因
     *
     * @param reasonModifyReq 项目文档未填写原因
     * @return Boolean
     */
    BaseResult<Boolean> modifyUnWriteReason(ProjectUnWriteReasonModifyReq reasonModifyReq);

    /**
     * 内部项目完成
     *
     * @param req 请求
     * @return {@link BaseResult}<{@link Boolean}>
     */
    BaseResult<Boolean> innerComplete(ProjectInnerCompleteReq req);

    /**
     * 结项
     *
     * @param req 请求
     * @return {@link BaseResult}<{@link Boolean}>
     */
    BaseResult<Boolean> conclusion(ProjectConclusionReq req);


    /**
     * 结项表单
     *
     * @param req 请求
     * @return {@link BaseResult}<{@link ConclusionFormVO}>
     */
    BaseResult<ConclusionFormVO> conclusionForm(ProjectConclusionReq req);

     /**
     * 项目列表
     *
     * @param query 查询条件
     * @return {@link BaseResult}<{@link PageQueryResult<ProjectSimpleVO>}>
     */
    BaseResult<PageQueryResult<ProjectSimpleVO>> pageAll(ProjectPageQuery query);

    /**
     * 对完成项目做检查
     * @param projectModifyReq 项目更新亲贵
     * @return 项目检查结果集合
     */
    BaseResult<List<ModifyProjectCheckVO>> checkForFinishProject(ProjectModifyReq projectModifyReq);

    /**
     * 对完成项目做检查
     * @param projectNodeAddReq 项目发布完成
     * @return 项目检查结果集合
     */
    BaseResult<Boolean> autoCompleteTime(ProjectNodeAddReq projectNodeAddReq);

    BaseResult<List<ProjectStageConfigVO.Stage>> queryStageConfig(Integer kind, Integer type, Integer version);

    BaseResult<Integer> getNodeVersion(String createDate);
}
