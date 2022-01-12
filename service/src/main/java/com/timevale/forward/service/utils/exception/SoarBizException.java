package com.timevale.forward.service.utils.exception;

import com.timevale.mandarin.base.enums.BaseResultCodeEnum;
import com.timevale.mandarin.base.exception.BaseRuntimeException;

/**
 * @author yuankai
 * @date 2020/12/14 18:39
 */
public class SoarBizException extends BaseRuntimeException {

    public SoarBizException(String detailMessage) {
        super(BaseResultCodeEnum.SYSTEM_ERROR, detailMessage);
    }

    public SoarBizException(String code, String detailMessage) {
        super(code, detailMessage);
    }

    public SoarBizException(String code, String digestMessage, String detailMessage) {
        super(code, digestMessage, detailMessage);
    }
}
