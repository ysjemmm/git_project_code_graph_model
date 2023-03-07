package com.timevale.forward.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.DevopsAppMapper;
import com.timevale.forward.dal.dto.DevopsAppDTO;
import com.timevale.forward.dal.dto.DevopsProjectDTO;
import com.timevale.forward.dal.entity.DevopsAppDO;
import com.timevale.forward.facade.api.client.DevopsAppService;
import com.timevale.forward.facade.api.query.DevopsAppQueryList;
import com.timevale.forward.facade.api.request.DevopsReq;
import com.timevale.forward.facade.api.request.DevopsUpdateStatReq;
import com.timevale.forward.facade.api.result.DevopsAppVO;
import com.timevale.forward.facade.api.result.DevopsProjectVO;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.DevopsAppCopier;
import com.timevale.forward.service.integration.publish.PublishPlatformClient;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.aop.LogPoint;
import com.timevale.mandarin.base.util.AssertUtil;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@LogPoint
@RestService
public class DevopsAppServiceImpl implements DevopsAppService {

    @Resource
    private DevopsAppMapper devopsAppMapper;
    @Resource
    private PublishPlatformClient platformClient;

    @Override
    public BaseResult<List<DevopsProjectVO>> getDevopsProjects(Long projectId) {
        List<DevopsAppDO> appDOList = devopsAppMapper.groupProjectByMainId(projectId);
        List<DevopsProjectVO> projectVOList = DevopsAppCopier.INSTANCE.do2pvo(appDOList);
        return BaseResult.success(projectVOList);
    }

    @Override
    public BaseResult<Boolean> addDevopsProject(DevopsReq devopsReq) {
        Long mainId = devopsReq.getMainId();
        String projectSign = devopsReq.getDevopsProjectSign();

        // 判断是否已经添加该项目数据
        List<DevopsAppDO> existAppDOList = devopsAppMapper.selectByProjectSign(mainId, projectSign);
        AssertUtil.checkState(CollUtil.isEmpty(existAppDOList), "该项目已添加");

        // 查询发布平台数据
        DevopsProjectDTO projectDTO = platformClient.getProject(projectSign);
        AssertUtil.notNull(projectDTO, "未查询到对应的项目信息");

        // 判断项目数据
        List<DevopsAppDTO> appList = projectDTO.getAppList();
        AssertUtil.checkState(CollUtil.isNotEmpty(appList), "该项目下未添加任何应用");

        // 转化数据
        List<DevopsAppDO> appDOList = appList.stream()
                .map(e -> DevopsAppCopier.INSTANCE.dto2do(e, projectDTO, mainId))
                .collect(Collectors.toList());

        // 落库
        devopsAppMapper.batchInsert(appDOList);

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> deleteDevopsProject(DevopsReq devopsReq) {
        devopsAppMapper.deleteProject(devopsReq.getMainId(), devopsReq.getDevopsProjectSign());
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> updateStatFlag(DevopsUpdateStatReq devopsReq) {
        devopsAppMapper.updateStatFlag(devopsReq.getId(), devopsReq.getStatFlag());
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<PageQueryResult<DevopsAppVO>> listDevopsApps(DevopsAppQueryList appQueryList) {
        // 分页查询
        PageHelper.startPage(appQueryList.pageNum, appQueryList.pageSize, CommonConstant.CREATE_ORDER_BY);
        List<DevopsAppDO> appDOList = devopsAppMapper.selectByMainId(appQueryList.getMainId());

        // 数据转换
        List<DevopsAppVO> appVOList = DevopsAppCopier.INSTANCE.do2vo(appDOList);

        // 分页转换
        PageQueryResult<DevopsAppVO> pageQueryResult = new PageQueryResult<>();
        PageInfo<DevopsAppDO> pageInfo = new PageInfo<>(appDOList);
        pageQueryResult.setResultList(appVOList);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);

        return BaseResult.success(pageQueryResult);
    }
}
