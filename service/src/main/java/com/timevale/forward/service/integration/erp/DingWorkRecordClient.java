package com.timevale.forward.service.integration.erp;

import com.timevale.forward.service.integration.erp.model.CreateTodoTaskMsg;
import com.timevale.forward.service.integration.erp.model.DeleteTodoTaskMsg;
import com.timevale.forward.service.integration.erp.model.UpdateTodoTaskMsg;

/**
 * 钉钉待办消息
 *
 * @author xingyun
 * @date 2022/01/25 10:51
 */
public interface DingWorkRecordClient {
    /**
     * 新增待办
     * @param createTodoTaskMsg 待办请求类型
     * @return 待办id
     */
    String addTask(CreateTodoTaskMsg createTodoTaskMsg);

    /**
     * 更新待办
     * @param updateTodoTaskMsg 待办请求类型
     * @return 待办返回信息
     */
    void updateTask(UpdateTodoTaskMsg updateTodoTaskMsg);

    /**
     * 删除待办
     * @param deleteTodoTaskMsg 待办请求类型
     * @return 待办返回信息
     */
    void deleteTask(DeleteTodoTaskMsg deleteTodoTaskMsg);
}
