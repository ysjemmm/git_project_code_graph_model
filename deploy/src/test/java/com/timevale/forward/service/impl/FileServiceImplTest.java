package com.timevale.forward.service.impl;

import com.timevale.forward.dal.dao.FileMapper;
import com.timevale.forward.dal.entity.FileDO;
import com.timevale.forward.facade.api.request.FileAddReq;
import com.timevale.forward.service.component.FileComponent;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;
import org.testng.collections.Lists;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@TestExecutionListeners(listeners = MockitoTestExecutionListener.class)
public class FileServiceImplTest extends AbstractTestNGSpringContextTests {
    @Mock
    private FileComponent fileComponent;

    @Mock
    private FileMapper fileMapper;

    @InjectMocks
    private FileServiceImpl fileService;

    @Test
    public void testAdd() {

        List<FileDO> list= Lists.newArrayList(new FileDO());
        when(fileComponent.select(1L ,1)).thenReturn(list);
        when(fileMapper.updateFileId(any())).thenReturn(1);
        assert fileService.add(new FileAddReq()).ifSuccess();
    }

    @Test
    public void testGetFiles() {
        List<FileDO> list= Lists.newArrayList(new FileDO());
        when(fileComponent.select(1L ,1)).thenReturn(list);
        assert fileService.getFiles().ifSuccess();
    }
}

































