package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.dto.UpdateTimeDTO;
import com.timevale.forward.service.component.DistributionComponent;
import com.timevale.forward.service.integration.superset.client.impl.BaseDistributeClientImpl;
import com.timevale.forward.service.integration.superset.config.DistributeConfig;
import com.timevale.forward.service.integration.superset.model.base.DistributePageQueryVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.assertj.core.util.Lists;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Optional;

/**
 * @author by YangXu
 * @date 2022/06/28 10:31
 */
@Slf4j
@Component
public class DistributionComponentImpl extends BaseDistributeClientImpl<UpdateTimeDTO> implements DistributionComponent {

    @Resource
    private DistributeConfig distributeConfig;

    @Override
    public UpdateTimeDTO getUpdateDate() {

        DistributePageQueryVO params = DistributePageQueryVO.builder()
                .params(Lists.emptyList())
                .distributeConfigVO(distributeConfig.getUpdateTime())
                .build();
        List<UpdateTimeDTO> updateTimeDTOList = doGet(params);

        if(CollectionUtils.isNotEmpty(updateTimeDTOList)) {
            Optional<UpdateTimeDTO> updateTime = updateTimeDTOList.stream().findAny();
            if(updateTime.isPresent()) {
                return updateTime.get();
            }
        }

        log.info("数据分发获取更新失败");
        return new UpdateTimeDTO();
    }
}
