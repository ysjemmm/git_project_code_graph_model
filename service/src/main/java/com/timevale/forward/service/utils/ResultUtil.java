package com.timevale.forward.service.utils;

import com.github.pagehelper.PageInfo;
import com.timevale.forward.facade.api.result.QueryResultVO;
import com.timevale.mandarin.common.result.ListResult;
import com.timevale.mandarin.common.result.PageQueryResult;
import com.timevale.mandarin.common.result.QueryResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author yuankai
 * @date 2021/10/25 20:20
 */
public class ResultUtil {

    public static void fillPageInfo(PageQueryResult<?> pageQueryResult, PageInfo<?> pageInfo) {
        pageQueryResult.setTotalItems(Math.toIntExact(pageInfo.getTotal()));
        pageQueryResult.setCurrentPage(pageInfo.getPageNum());
        pageQueryResult.setTotalPages(pageInfo.getPages());
        pageQueryResult.setItemsPerPage(pageInfo.getPageSize());
    }

    public static <T> PageQueryResult<T> pageSuccess(PageInfo<T> data) {
        PageQueryResult<T> result = new PageQueryResult<>(true);
        result.setResultList(data.getList());
        result.setTotalItems(Math.toIntExact(data.getTotal()));
        result.setCurrentPage(data.getPageNum());
        result.setTotalPages(data.getPages());
        result.setItemsPerPage(data.getPageSize());
        return result;
    }

    public static <T> PageQueryResult<T> pageEmpty() {
        PageQueryResult<T> result = new PageQueryResult<>(true);
        result.setResultList(Collections.emptyList());
        return result;
    }


    public static <T> QueryResult<T> querySuccess(T data) {
        QueryResult<T> result = new QueryResult<>(true);
        result.setResultObject(data);
        return result;
    }

    public static <T> QueryResult<T> queryFail(String msg) {
        QueryResult<T> result = new QueryResult<>(false);
        result.setMessage(msg);
        return result;
    }

    public static <T> ListResult<T> success(List<T> data) {
        ListResult<T> result = new ListResult<>(true);
        result.setResultList(data);
        return result;
    }

    public static <T> QueryResultVO<T> queryResultEmpty(){
        QueryResultVO<T> empty = new QueryResultVO<>();
        empty.setAnalyseVOList(new ArrayList<>());
        empty.setPageQueryResult(ResultUtil.pageEmpty());
        return empty;
    }
}
