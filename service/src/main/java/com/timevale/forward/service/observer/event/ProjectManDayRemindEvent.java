package com.timevale.forward.service.observer.event;

import com.google.common.collect.Lists;
import com.timevale.forward.model.enums.MessageTitleEnum;
import com.timevale.forward.model.enums.TabEnum;
import com.timevale.forward.service.integration.erp.model.MarkdownMsg;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author xiaoyun
 * @date 2022/8/29/029 16:37
 */
@Slf4j
public class ProjectManDayRemindEvent extends MessageEvent{
    private final Long bizDemandId;
    private final String receiver;
    private final String name;

    private static final String BIZ_DEMAND_TO_RECEIVE_MSG = "### %s  \n  **%s** 上一周的人天数据未填报，请及时维护   \n\n  ***  \n  [查看详情](%s)";

    public ProjectManDayRemindEvent(Object source, Long bizDemandId, String receiver, String name) {
        super(source);
        this.bizDemandId = bizDemandId;
        this.receiver = receiver;
        this.name = name;
    }

    @Override
    public void run() {
        List<String> receivers = Lists.newArrayList(receiver);
        String title = MessageTitleEnum.PROJECT_MAN_DAY_REMIND.getText();
        String singleUrl = domainName + String.format(PARAM, TabEnum.PROJECT_MANAGEMENT.getText(), bizDemandId);
        String markdown = String.format(BIZ_DEMAND_TO_RECEIVE_MSG, title, name, singleUrl);

        MarkdownMsg markdownMsg = MarkdownMsg.builder()
                .title(title)
                .content(markdown)
                .receivers(receivers)
                .build();

        log.info("PROJECT_MAN_DAY_REMIND, title:{}, pm:{}", name, receiver);

        erpMessageClient.sendMarkdownMsg(markdownMsg);
    }
}
