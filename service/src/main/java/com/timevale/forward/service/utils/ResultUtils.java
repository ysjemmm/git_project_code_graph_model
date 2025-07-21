package com.timevale.forward.service.utils;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.mandarin.base.enums.BaseResultCodeEnum;
import com.timevale.mandarin.common.result.BusinessResult;

/**
 * @author jingchun
 * created on 2021/10/15
 */
public class ResultUtils {

    public static <T> BusinessResult<T> success(T data) {
        BusinessResult<T> result = new BusinessResult<>();
        result.setData(data);
        return result;
    }

    public static <T> BusinessResult<T> success() {
        return new BusinessResult<>();
    }


    public static <T> BusinessResult<T> fail(int code, String msg) {
        return new BusinessResult<>(code, msg);
    }

    public static <T> BusinessResult<T> fromBaseResult(BaseResult<T> result) {
        if (result == null) {
            return fail(BaseResultCodeEnum.SYSTEM_ERROR.getNCode(), BaseResultCodeEnum.SYSTEM_ERROR.getMessage());
        }
        if (result.ifSuccess()) {
            return success(result.getData());
        }
        return fail(result.getCode(), result.getMessage());
    }

    public static <T> BusinessResult<T> result(BaseResult<T> result) {
        return new BusinessResult<>(result.getCode(), result.getMessage(), result.getData());
    }

}
