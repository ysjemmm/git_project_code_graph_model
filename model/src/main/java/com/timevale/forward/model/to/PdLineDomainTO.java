package com.timevale.forward.model.to;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author by YangXu
 * @date 2023/03/29 17:27
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PdLineDomainTO {

    /**
     * 产品线id
     */
    private Long productLineId;

    /**
     * 产品线名称
     */
    private String productLineName;

    /**
     * 业务域id
     */
    private Long bizDomainId;

    /**
     * 业务域名称
     */
    private String bizDomainName;
}
