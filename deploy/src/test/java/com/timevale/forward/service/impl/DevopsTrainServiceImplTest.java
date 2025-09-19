package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.service.integration.publish.PublishPlatformClient;
import com.timevale.mandarin.common.result.PageQueryResult;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Devops发布火车服务测试类
 *
 * @author your-name
 * @date 2025/9/17
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class DevopsTrainServiceImplTest {

    @InjectMocks
    private DevopsTrainServiceImpl devopsTrainService;

    @Mock
    private PublishPlatformClient platformClient;

    @Test
    public void testGetTrainList() {
        // 准备测试数据
        Map<String, Object> params = new HashMap<>();
        params.put("limit", 10);
        params.put("offset", 0);

        Map<String, Object> mockResult = new HashMap<>();
        mockResult.put("count", 1);
        mockResult.put("list", new HashMap<>());

        // 模拟客户端调用
        when(platformClient.getTrainList(params)).thenReturn(mockResult);

        // 执行测试
        BaseResult<PageQueryResult<Map<String, Object>>> result = devopsTrainService.getTrainList(params);

        // 验证结果
        assertTrue(result.ifSuccess());
        assertNotNull(result.getData());
        assertEquals(1, (int) result.getData().getTotalItems());

        // 验证方法调用
        verify(platformClient, times(1)).getTrainList(params);
    }

    @Test
    public void testGetTrainListWithNullResult() {
        // 准备测试数据
        Map<String, Object> params = new HashMap<>();

        // 模拟客户端调用返回null
        when(platformClient.getTrainList(params)).thenReturn(null);

        // 执行测试
        BaseResult<PageQueryResult<Map<String, Object>>> result = devopsTrainService.getTrainList(params);

        // 验证结果
        assertFalse(result.ifSuccess());
        assertEquals("获取发布火车列表失败", result.getMessage());

        // 验证方法调用
        verify(platformClient, times(1)).getTrainList(params);
    }

    @Test
    public void testGetTrainDetail() {
        // 准备测试数据
        Integer trainId = 646;

        Map<String, Object> mockResult = new HashMap<>();
        Map<String, Object> resultObj = new HashMap<>();
        resultObj.put("id", 123);
        resultObj.put("name", "test train");
        mockResult.put("result", resultObj);

        // 模拟客户端调用
        when(platformClient.getTrainDetail(trainId)).thenReturn(mockResult);

        // 执行测试
        BaseResult<Map<String, Object>> result = devopsTrainService.getTrainDetail(trainId);

        // 验证结果
        assertTrue(result.ifSuccess());
        assertNotNull(result.getData());
        assertEquals("test train", result.getData().get("name"));

        // 验证方法调用
        verify(platformClient, times(1)).getTrainDetail(trainId);
    }

    @Test
    public void testGetTrainDetailWithNullResult() {
        // 准备测试数据
        Integer trainId = 646;

        // 模拟客户端调用返回null
        when(platformClient.getTrainDetail(trainId)).thenReturn(null);

        // 执行测试
        BaseResult<Map<String, Object>> result = devopsTrainService.getTrainDetail(trainId);

        // 验证结果
        assertFalse(result.ifSuccess());
        assertEquals("获取发布火车详情失败", result.getMessage());

        // 验证方法调用
        verify(platformClient, times(1)).getTrainDetail(trainId);
    }
}
