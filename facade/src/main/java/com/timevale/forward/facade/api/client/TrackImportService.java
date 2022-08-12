package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.query.TaskImportLogQueryList;
import com.timevale.forward.facade.api.request.TrackImportReq;
import com.timevale.forward.facade.api.result.*;
import com.timevale.mandarin.common.annotation.RestClient;
import com.timevale.mandarin.common.result.PageQueryResult;


/**
 * @author by YangXu
 * @date 2022/08/08 15:59
 */
@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface TrackImportService {

    /**
     * 导入埋点
     *
     * @param trackImportReq 导入埋点请求
     * @return {@link BaseResult}<{@link Boolean}>
     */
    BaseResult<Boolean> importEvent(TrackImportReq trackImportReq);

    /**
     * 取消导入
     *
     * @return {@link BaseResult}<{@link Boolean}>
     */
    BaseResult<Boolean> cancel();

    /**
     * 查询导入进度
     *
     * @return {@link BaseResult}<{@link TrackImportProgressVO}>
     */
    BaseResult<TrackImportProgressVO> progress();

    /**
     * 模板
     *
     * @return {@link BaseResult}<{@link Boolean}>
     */
    BaseResult<TrackImportLogFileVO> template();

    /**
     * 导入记录
     *
     * @param query 查询
     * @return {@link BaseResult}<{@link PageQueryResult}<{@link TrackImportLogVO}>>
     */
    BaseResult<PageQueryResult<TrackImportLogListVO>> log(TaskImportLogQueryList query);
}
