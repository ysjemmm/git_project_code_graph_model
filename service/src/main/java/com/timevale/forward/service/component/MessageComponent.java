package com.timevale.forward.service.component;

import java.util.Date;
import java.util.List;

/**
 * @author by YangXu
 * @date 2021/12/27 16:58
 */
public interface MessageComponent {

    /**
     * 业务需求已经被接收通知
     *
     * @param operator            操作人
     * @param receiver            接收人
     * @param name                需求主题
     * @param planReleaseDate 计划发布日期
     */
    void bizDemandReceivedMsg(String operator, String receiver, String name, String planReleaseDate);

    /**
     * 业务需求被驳回通知
     *
     * @param operator 操作人
     * @param receiver 接收人
     * @param name     需求主题
     * @param rejectReason   驳回原因原因
     */
    void bizDemandRejectMsg(String operator, String receiver, String name, String rejectReason);

    /**
     * 业务需求解决状态变为已列入项目、项目进行中、已完成上线时通知
     *
     * @param receiver       接收人
     * @param name           需求主题
     * @param status         状态
     * @param projectEndDate 项目发布时间
     */
    void bizDemandStatusChangeMsg(String receiver, String name, String status, Date projectEndDate);

    /**
     * 业务需求请求接收通知
     *
     * @param operator 操作人
     * @param receiver 接收人
     * @param name     需求主题
     */
    void bizDemandToReceiveMsg(String operator, String receiver, String name);

    /**
     * 业务需求作废通知
     *
     * @param operator 操作人
     * @param receiver 接收人
     * @param name     需求主题
     */
    void bizDemandInvalidMsg(String operator, String receiver, String name);

    /**
     * 评论通知
     *
     * @param operator  操作人
     * @param receivers 接收人
     * @param type      主体类型
     * @param name      业务需求主题/产品需求主题/项目名称
     * @param content   评论内容
     */
    void commentMsg(String operator, List<String> receivers, String type, String name, String content);
}
