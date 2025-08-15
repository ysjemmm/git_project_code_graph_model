package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.query.ViewsGroupQuery;
import com.timevale.forward.facade.api.query.ViewsQueryList;
import com.timevale.forward.facade.api.request.*;
import com.timevale.forward.facade.api.result.ViewsGroupVO;
import com.timevale.forward.facade.api.result.ViewsUserListVO;
import com.timevale.mandarin.common.annotation.RestClient;

import java.util.List;

/**
 * 用户视图服务接口
 * @author qiyuan
 * @date 2025/08/14 15:00
 */
@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface ViewsService {

    BaseResult<List<ViewsUserListVO>> list(ViewsQueryList viewsQueryList);

    /**
     * 保存视图的分组和筛选条件
     * @param viewsSaveReq 保存请求
     * @return 是否成功
     */
    BaseResult<Boolean> save(ViewsSaveReq viewsSaveReq);

    /**
     * 新增视图
     * @param viewsAddReq 新增请求
     * @return 是否成功
     */
    BaseResult<Boolean> add(ViewsAddReq viewsAddReq);

    /**
     * 复制视图
     * @param viewsModifyReq 修改请求
     * @return 是否成功
     */
    BaseResult<Boolean> copy(ViewsModifyReq viewsModifyReq);

    /**
     * 修改视图
     * @param viewsModifyReq 修改请求
     * @return 是否成功
     */
    BaseResult<Boolean> modify(ViewsModifyReq viewsModifyReq);

    /**
     * 启用视图
     * @param viewsReq 修改请求
     * @return 是否成功
     */
    BaseResult<Boolean> hidden(ViewsReq viewsReq);

    /**
     * 删除视图
     * @param viewsReq 删除请求
     * @return 是否成功
     */
    BaseResult<Boolean> delete(ViewsReq viewsReq);

    /**
     * 移除视图
     * @param viewsReq 移除请求
     * @return 是否成功
     */
    BaseResult<Boolean> remove(ViewsReq viewsReq);

    /**
     * 分享视图
     * @param viewsShareReq 分享请求
     * @return 是否成功
     */
    BaseResult<Boolean> share(ViewsShareReq viewsShareReq);

    /**
     * 分组条件
     * @param viewsGroupQuery 分组条件请求
     * @return 是否成功
     */
    BaseResult<List<ViewsGroupVO>> groupConditions(ViewsGroupQuery viewsGroupQuery);


    /**
     * 置顶视图
     * @param viewsReq 置顶请求
     * @return 是否成功
     */
    BaseResult<Boolean> top(ViewsReq viewsReq);
} 