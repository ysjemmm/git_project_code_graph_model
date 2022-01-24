package com.timevale.forward.service.component;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.result.HomePageTodoCardVO;
import com.timevale.mandarin.common.annotation.RestClient;

/**
 * @author by YangXu
 * @date 2022/01/24 09:45
 */
public interface HomePageTodoCardComponent {

    /**
     * 待办卡片
     *
     * @return 列表
     */
    BaseResult<HomePageTodoCardVO> getTodoCard();
}
