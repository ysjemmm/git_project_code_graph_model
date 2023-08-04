package com.timevale.forward.service.integration.erp;

import com.timevale.erp.message.service.result.DingTodoTaskResponseBody;
import com.timevale.forward.service.integration.erp.model.*;

import java.util.List;
import java.util.Map;

/**
 * 钉钉待办消息
 *
 * @author xingyun
 * @date 2022/01/25 10:51
 */
public interface DingWorkRecordClient {

    /**
     * 新版本新增待办
     * @param req 新增待办参数
     * @return 新增待办id
     */
    String addTask(AddDingTodoReq req);

    /**
     * 完成钉钉待办
     * @param recordId 待办id
     * @param userId 完成人id
     */
    void finishTask(String recordId, String userId);

    /**
     * 新增待办
     * @param createTodoTaskMsg 待办请求类型
     * @return 待办id
     */
    String addTask(CreateTodoTaskMsg createTodoTaskMsg);

    /**
     * 更新待办
     * @param updateTodoTaskMsg 待办请求类型
     */
    void updateTask(UpdateTodoTaskMsg updateTodoTaskMsg);

    /**
     * 删除待办
     * @param deleteTodoTaskMsg 待办请求类型
     */
    void deleteTask(DeleteTodoTaskMsg deleteTodoTaskMsg);

    /**
     * 获取待办详情
     *
     * @param getTodoTaskMsg 待办请求类型
     * @return 待办详情
     */
    DingTodoTaskResponseBody getTask(GetTodoTaskMsg getTodoTaskMsg);

    /**
     * 批量获取待办详情
     *
     * @param getTodoTaskMsgList 待办请求类型-列表
     * @return 待办详情Map（待办id，待办详情）
     */
    Map<String, DingTodoTaskResponseBody> batchGetTask(List<GetTodoTaskMsg> getTodoTaskMsgList);

}
