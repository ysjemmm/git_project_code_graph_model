package com.timevale.forward.service.copy.convertor;

import com.timevale.forward.service.utils.date.DateStyle;
import com.timevale.forward.service.utils.date.DateUtil;
import org.apache.commons.lang3.StringUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author by YangXu
 * @date 2022/01/26 14:45
 */
public class DateConvertor {
    public Date convert(String date){
        if (!StringUtils.isEmpty(date)) {
            return DateUtil.parseToDate(date, DateStyle.YYYY_MM_DD_HH_MM_SS);
        }
        return null;
    }
}
