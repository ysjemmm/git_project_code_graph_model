package com.timevale.forward.service.observer.event;

import com.google.common.collect.Lists;
import com.timevale.forward.model.enums.MessageTitleEnum;
import com.timevale.forward.model.enums.TabEnum;
import com.timevale.forward.service.integration.erp.model.MarkdownMsg;

import java.util.List;

/**
 * @author by YangXu
 * @date 2022/01/21 15:24
 */
public class BizDemandPlanReleaseDateMsgEvent extends MessageEvent {

    private final Long bizDemandId;
    private final String receiver;
    private final String name;
    private final String status;
    private final String planReleaseDate;

    public BizDemandPlanReleaseDateMsgEvent(Object source, Long bizDemandId, String receiver, String name, String status, String planReleaseDate) {
        super(source);
        this.bizDemandId = bizDemandId;
        this.receiver = receiver;
        this.name = name;
        this.status = status;
        this.planReleaseDate = planReleaseDate;
    }

    private static final String BIZ_PLAN_RELEASE_DATE_MSG = "### %s  \n  您提交的业务需求： **%s**，状态为 **%s**，预期上线时间变更为 **%s**  \n\n  ***  \n  [查看详情](%s)";

    @Override
    public void run() {
        List<String> receivers = Lists.newArrayList(receiver);
        String title = MessageTitleEnum.BIZDEMAND_STATUS_CHANGE.getText();
        String singleUrl = domainName + String.format(PARAM, TabEnum.BUSINESS_MANAGEMENT.getText(), bizDemandId);
        String markdown = String.format(BIZ_PLAN_RELEASE_DATE_MSG, title, name, status, planReleaseDate, singleUrl);

        MarkdownMsg markdownMsg = MarkdownMsg.builder()
                .title(title)
                .content(markdown)
                .receivers(receivers)
                .build();

        erpMessageClient.sendMarkdownMsg(markdownMsg);
    }
}
