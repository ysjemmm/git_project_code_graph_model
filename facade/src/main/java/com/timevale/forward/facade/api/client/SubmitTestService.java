package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.request.FileAddReq;
import com.timevale.forward.facade.api.result.SubmitTestVO;
import com.timevale.mandarin.common.annotation.RestClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * @Date 2022/1/21 13:55
 * @Author 望轩
 */
@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface SubmitTestService {
    /**
     * 创建提测单
     *
     * @param projectId 项目id
     * @return 返回结果（项目是否有提测单和提测计划时间以及提测人）
     */
    BaseResult<Map<String, Object>> addTestBill(Long projectId);


    /**
     * 提交提测单
     *
     * @param projectId 项目id
     * @param testMan   测试人
     * @param testManId 测试人花名拼音
     * @return boolean 返回结果
     */
    BaseResult<Boolean> submitTestBill(Long projectId, String testMan, String testManId);

    /**
     * 提测单详情
     *
     * @param projectId 项目id
     * @param type      文件类型
     * @return 返回提测单详情信息
     */
    BaseResult<SubmitTestVO> getTestBill(Long projectId, Integer type);

    /**
     * 提交提测单
     *
     * @param projectId 项目id
     * @param caseUrl   冒烟用例连接地址
     * @param list      上传的附件的集合信息
     * @return boolean 返回结果
     */
    BaseResult<Boolean> submitSmokeTesting(Long projectId, String caseUrl, List<FileAddReq> list);

    /**
     * 修改测试人
     *
     * @param projectId 项目id
     * @param testMan   测试人
     * @param testManId 测试人花名拼音
     * @return boolean 返回结果
     */
    BaseResult<Boolean> modifyTestMan(Long projectId, String testMan, String testManId);

    /**
     * 自测通过
     *
     * @param desc      影响范围与变更SQL
     * @param progress  用例执行情况:0冒烟用例执行通过,1冒烟用例部分执行,2冒烟用例未执行
     * @param list      上传的附件的集合信息
     * @param attacheId 附件所属的提测单对应的项目id
     * @return boolean 返回结果
     */
    BaseResult<Boolean> selfTestPass(String desc, Integer progress, List<FileAddReq> list, Long attacheId);

    /**
     * 提测通过
     *
     * @param projectId  项目id
     * @param passRate   提测通过率
     * @param actualDate 实际提测时间
     * @return boolean 返回结果
     */
    BaseResult<Boolean> submitTestPass(Long projectId, Integer passRate, LocalDateTime actualDate);

    /**
     * 提测打回
     *
     * @param projectId 项目id
     * @param reason    打回原因
     * @return boolean 返回结果
     */
    BaseResult<Boolean> submitTestBack(Long projectId, String reason);
}
































