package com.timevale.forward.service.utils;

import com.timevale.forward.dal.entity.ManDayDO;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.service.utils.date.DateUtil;
import com.timevale.mandarin.common.query.QueryBase;
import org.apache.commons.collections.CollectionUtils;

import java.util.List;
import java.util.function.Function;

/**
 * @author xiaoyun
 * @date 2022/8/29/029 16:22
 */
public class PageUtil {

    public static void page(Function<QueryBase, List> function) {
        QueryBase queryBase = new QueryBase();

        while (true) {
            List list = function.apply(queryBase);

            if (list.size() < queryBase.getPageSize()) {
                return;
            }

            queryBase.setPageNum(queryBase.getPageNum() + 1);
        }
    }
}
