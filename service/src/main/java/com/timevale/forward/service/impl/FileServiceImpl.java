package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.client.FileService;
import com.timevale.forward.facade.api.request.FileAddReq;
import com.timevale.mandarin.common.annotation.RestService;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;

import java.util.List;

/**
 * @author: xingyun
 * @create: 2021-12-13 13:44
 **/
@Slf4j
@RestService
public class FileServiceImpl implements FileService {

    @Override
    public BaseResult<Integer> add(FileAddReq fileAddReq) {
        log.info("新增文件接收参数:{}", fileAddReq);
        return BaseResult.success(1);
    }

    @Override
    public BaseResult<List<String>> list(String attachId) {
        log.info("文件列表接收参数:attachId={}", attachId);
        return BaseResult.success(Lists.newArrayList("1"));
    }

    @Override
    public BaseResult<Integer> delete(String attachId) {
        log.info("删除文件接收参数:attachId={}", attachId);
        return BaseResult.success(1);
    }
}
