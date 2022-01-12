package com.timevale.forward.service.integration.superset.client;

import com.timevale.forward.service.integration.superset.model.base.DistributePageQueryVO;
import com.timevale.forward.service.integration.superset.model.base.PageResult;

import java.util.List;

/**
 * @author by YangXu
 * @date 2022/01/11 18:00
 */
public interface BaseDistributeClient<T> {

    /**
     * superset 查询工具
     *
     * @param param 查询条件 json
     * @return 返回结果  json
     */
    List<T> doGet(DistributePageQueryVO param);

    /**
     * 获取查询数量
     *
     * @param param 查询条件 json
     * @return 总数
     */
    Integer doCount(DistributePageQueryVO param);

    /**
     * 查询分页
     *
     * @param param 查询条件
     * @param countParam 查询条件
     * @return 数据
     */
    PageResult<T> doPage(DistributePageQueryVO param, DistributePageQueryVO countParam);
}
