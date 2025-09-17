package com.timevale.forward.service.integration.publish;

import com.timevale.forward.dal.dto.DevopsTrainDTO;
import com.timevale.forward.dal.dto.DevopsTrainListResultDTO;
import com.timevale.forward.facade.api.request.DevopsTrainListReq;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author your-name
 * @date 2025/9/16
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class PublishPlatformClientTest {

    @Resource
    private PublishPlatformClient publishPlatformClient;

    @Test
    public void testGetTrain() {
        // 测试正常情况
        DevopsTrainDTO train = publishPlatformClient.getTrain(1);
        // 由于我们没有真实的发布平台环境，这里只验证方法能够正常执行
        assertNotNull("方法应该正常执行", train);
        
        // 测试null参数情况
        DevopsTrainDTO nullTrain = publishPlatformClient.getTrain(null);
        assertNull("当参数为null时，应该返回null", nullTrain);
    }
    
    @Test
    public void testListTrains() {
        // 测试正常情况
        DevopsTrainListReq trainListReq = new DevopsTrainListReq();
        trainListReq.setLimit(10);
        trainListReq.setOffset(0);
        DevopsTrainListResultDTO trainList = publishPlatformClient.listTrains(trainListReq);
        // 由于我们没有真实的发布平台环境，这里只验证方法能够正常执行
        assertNotNull("方法应该正常执行", trainList);
    }
}