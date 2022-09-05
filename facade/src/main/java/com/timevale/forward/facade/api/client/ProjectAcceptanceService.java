package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.query.ProjectAcceptanceQueryList;
import com.timevale.forward.facade.api.request.ProjectAcceptanceAddReq;
import com.timevale.forward.facade.api.request.ProjectAcceptanceModifyReq;
import com.timevale.forward.facade.api.result.ProjectAcceptanceVO;
import com.timevale.mandarin.common.annotation.RestClient;
import com.timevale.mandarin.common.result.PageQueryResult;

import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface ProjectAcceptanceService {


    /**
     *
     * @param projectId projectId
     * @return List
     */
    BaseResult<List<ProjectAcceptanceVO>> acceptList(Long projectId);

    /**
     * 项目验收历史
     * @param query query
     * @return PageQueryResult
     */
    BaseResult<PageQueryResult<ProjectAcceptanceVO>> history(ProjectAcceptanceQueryList query);

    /**
     * 项目验收发起
     *
     * @param req req
     * @return Boolean
     */
    BaseResult<Boolean> add(ProjectAcceptanceAddReq req);

    /**
     * 项目验收通过
     * @param req req
     * @return Boolean
     */

    BaseResult<Boolean> accept(ProjectAcceptanceModifyReq req);

    /**
     * 项目验收不通过
     * @param req req
     * @return Boolean
     */
    BaseResult<Boolean> unAccept(ProjectAcceptanceModifyReq req);

    /**
     *
     * @param id id
     * @return Boolean
     */
    BaseResult<Boolean> remind(Long id);

    /**
     *
     * @param id id
     * @return Boolean
     */
    BaseResult<Boolean> revoke(Long id);


}
