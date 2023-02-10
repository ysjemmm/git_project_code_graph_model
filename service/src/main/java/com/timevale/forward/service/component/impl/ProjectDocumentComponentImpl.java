package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.dao.ProjectDocumentMapper;
import com.timevale.forward.dal.dao.ProjectFlowMapper;
import com.timevale.forward.dal.dao.TestBillMapper;
import com.timevale.forward.dal.entity.ProjectDocument;
import com.timevale.forward.dal.entity.ProjectFlowDO;
import com.timevale.forward.dal.entity.ProjectNodeDO;
import com.timevale.forward.dal.entity.TestBillDO;
import com.timevale.forward.model.enums.ProjectNodeEnum;
import com.timevale.forward.model.enums.ProjectTypeEnum;
import com.timevale.forward.service.component.ProjectDocumentComponent;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author xiaoyun
 * @date 2022/8/31/031 18:21
 */
@Component
@Slf4j
public class ProjectDocumentComponentImpl implements ProjectDocumentComponent {

    @Resource
    private ProjectDocumentMapper projectDocumentMapper;

    @Resource
    private ProjectFlowMapper projectFlowMapper;

    @Resource
    private TestBillMapper testBillMapper;

    @Override
    public ProjectDocument getByProjectId(Long projectId, Integer type) {
        return projectDocumentMapper.getByProjectId(projectId, type);
    }

    @Override
    public Long insert(ProjectDocument projectDocument) {
        return projectDocumentMapper.insert(projectDocument);
    }

    @Override
    public void updateSelective(ProjectDocument projectDocument) {
        projectDocumentMapper.updateByPrimaryKeySelective(projectDocument);
    }

    @Override
    public List<String> docNeedFillIn(Long projectId, List<ProjectNodeDO> projectNodeDOList, Integer type) {
        List<String> result = new ArrayList<>();
        if (!ProjectTypeEnum.OPTIMIZE.getCode().equals(type)) {
            ProjectDocument projectDocument = projectDocumentMapper.getByProjectId(projectId, 1);
            if (projectDocument == null) {
                result.add("产品需求文档");
            }
        }
        List<Integer> codes = projectNodeDOList.stream()
                .filter(a -> ProjectNodeEnum.UED_AUDIT.getText().equals(a.getName())
                        || ProjectNodeEnum.TECHNICAL_DETAIL_REVIEW.getText().equals(a.getName()))
                .map(a -> ProjectNodeEnum.getCodeByName(a.getName())).collect(Collectors.toList());

        List<ProjectFlowDO> projectFlowDOList = projectFlowMapper.getByProjectId(projectId);
        List<Integer> types = projectFlowDOList.stream().map(ProjectFlowDO::getFlowType).collect(Collectors.toList());

        codes.removeAll(types);
        codes.forEach(a -> {
            if (ProjectNodeEnum.UED_AUDIT.getCode().equals(a)) {
                result.add("UED设计文档");
            } else if (ProjectNodeEnum.TECHNICAL_DETAIL_REVIEW.getCode().equals(a)) {
                result.add("详设文档");
            }
        });
        boolean anyMatch = projectNodeDOList.stream().anyMatch(a -> ProjectNodeEnum.WRITE_TEST_CASES.getText().equals(a.getName()));
        TestBillDO testBillDO = testBillMapper.selectByProjectId(projectId);
        if (anyMatch && (testBillDO == null || StringUtils.isEmpty(testBillDO.getDocCreateManId()))) {
            //有编写测试用例节点,无文档
            result.add("测试文档");
        }
        return result;
    }

    @Override
    public List<ProjectDocument> list(Long projectId) {
        return projectDocumentMapper.selectByProjectId(projectId);
    }
}
