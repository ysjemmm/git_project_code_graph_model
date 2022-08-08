package com.timevale.forward.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.LabelCategoryListCondition;
import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.facade.api.client.LabelCategoryService;
import com.timevale.forward.facade.api.query.LabelCategoryQueryList;
import com.timevale.forward.facade.api.query.LabelInCategoryQueryList;
import com.timevale.forward.facade.api.request.LabelCategoryAddReq;
import com.timevale.forward.facade.api.request.LabelCategoryModifyReq;
import com.timevale.forward.facade.api.result.LabelCategoryDetailVO;
import com.timevale.forward.facade.api.result.LabelCategorySimpleVO;
import com.timevale.forward.facade.api.result.LabelCategoryVO;
import com.timevale.forward.facade.api.result.LabelSimpleVO;
import com.timevale.forward.model.enums.BizTypeEnum;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.LabelCategoryCopier;
import com.timevale.forward.service.copy.LabelCopier;
import com.timevale.forward.service.integration.inneruser.InnerGroupClient;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import com.timevale.security.facade.response.BaseInfoResponse;
import com.timevale.security.facade.response.GroupModelResponse;
import com.timevale.security.facade.response.GroupResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.util.Lists;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Slf4j
@RestService
public class LabelCategoryServiceImpl implements LabelCategoryService {

    @Resource
    private LabelCategoryMapper labelCategoryMapper;

    @Resource
    private LabelMapper labelMapper;

    @Resource
    private LabelCategoryBizDomainMapper labelCategoryBizDomainMapper;

    @Resource
    private BizDomainMapper bizDomainMapper;

    @Resource
    private InnerGroupClient innerGroupClient;

    @Resource
    private InnerUserPersonClient innerUserPersonClient;

    @Resource
    private ProductLineMapper productLineMapper;


