package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.request.TestBillAddReq;
import com.timevale.forward.facade.api.request.TestBillModifyReq;
import com.timevale.forward.facade.api.result.CreateTestBillVO;
import com.timevale.forward.facade.api.result.TestBillVO;
import com.timevale.mandarin.common.annotation.RestClient;

import java.util.Map;

/**
 * @Date 2022/1/21 13:55
 * @Author 望轩
 */
@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface TestBillService {
    /**
     * 创建提测单
     *
     * @param projectId 项目id
     * @return 返回结果（项目是否有提测单和提测计划时间以及提测人）
     */
    BaseResult<CreateTestBillVO> addTestBill(Long projectId);


    /**
     * 提交提测单
     *
     * @param testBillAddReq 提测单详情
     * @return boolean 返回结果
     */
    BaseResult<Boolean> submitTestBill(TestBillAddReq testBillAddReq);

    /**
     * 提测单详情
     *
     * @param projectId 项目id
     * @return 返回提测单详情信息
     */
    BaseResult<TestBillVO> getTestBill(Long projectId);

    /**
     * 提交提测单
     *
     * @param testBillModifyReq 提测单修改信息
     * @return boolean 返回结果
     */
    BaseResult<Boolean> submitSmokeTesting(TestBillModifyReq testBillModifyReq);

    /**
     * 修改测试人
     *
     * @param testBillModifyReq 提测单修改信息
     * @return String 返回结果
     */
    BaseResult<String> modifyTestMan(TestBillModifyReq testBillModifyReq);

    /**
     * 自测通过
     *
     * @param testBillModifyReq 提测单修改信息
     * @return boolean 返回结果
     */
    BaseResult<Boolean> selfTestPass(TestBillModifyReq testBillModifyReq);

    /**
     * 提测通过
     *
     * @param testBillModifyReq 提测修改信息
     * @return boolean 返回结果
     */
    BaseResult<Boolean> submitTestPass(TestBillModifyReq testBillModifyReq);

    /**
     * 提测打回
     *
     * @param testBillModifyReq 提测单修改信息
     * @return boolean 返回结果
     */
    BaseResult<Boolean> submitTestBack(TestBillModifyReq testBillModifyReq);
}
































