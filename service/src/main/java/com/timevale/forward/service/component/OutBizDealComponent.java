package com.timevale.forward.service.component;

import com.timevale.forward.dal.dao.BizDemandMapper;
import com.timevale.forward.dal.dao.BugOnlineMapper;
import com.timevale.forward.dal.entity.BizDemandDO;
import com.timevale.forward.dal.entity.BugOnlineDO;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.mq.producer.MqProducer;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.base.util.JsonUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;

import javax.annotation.Resource;

/**
 * 外部业务处理逻辑
 *
 * @author jingchun
 * created on 2022/9/9
 */
@Component
public class OutBizDealComponent {

    @Resource
    private BizDemandMapper bizDemandMapper;

    @Resource
    private BugOnlineMapper bugOnlineMapper;

    @Resource
    private MqProducer mqProducer;

    /**
     * 检查工单id是否已经被占用
     */
    public void checkBizIdExistence(String bizId) {
        if (bizDemandMapper.bizIdExists(bizId) ||
                bugOnlineMapper.bizIdExists(bizId)) {
            throw new BaseBizRuntimeException("该工单的数据已被转需求或者BUG，无法再次关联");
        }
    }


    /**
     * 发送业务需求关联消息
     */
    public void sendBizDemandRelMsg(@RequestBody BizDemandDO bizDemandDO) {
        mqProducer.asyncSend(CommonConstant.FORWARD_BIZ_RELATION_TOPIC,
                JsonUtils.obj2json(new ForwardRelationMsg(bizDemandDO.getId(), bizDemandDO.getBizId(), 1)));
    }

    /**
     * 发送线上BUG关联消息
     */
    public void sendBugOnlineRelMsg(@RequestBody BugOnlineDO bugOnlineDO) {
        mqProducer.asyncSend(CommonConstant.FORWARD_BIZ_RELATION_TOPIC,
                JsonUtils.obj2json(new ForwardRelationMsg(bugOnlineDO.getId(), bugOnlineDO.getBizId(), 2)));
    }
}

@Getter
@Setter
@AllArgsConstructor
class ForwardRelationMsg {

    private Long id;

    private String bizId;

    // 类型 1-业务需求; 2-线上BUG
    private Integer type;

}
