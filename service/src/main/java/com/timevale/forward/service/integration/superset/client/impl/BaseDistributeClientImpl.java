package com.timevale.forward.service.integration.superset.client.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.timevale.forward.service.integration.superset.client.BaseDistributeClient;
import com.timevale.forward.service.integration.superset.config.DistributeConfigVO;
import com.timevale.forward.service.integration.superset.constant.CommonConstant;
import com.timevale.forward.service.integration.superset.model.base.DistributeCount;
import com.timevale.forward.service.integration.superset.model.base.DistributePageQueryVO;
import com.timevale.forward.service.integration.superset.model.base.DistributeResult;
import com.timevale.forward.service.integration.superset.model.base.PageResult;
import com.timevale.forward.service.integration.superset.util.DistributeThread;
import com.timevale.forward.service.utils.exception.SoarBizException;
import com.timevale.mandarin.base.util.AssertUtil;
import com.timevale.mandarin.base.util.StringUtils;
import com.timevale.mandarin.common.service.retry.RetryCallback;
import com.timevale.mandarin.common.service.retry.RetryTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

import javax.annotation.Resource;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * @author yuankai
 * @date 2021/6/16 16:56
 */
@Slf4j
public class BaseDistributeClientImpl<T> implements BaseDistributeClient<T> {

    /**
     * superset 环境地址
     */
    @Value("${distribute.host:http://hua.testk8s.tsign.cn/proxy/dataDis/api/40/}")
    private String distributeHost;

    @Resource
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    @Resource
    private RestTemplate restTemplate;

    public static final String SLASH = "/";


    /**
     * 解析列表
     *
     * @param result 待解析数据
     * @return 列表
     */
    private List<T> getListResult(String result) {

        checkResult(result);

        ParameterizedType pt = (ParameterizedType) this.getClass().getGenericSuperclass();

        Type type = ParameterizedTypeImpl.make(DistributeResult.class, pt.getActualTypeArguments(), null);

        DistributeResult<T> o = JSON.parseObject(result, type);
        if (!Integer.valueOf(0).equals(o.getCode())) {
            log.info("数据分发返回code={},message={},data={},", o.getCode(), o.getMessage(), o.getData());
        }
        return o.getData();
    }

    private void checkResult(String result) {
        if (StringUtils.isEmpty(result)) {
            throw new SoarBizException("20301001", "查询数据分发异常");
        }
    }

    /**
     * 解析数量
     *
     * @param result 待解析数据
     * @return 数量
     */
    private Integer getCountResult(String result) {
        checkResult(result);

        DistributeResult<DistributeCount> countDistributeResult = JSON.parseObject(result, new TypeReference<DistributeResult<DistributeCount>>() {
        });
        List<DistributeCount> list = countDistributeResult.getData();
        return list.isEmpty() ? CommonConstant.ZERO : list.get(CommonConstant.ZERO).getCount();
    }

    /**
     * superset 查询工具
     *
     * @param param 查询条件 json
     * @return 返回结果  json
     */
    @Override
    public List<T> doGet(DistributePageQueryVO param) {
        // 验空
        AssertUtil.notNull(param);

        // 发起请求
        String result = doRequest(param);

        // 解析返回
        return getListResult(result);
    }


    /**
     * 获取查询数量
     *
     * @param param 查询条件 json
     * @return 总数
     */
    @Override
    public Integer doCount(DistributePageQueryVO param) {
        // 验空
        AssertUtil.notNull(param);

        // 发起请求
        String result = doRequest(param);

        // 解析返回
        return getCountResult(result);
    }

    /**
     * 查询分页
     *
     * @param param      查询条件
     * @param countParam 查询条件
     * @return 数据
     */
    @Override
    public PageResult<T> doPage(DistributePageQueryVO param, DistributePageQueryVO countParam) {
        try {

            CountDownLatch threadSignal = new CountDownLatch(2);

            DistributeThread queryThread = new DistributeThread(param, this::doRequest);
            queryThread.setThreadsSignal(threadSignal);
            threadPoolTaskExecutor.execute(queryThread);

            DistributeThread countThread = new DistributeThread(countParam, this::doRequest);
            countThread.setThreadsSignal(threadSignal);
            threadPoolTaskExecutor.execute(countThread);

            // 等待请求返回
            threadSignal.await();

            // 获取查询的结果
            PageResult<T> pageResult = new PageResult<>();
            pageResult.setResult(getListResult(queryThread.getSupersetResult()));
            pageResult.setTotal(getCountResult(countThread.getSupersetResult()));
            return pageResult;
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
            log.error("查询异常, queryFormData:{}, countFormData:{}", param, countParam);
            return new PageResult<>();
        }
    }

    private String doRequest(DistributePageQueryVO param) {
        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setWaitTime(100);
        return (String) retryTemplate.execute(new RetryCallback() {
            @Override
            public Object doWithRetry() {
                return doRetryRequest(param);
            }

            @Override
            public boolean isComplete(Object result) {
                boolean b = result != null;
                if (!b) {
                    log.warn("请求产生一次重试   param: {} ", param);
                }
                return b;
            }
        });
    }

    private String doRetryRequest(DistributePageQueryVO param) {
        try {
            JSONObject paramObject = new JSONObject();
            paramObject.put("param", param.getParams());
            paramObject.put("token", param.getDistributeConfigVO().getToken());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<JSONObject> entity = new HttpEntity<>(paramObject, headers);
            DistributeConfigVO vo = param.getDistributeConfigVO();
            return restTemplate.postForObject(distributeHost + vo.getLevel() + SLASH + vo.getPath(), entity, String.class);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            log.error("superset request form_data: {}", param);
        }
        return null;
    }
}
