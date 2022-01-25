package com.timevale.forward.service.component;

import com.timevale.forward.dal.dto.HomePageProjectOnlineLatelyDTO;

import java.util.List;

/**
 * @author by YangXu
 * @date 2022/01/24 09:46
 */
public interface HomePageProjectOnlineLatelyComponent {
    /**
     * 近三周上线项目
     *
     * @param pageNum 查询页数
     * @return 项目列表
     */
    List<HomePageProjectOnlineLatelyDTO> getProjectOnlineLately(Integer pageNum);
}
