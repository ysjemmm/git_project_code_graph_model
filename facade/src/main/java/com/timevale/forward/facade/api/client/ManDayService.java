package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.query.ManDayQueryList;
import com.timevale.forward.facade.api.request.ManDayModifyReq;
import com.timevale.forward.facade.api.result.ManDayListVO;
import com.timevale.forward.facade.api.result.ProjectManDayVO;
import com.timevale.mandarin.common.annotation.RestClient;

import java.util.List;

/**
 * 人天RPC接口
 *
 * @author jingchun
 * create on 2022/6/20
 */
@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface ManDayService {

    /**
     * 使用时间起止查询人天列表
     */
    BaseResult<List<ManDayListVO>> list(ManDayQueryList manDayQueryList);

    /**
     * 使用项目id查询人天列表
     */
    BaseResult<List<ProjectManDayVO>> listProjectManDays(Long projectId);

    /**
     * 新增、修改、删除人天记录
     */
    BaseResult<Boolean> modify(ManDayModifyReq manDayModifyReq);

    /**
     * 查询项目人天时间周期列表
     */
    BaseResult<List<String>> queryManDayDateRanges(Long projectId);

}
