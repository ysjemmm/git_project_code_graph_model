package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.query.ProjectNodeRecordQuery;
import com.timevale.forward.facade.api.result.ProjectNodeRecordCompareVO;
import com.timevale.forward.facade.api.result.ProjectNodeRecordVO;
import com.timevale.mandarin.common.annotation.RestClient;

import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface ProjectNodeRecordService {


    /**
     *
     * @param projectId projectId
     * @return 项目节点记录列表
     */
    BaseResult<List<ProjectNodeRecordVO>> list(Long projectId);

    /**
     *
     * @param projectNodeRecordQuery projectNodeRecordQuery
     * @return 项目节点记录列表
     */
    BaseResult<List<ProjectNodeRecordCompareVO>> list(ProjectNodeRecordQuery projectNodeRecordQuery);

}