    @Override
    public BaseResult<PageQueryResult<LabelCategoryVO>> list(LabelCategoryQueryList labelCategoryQueryList) {
        log.info("类别列表,参数:{}", labelCategoryQueryList);

        PageHelper.startPage(labelCategoryQueryList.getPageNum(), labelCategoryQueryList.getPageSize(), CommonConstant.DEFAULT_ORDER_BY);
        LabelCategoryListCondition condition = LabelCategoryCopier.INSTANCE.convert(labelCategoryQueryList);
        List<LabelCategoryDO> labelCategoryDOList = labelCategoryMapper.list(condition);
        if(CollectionUtils.isEmpty(labelCategoryDOList)){
            return BaseResult.success(ResultUtil.pageEmpty());
        }

        List<Long> categoryIds = labelCategoryDOList.stream().map(LabelCategoryDO::getId).collect(Collectors.toList());
        List<LabelCategoryBizDomainDO> lcbds = labelCategoryBizDomainMapper.get(categoryIds);

        Map<Long, List<Long>> lcbdMap = lcbds.stream().collect(Collectors.groupingBy(LabelCategoryBizDomainDO::getLabelCategoryId
                , Collectors.mapping(LabelCategoryBizDomainDO::getBizDomainId, Collectors.toList())));


        List<Long> bizDomainIds = lcbds.stream().map(LabelCategoryBizDomainDO::getBizDomainId).collect(Collectors.toList());
        List<BizDomainDO> bizDomainDOList = bizDomainMapper.selectByIdList(bizDomainIds);
        Map<Long, BizDomainDO> bizDomainMap = bizDomainDOList.stream().collect(Collectors.toMap(BizDomainDO::getId, k -> k, (v1, v2) -> v2));

        List<String> allDeptIds = new ArrayList<>();
        labelCategoryDOList.forEach(a->{
            if(StringUtils.isNotEmpty(a.getDeptId())){
                List<String> deptIds = JSONObject.parseArray(a.getDeptId(), String.class);
                allDeptIds.addAll(deptIds);
            }
        });
        Map<String, String> deptMap=new HashMap<>();
        if(CollectionUtils.isNotEmpty(allDeptIds)){
            deptMap = innerGroupClient.batchGetSimpleGroupMap(allDeptIds);
        }

        List<LabelCategoryVO> labelCategoryVos = LabelCategoryCopier.INSTANCE.change(labelCategoryDOList);
        for (LabelCategoryVO a : labelCategoryVos) {
            if(StringUtils.isNotEmpty(a.getDeptId())){
                List<String> deptIds = JSONObject.parseArray(a.getDeptId(), String.class);
                List<String> depts = deptIds.stream().filter(deptMap::containsKey).map(deptMap::get).collect(Collectors.toList());
                a.setDepts(depts);
            }
            List<Long> bdIds = lcbdMap.get(a.getId());
            List<String> bizDomains = bdIds.stream().map(bizDomainMap::get).map(BizDomainDO::getName).collect(Collectors.toList());
            a.setBizDomains(bizDomains);
        }
        PageInfo<LabelCategoryDO> pageInfo = new PageInfo<>(labelCategoryDOList);
        PageQueryResult<LabelCategoryVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(labelCategoryVos);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);
        return BaseResult.success(pageQueryResult);
    }

    @Override
    public BaseResult<List<LabelCategorySimpleVO>> getAll() {
        List<LabelCategoryDO> all = labelCategoryMapper.getAll();
        List<LabelCategorySimpleVO> labelSimpleVOList = LabelCategoryCopier.INSTANCE.changeT(all);
        return BaseResult.success(labelSimpleVOList);
    }

    @Override
    public BaseResult<List<LabelCategorySimpleVO>> getLabelInCategory(LabelInCategoryQueryList labelInCategoryQueryList) {
        log.info("类别下的标签,参数:{}", labelInCategoryQueryList);
        LabelCategoryListCondition condition = LabelCategoryCopier.INSTANCE.convert(labelInCategoryQueryList);

        if(CollectionUtils.isNotEmpty(labelInCategoryQueryList.getProductLineIds())){
            List<ProductLineDO> productLineDOList = productLineMapper.selectByIds(labelInCategoryQueryList.getProductLineIds());
            List<Long> bizDomainIds = productLineDOList.stream().map(ProductLineDO::getBizDomainId).collect(Collectors.toList());
            condition.setBizDomainIds(bizDomainIds);
        }
        List<LabelCategoryDO> labelCategoryDOList = labelCategoryMapper.list(condition);
        if(CollectionUtils.isEmpty(labelCategoryDOList)){
            return BaseResult.success(Lists.emptyList());
        }

        if(labelInCategoryQueryList.getAuth()){
            List<GroupResponse> gdata = innerGroupClient.getGroupListTree(false);
            Map<String, GroupResponse> groupMap = gdata.stream().collect(Collectors.toMap(GroupResponse::getGroupId, a -> a, (v1, v2) -> v2));

            String account = LocalSessionUtils.getUserInfo().getId();
            BaseInfoResponse selfInfo = innerUserPersonClient.getSelfInfo(account, false);
            List<String> groupIds = selfInfo.getGroupList().stream().map(GroupModelResponse::getGroupId).collect(Collectors.toList());
            log.info("花名:{},部门:{}",account,groupIds);
            Set<String> deptSets=new HashSet<>(groupIds);
            for (String gid : groupIds) {
                getSuperiorGroupId(groupMap,gid,deptSets);
            }
            log.info("花名:{},部门及其上级部门:{}",account,deptSets);

            labelCategoryDOList = labelCategoryDOList.stream().filter(a -> {
                List<String> markManIds = JSONObject.parseArray(a.getMarkManId(), String.class);
                List<String> deptIds = JSONObject.parseArray(a.getDeptId(), String.class);
                //打标人或打标部门 包含该用户,或用户所在部门
                if(CollectionUtils.isNotEmpty(markManIds)&&markManIds.contains(account)){
                    return true;
                }
                if(CollectionUtils.isNotEmpty(deptIds)){
                    deptIds.retainAll(deptSets);
                    return CollectionUtils.isNotEmpty(deptIds);
                }
                return false;
            }).collect(Collectors.toList());
        }

        List<LabelCategorySimpleVO> labelCategorySimpleVos = LabelCategoryCopier.INSTANCE.convert(labelCategoryDOList);

        List<Long> categoryIds = labelCategoryDOList.stream().map(LabelCategoryDO::getId).collect(Collectors.toList());
        List<LabelDO> labelDOList = labelMapper.getByCategoryIds(categoryIds);
        List<LabelSimpleVO> labelSimpleVos = LabelCopier.INSTANCE.convert(labelDOList);

        Map<Long, List<LabelSimpleVO>> labelMap = labelSimpleVos.stream().collect(Collectors.groupingBy(LabelSimpleVO::getLabelCategoryId));

        labelCategorySimpleVos.forEach(a->{
            a.setLabelSimples(labelMap.get(a.getId()));
        });
        return BaseResult.success(labelCategorySimpleVos);
    }

    @Override
    public BaseResult<LabelCategoryDetailVO> get(Long categoryId) {
        log.info("类别查看,参数:{}", categoryId);
        List<LabelCategoryDO> labelCategoryDOList = labelCategoryMapper.get(Lists.newArrayList(categoryId));

        if(CollectionUtils.isEmpty(labelCategoryDOList)){
            throw new BaseBizRuntimeException("找不到该标签类别");
        }

        LabelCategoryDetailVO labelCategoryDetailVO = LabelCategoryCopier.INSTANCE.convert(labelCategoryDOList.get(0));
        List<LabelCategoryBizDomainDO> labelCategoryBizDomainDos = labelCategoryBizDomainMapper.get(Lists.newArrayList(categoryId));
        List<Long> bizDomainIds = labelCategoryBizDomainDos.stream().map(LabelCategoryBizDomainDO::getBizDomainId).collect(Collectors.toList());
        labelCategoryDetailVO.setBizDomainIds(bizDomainIds);
        return BaseResult.success(labelCategoryDetailVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> delete(Long categoryId) {
        log.info("类别删除,参数:{}", categoryId);
        List<LabelDO> labelDOList = labelMapper.getByCategoryIds(Lists.newArrayList(categoryId));

        if(CollectionUtils.isNotEmpty(labelDOList)){
            throw new BaseBizRuntimeException("该类别下已存在标签名称,不可删除。");
        }

        LabelCategoryDO labelCategoryDO=new LabelCategoryDO();
        labelCategoryDO.setId(categoryId);
        labelCategoryDO.setIsDeleted(true);
        labelCategoryMapper.update(labelCategoryDO);
        return BaseResult.success(true);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> add(LabelCategoryAddReq labelCategoryAddReq) {
        log.info("类别新增,参数:{}", labelCategoryAddReq);
        //同模块下,类别唯一
        LabelCategoryDO labelCategoryDO = LabelCategoryCopier.INSTANCE.convert(labelCategoryAddReq);

        checkBeforeInsert(labelCategoryDO);

        labelCategoryMapper.insert(labelCategoryDO);

        addRelation(labelCategoryAddReq.getBizDomainIds(),labelCategoryDO.getId());


        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> modify(LabelCategoryModifyReq labelCategoryModifyReq) {
        log.info("类别修改,参数:{}", labelCategoryModifyReq);
        LabelCategoryDO labelCategoryDO = LabelCategoryCopier.INSTANCE.convert(labelCategoryModifyReq);

        checkBeforeInsert(labelCategoryDO);

        labelCategoryMapper.update(labelCategoryDO);

        delRelation(labelCategoryModifyReq.getBizDomainIds(),labelCategoryModifyReq.getId());

        return BaseResult.success(true);
    }

    private void checkBeforeInsert(LabelCategoryDO labelCategoryDO){
        List<Integer> newType = JSONObject.parseArray(labelCategoryDO.getType(), Integer.class);
        List<LabelCategoryDO> categoryDOList = labelCategoryMapper.getByName(labelCategoryDO.getName());
        List<LabelCategoryDO> filter = categoryDOList.stream().filter(a -> !Objects.equals(labelCategoryDO.getId(), a.getId())).collect(Collectors.toList());
        filter.forEach(a->{
            List<Integer> oldType = JSONObject.parseArray(a.getType(), Integer.class);
            oldType.retainAll(newType);
            if(CollectionUtils.isNotEmpty(oldType)){
                throw new BaseBizRuntimeException("类别名称:"+labelCategoryDO.getName()+",在应用模块 "+ BizTypeEnum.getTextByCode(oldType)+" 中已存在,请修改后重试");
            }
        });
    }

    private void addRelation(List<Long> bizDomainIds, Long labelCategoryId) {
        if (CollectionUtils.isEmpty(bizDomainIds)) {
            return;
        }
        List<LabelCategoryBizDomainDO> lcbd = bizDomainIds.stream().map(a -> {
            LabelCategoryBizDomainDO o = new LabelCategoryBizDomainDO();
            o.setBizDomainId(a);
            o.setLabelCategoryId(labelCategoryId);
            return o;
        }).collect(Collectors.toList());

        labelCategoryBizDomainMapper.batchInsert(lcbd);
    }

    private void delRelation(List<Long> bizDomainIds, Long labelCategoryId) {
        if (CollectionUtils.isEmpty(bizDomainIds)) {
            return;
        }

        List<LabelCategoryBizDomainDO> lcbds = labelCategoryBizDomainMapper.get(Lists.newArrayList(labelCategoryId));
        List<Long> oldbdIds = lcbds.stream().map(LabelCategoryBizDomainDO::getBizDomainId).collect(Collectors.toList());

        List<Long> copyBizDomainIds=new ArrayList<>(bizDomainIds);
        copyBizDomainIds.removeAll(oldbdIds);

        addRelation(copyBizDomainIds, labelCategoryId);
        //删除
        lcbds.forEach(a -> {
            if (!bizDomainIds.contains(a.getBizDomainId())) {
                LabelCategoryBizDomainDO o = new LabelCategoryBizDomainDO();
                o.setId(a.getId());
                o.setIsDeleted(true);
                labelCategoryBizDomainMapper.update(o);
            }
        });
    }

    private void getSuperiorGroupId(Map<String, GroupResponse> groupMap,String gid,Set<String>deptIds){
        if(groupMap.containsKey(gid)){
            GroupResponse groupResponse = groupMap.get(gid);
            if (groupResponse.getDepth()>1){
                //部门及其上级部门,不包含顶级部门
                deptIds.add(groupResponse.getGroupId());
                getSuperiorGroupId(groupMap,groupResponse.getParentId(),deptIds);
            }
        }
    }

}
