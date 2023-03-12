package com.timevale.forward.service.impl;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.HistoryRecordMapper;
import com.timevale.forward.dal.entity.HistoryRecordDO;
import com.timevale.forward.dal.entity.ProjectMemberEvaluateDO;
import com.timevale.forward.facade.api.client.HistoryRecordService;
import com.timevale.forward.facade.api.request.HistoryRecordCmpReq;
import com.timevale.forward.facade.api.result.HistoryRecordCmpVO;
import com.timevale.forward.facade.api.result.HistoryRecordVO;
import com.timevale.forward.model.enums.AddOrDelOrCoEnum;
import com.timevale.forward.service.copy.HistoryRecordCopier;
import com.timevale.forward.service.utils.aop.LogPoint;
import com.timevale.mandarin.common.annotation.RestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

@Slf4j
@LogPoint
@RestService
@RequiredArgsConstructor
public class HistoryRecordImpl implements HistoryRecordService {
    private final HistoryRecordMapper recordMapper;

    @Override
    public BaseResult<List<HistoryRecordVO>> recordList(Long id) {
        List<HistoryRecordDO> recordDOList = recordMapper.selectByProjectId(id);
        List<HistoryRecordVO> recordVOList = HistoryRecordCopier.INSTANCE.do2vo(recordDOList);
        return BaseResult.success(recordVOList);
    }


    @Override
    public BaseResult<List<HistoryRecordCmpVO>> compare(HistoryRecordCmpReq req) {
        final Long projectId = req.getId();
        final BigDecimal minVersion = req.getMinVersion();
        final BigDecimal maxVersion = req.getMaxVersion();

        // 查询版本记录
        HistoryRecordDO minRecordDO = recordMapper.selectByVersion(projectId, minVersion);
        HistoryRecordDO maxRecordDO = recordMapper.selectByVersion(projectId, maxVersion);

        // 获取并解析工作量内容
        String minContent = minRecordDO.getRecordContent();
        String maxContent = maxRecordDO.getRecordContent();
        List<ProjectMemberEvaluateDO> minEvalDOList = JSON.parseArray(minContent, ProjectMemberEvaluateDO.class);
        List<ProjectMemberEvaluateDO> maxEvalDOList = JSON.parseArray(maxContent, ProjectMemberEvaluateDO.class);

        // 按照用户名称分类
        ImmutableMap<String, ProjectMemberEvaluateDO> minRecordMap = Maps.uniqueIndex(minEvalDOList, ProjectMemberEvaluateDO::getUserName);
        ImmutableMap<String, ProjectMemberEvaluateDO> maxRecordMap = Maps.uniqueIndex(maxEvalDOList, ProjectMemberEvaluateDO::getUserName);

        // 两个版本的用户名集合
        ImmutableSet<String> minUserNameSet = minRecordMap.keySet();
        ImmutableSet<String> maxUserNameSet = maxRecordMap.keySet();

        // 合并两个版本的用户名集合，遍历用户
        HashSet<String> userNameSet = new HashSet<>();
        userNameSet.addAll(minUserNameSet);
        userNameSet.addAll(maxUserNameSet);

        // 遍历全部用户，比较
        List<HistoryRecordCmpVO> result = new ArrayList<>();
        for (String userName : userNameSet) {
            // 获取大小版本的计划工作量，计算相差的天数
            BigDecimal minWorkload = Optional.ofNullable(minRecordMap.get(userName))
                    .flatMap(e -> Optional.ofNullable(e.getPlanWorkload()))
                    .orElse(BigDecimal.ZERO);
            BigDecimal maxWorkload = Optional.ofNullable(maxRecordMap.get(userName))
                    .flatMap(e -> Optional.ofNullable(e.getPlanWorkload()))
                    .orElse(BigDecimal.ZERO);
            BigDecimal timeDiff = maxWorkload.subtract(minWorkload);

            // 当前用户在两个版本对比中，是新增还是删除还是共有
            int addOrDelOrCo;
            if (minUserNameSet.contains(userName) && maxUserNameSet.contains(userName)) {
                addOrDelOrCo = AddOrDelOrCoEnum.COEXIST.getCode();
            } else if (maxUserNameSet.contains(userName)){
                addOrDelOrCo = AddOrDelOrCoEnum.ADD.getCode();
            } else {
                addOrDelOrCo = AddOrDelOrCoEnum.DEL.getCode();
            }

            // 组装参数
            HistoryRecordCmpVO cmpVO = new HistoryRecordCmpVO();
            cmpVO.setUserName(userName);
            cmpVO.setTimeDiff(timeDiff);
            cmpVO.setMaxVersion(maxVersion);
            cmpVO.setMinVersion(minVersion);
            cmpVO.setAddOrDelOrCo(addOrDelOrCo);

            // 添加到返回结果中
            result.add(cmpVO);
        }

        return BaseResult.success(result);
    }
}
