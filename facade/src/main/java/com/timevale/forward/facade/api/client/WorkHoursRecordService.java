package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.query.OverviewWorkHoursQueryList;
import com.timevale.forward.facade.api.query.TaskExecutorWorkHoursQueryList;
import com.timevale.forward.facade.api.query.WorkHoursRecordQueryList;
import com.timevale.forward.facade.api.request.WorkHoursRecordAddReq;
import com.timevale.forward.facade.api.request.WorkHoursRecordBatchAddReq;
import com.timevale.forward.facade.api.request.WorkHoursRecordModifyReq;
import com.timevale.forward.facade.api.request.WorkHoursRecordQueryReq;
import com.timevale.forward.facade.api.result.WorkHoursOverviewVO;
import com.timevale.forward.facade.api.result.RegisterWorkHoursTaskVO;
import com.timevale.forward.facade.api.result.WorkHoursProgressVO;
import com.timevale.forward.facade.api.result.WorkHoursRecordVO;
import com.timevale.forward.facade.api.result.WorkHoursRemainVO;
import com.timevale.forward.facade.api.result.WorkbenchesWorkHoursVO;
import com.timevale.mandarin.common.annotation.RestClient;
import com.timevale.mandarin.common.result.PageQueryResult;

import java.util.List;

/**
 * @auther: yuhua
 * @date: 2025/7/3 17:50
 * @description: 工时记录
 */
@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface WorkHoursRecordService {

    /**
     * 查列表
     *
     * @param workHoursRecordQueryList 工时记录信息
     * @return 列表
     */
    BaseResult<PageQueryResult<WorkHoursRecordVO>> list(WorkHoursRecordQueryList workHoursRecordQueryList);

    /**
     * 新增
     *
     * @param workTimeRecordAddReq 工时信息
     * @return 数量
     */
    BaseResult<Long> add(WorkHoursRecordAddReq workTimeRecordAddReq);

    /**
     * 删除
     *
     * @param workHoursRecordId workHoursRecordId
     * @return 详情信息
     */
    BaseResult<Boolean> delete(Long workHoursRecordId);

    /**
     * 修改
     *
     * @param workHoursRecordModifyReq 工时信息
     * @return 数量
     */
    BaseResult<Boolean> modify(WorkHoursRecordModifyReq workHoursRecordModifyReq);

    /**
     * 查看
     *
     * @param workHoursRecordId 工时信息
     * @return 详情信息
     */
    BaseResult<WorkHoursRecordVO> get(Long workHoursRecordId);

    /**
     * 批量新增
     *
     * @param workHoursRecordBatchAddReq 工时信息
     * @return 数量
     */
    BaseResult<Boolean> batchAdd(WorkHoursRecordBatchAddReq workHoursRecordBatchAddReq);

    BaseResult<WorkHoursRemainVO> remainInfo(WorkHoursRecordQueryReq workHoursRecordQueryReq);

    BaseResult<WorkHoursProgressVO> progressInfo(WorkHoursRecordQueryReq workHoursRecordQueryReq);

    BaseResult<List<RegisterWorkHoursTaskVO>> waitRegisterTaskList(String dateStr);

    BaseResult<PageQueryResult<WorkbenchesWorkHoursVO>> workbenches(TaskExecutorWorkHoursQueryList query);

    BaseResult<PageQueryResult<WorkHoursOverviewVO>> overview(OverviewWorkHoursQueryList query);
}
