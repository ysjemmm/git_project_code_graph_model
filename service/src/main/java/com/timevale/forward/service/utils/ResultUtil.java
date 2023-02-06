package com.timevale.forward.service.utils;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageInfo;
import com.timevale.forward.facade.api.result.QueryResultVO;
import com.timevale.mandarin.common.result.ListResult;
import com.timevale.mandarin.common.result.PageQueryResult;

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

    public static <T, R> void fillPageInfo(PageQueryResult<R> pageQueryResult, Page<T> page) {
        pageQueryResult.setTotalItems(Math.toIntExact(page.getTotal()));
        pageQueryResult.setCurrentPage(page.getPageNum());
        pageQueryResult.setTotalPages(page.getPages());
        pageQueryResult.setItemsPerPage(page.getPageSize());
    }

    public static <T> PageQueryResult<T> pageEmpty() {
        PageQueryResult<T> result = new PageQueryResult<>(true);
        result.setResultList(Collections.emptyList());
        return result;
    }


    public static <T> ListResult<T> success(List<T> data) {
        ListResult<T> result = new ListResult<>(true);
        result.setResultList(data);
        return result;
    }

    public static <T> QueryResultVO<T> queryResultEmpty() {
        QueryResultVO<T> empty = new QueryResultVO<>();
        empty.setAnalyseVOList(new ArrayList<>());
        empty.setPageQueryResult(ResultUtil.pageEmpty());
        return empty;
    }
}
