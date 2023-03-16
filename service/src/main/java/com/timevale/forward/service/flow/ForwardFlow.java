package com.timevale.forward.service.flow;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * @author by YangXu
 * @date 2023/03/16 14:01
 */
@Component
@RequiredArgsConstructor
public class ForwardFlow {
    public final ConclusionFlow conclusionFlow;
    public final WorkloadChangeFlow workloadChangeFlow;
}
