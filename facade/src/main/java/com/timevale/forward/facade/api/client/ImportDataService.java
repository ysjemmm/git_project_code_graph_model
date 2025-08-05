package com.timevale.forward.facade.api.client;

import com.timevale.forward.facade.api.MagicValue;
import com.timevale.mandarin.common.annotation.RestClient;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;

/**
 * @auther: yuhua
 * @date: 2025/7/3 17:50
 * @description: 历史数据导入
 */
@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface ImportDataService {

    /**
     * 下载导入模版
     * @param type 1:项目 2:需求 3:任务
     * @param response
     */
    void downloadTemplate(Integer type, HttpServletResponse response);

    /**
     * 导入项目数据
     * @param file
     */
    void importProjectData(MultipartFile file, HttpServletResponse response);

    /**
     * 导入需求数据
     * @param file
     */
    void importDemandData(MultipartFile file, HttpServletResponse response);

    /**
     * 导入任务数据
     * @param file
     */
    void importTaskData(MultipartFile file, HttpServletResponse response);
}
