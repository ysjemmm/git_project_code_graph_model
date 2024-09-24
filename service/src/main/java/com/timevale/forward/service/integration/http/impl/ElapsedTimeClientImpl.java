package com.timevale.forward.service.integration.http.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.timevale.forward.service.integration.http.ElapsedTimeClient;
import com.timevale.forward.service.utils.date.DateUtil;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 * 根据入参的开始时间和结束时间，计算两者之间经过的时间，单位精确到秒
 * <p>
 * 其中经过时间，只计算工作日：周一至周五，排除国定节假日，加上国定调休日。
 * <p>
 * 过滤时间时，仅计算工作时间段：上午9:00~12:00，下午13:30~18:30
 **/
@Slf4j
@Service
public class ElapsedTimeClientImpl implements ElapsedTimeClient {

    @Resource
    private RestTemplate restTemplate;

    // 本地测试使用这行
//     @Value("${elapsedTime.baseUrl:http://dingtalk.testk8s.tsign.cn/workday/}")
    @Value("${elapsedTime.baseUrl:http://dingtalk-testvpc-svc.local-test:8181/workday/}")
    private String baseUrl;

    @Override
    public Long getElapsedTime(Date startTime, Date endTime) {
        log.info("secondTime: startTime: {},endTime: {}", startTime, endTime);
        JSONObject param = new JSONObject();
        param.put("startTime", DateUtil.parseToString(startTime));
        param.put("endTime", DateUtil.parseToString(endTime));
        return getTime(param, baseUrl + "elapsedTimeV2/").getLong("elapsedtime");
    }

    @Override
    public String getElapsedEndTime(Date startTime, Long seconds) {
        log.info("endTime: startTime: {},seconds: {}", startTime, seconds);
        JSONObject param = new JSONObject();
        param.put("startTime", DateUtil.parseToString(startTime));
        param.put("seconds", seconds);
        return getTime(param, baseUrl + "deadlineV2/").getString("deadline");
    }

    @Override
    public List<String> getHolidays(Date startTime, Date endTime, boolean holiday) {
        log.info("holiday: startTime: {},endTime: {}", startTime, endTime);
        JSONObject param = new JSONObject();
        param.put("startTime", DateUtil.parseToString(startTime));
        param.put("endTime", DateUtil.parseToString(endTime));
        String url = holiday ? "holidayList/" : "workdayList/";
        JSONArray dayList = getTime(param, baseUrl + url).getJSONArray("dayList");
        return JSONObject.parseArray(dayList.toJSONString(), String.class);
    }

    private JSONObject getTime(JSONObject param, String url) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<JSONObject> entity = new HttpEntity<>(param, httpHeaders);
        String result = restTemplate.postForObject(url, entity, String.class);
        JSONObject jsonObject = JSONObject.parseObject(result);
        Integer code = jsonObject.getInteger("code");
        if (Integer.valueOf(0).equals(code)) {
            return jsonObject.getJSONObject("data");
        }
        log.info("获取工作日工作时长返回结果: result :{}", result);
        throw new BaseBizRuntimeException("计算工作日工作时长失败！");
    }
}
