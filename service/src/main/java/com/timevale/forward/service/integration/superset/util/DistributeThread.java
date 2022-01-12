package com.timevale.forward.service.integration.superset.util;

import com.timevale.forward.service.integration.superset.model.base.DistributePageQueryVO;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;
import java.util.function.Function;

/**
 * 查询superset的线程
 *
 * @author 李涛
 * @date 2020/11/6 10:53
 */
@Slf4j
public class DistributeThread implements Runnable {

    private CountDownLatch threadsSignal;
    private String supersetResult;

    private final DistributePageQueryVO params;

    private final Function<DistributePageQueryVO, String> apply;

    public DistributeThread(DistributePageQueryVO params, Function<DistributePageQueryVO, String> apply) {
        this.apply = apply;
        this.params = params;
    }

    @Override
    public void run() {
        try {
            supersetResult = apply.apply(params);
        } finally {
            threadsSignal.countDown();
        }
    }

    public String getSupersetResult() {
        return supersetResult;
    }

    public void setThreadsSignal(CountDownLatch threadsSignal) {
        this.threadsSignal = threadsSignal;
    }
}

