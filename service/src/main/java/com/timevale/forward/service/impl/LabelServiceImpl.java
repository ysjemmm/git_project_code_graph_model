package com.timevale.forward.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.LabelListCondition;
import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.facade.api.client.LabelService;
import com.timevale.forward.facade.api.query.LabelQueryList;
import com.timevale.forward.facade.api.request.LabelAddReq;
import com.timevale.forward.facade.api.request.LabelModifyReq;
import com.timevale.forward.facade.api.result.LabelDetailVO;
import com.timevale.forward.facade.api.result.LabelVO;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.LabelCopier;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.assertj.core.util.Lists;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Slf4j
@RestService
public class LabelServiceImpl implements LabelService {

    @Resource
    private LabelCategoryMapper labelCategoryMapper;

    @Resource
    private LabelMapper labelMapper;

    @Resource
    private LabelCategoryBizDomainMapper labelCategoryBizDomainMapper;

    @Resource
    private BizDomainMapper bizDomainMapper;

    @Resource
    private BizLabelMapper bizLabelMapper;


    @Override
    public BaseResult<PageQueryResult<LabelVO>> list(LabelQueryList labelQueryList) {
        log.info("标签列表,参数:{}", labelQueryList);
        PageHelper.startPage(labelQueryList.getPageNum(), labelQueryList.getPageSize(), CommonConstant.DEFAULT_ORDER_BY);
        LabelListCondition condition = LabelCopier.INSTANCE.convert(labelQueryList);
        List<LabelDO> labelDOList = labelMapper.list(condition);
        if (CollectionUtils.isEmpty(labelDOList)) {
            return BaseResult.success(ResultUtil.pageEmpty());
        }

        List<Long> categoryIds = labelDOList.stream().map(LabelDO::getLabelCategoryId).collect(Collectors.toList());

        List<LabelCategoryDO> labelCategoryDOList = labelCategoryMapper.get(Lists.newArrayList(categoryIds));
        Map<Long, LabelCategoryDO> labelCategoryMap = labelCategoryDOList.stream().collect(Collectors.toMap(LabelCategoryDO::getId, a -> a, (v1, v2) -> v2));

        List<LabelCategoryBizDomainDO> lcbds = labelCategoryBizDomainMapper.get(categoryIds);
        Map<Long, List<Long>> lcbdMap = lcbds.stream().collect(Collectors.groupingBy(LabelCategoryBizDomainDO::getLabelCategoryId
                , Collectors.mapping(LabelCategoryBizDomainDO::getBizDomainId, Collectors.toList())));


        List<Long> bizDomainIds = lcbds.stream().map(LabelCategoryBizDomainDO::getBizDomainId).collect(Collectors.toList());
        List<BizDomainDO> bizDomainDOList = bizDomainMapper.selectByIdList(bizDomainIds);
        Map<Long, BizDomainDO> bizDomainMap = bizDomainDOList.stream().collect(Collectors.toMap(BizDomainDO::getId, k -> k, (v1, v2) -> v2));

        List<LabelVO> labelVOList = LabelCopier.INSTANCE.convertT(labelDOList);

        labelVOList.forEach(a -> {
            List<Long> bdIds = lcbdMap.get(a.getLabelCategoryId());
            List<String> bizDomains = bdIds.stream().map(bizDomainMap::get).map(BizDomainDO::getName).collect(Collectors.toList());
            a.setDepts(Lists.emptyList());
            a.setBizDomains(bizDomains);
            LabelCategoryDO labelCategoryDO = labelCategoryMap.get(a.getLabelCategoryId());
            if (labelCategoryDO != null) {
                a.setCategoryName(labelCategoryDO.getName());
                a.setTypes(JSONObject.parseArray(labelCategoryDO.getType(), Integer.class));
                a.setMarkMans(JSONObject.parseArray(labelCategoryDO.getMarkMan(), String.class));
                a.setCategoryName(labelCategoryDO.getName());
            }
        });

        PageInfo<LabelDO> pageInfo = new PageInfo<>(labelDOList);
        PageQueryResult<LabelVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(labelVOList);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);
        return BaseResult.success(pageQueryResult);
    }

    @Override
    public BaseResult<LabelDetailVO> get(Long labelId) {
        log.info("标签查看,参数:{}", labelId);
        LabelDO labelDO = labelMapper.get(labelId);
        if(labelDO==null){
            throw new BaseBizRuntimeException("找不到标签");
        }
        List<LabelCategoryDO> labelCategoryDOList = labelCategoryMapper.get(Lists.newArrayList(labelDO.getLabelCategoryId()));
        LabelCategoryDO labelCategoryDO = labelCategoryDOList.get(0);
        Long categoryId = labelCategoryDO.getId();

        List<LabelCategoryBizDomainDO> lcbds = labelCategoryBizDomainMapper.get(Lists.newArrayList(categoryId));
        List<Long> bdIds = lcbds.stream().map(LabelCategoryBizDomainDO::getBizDomainId).collect(Collectors.toList());

        List<BizDomainDO> bizDomainDOList = bizDomainMapper.selectByIdList(bdIds);
        List<String> bizDomains = bizDomainDOList.stream().map(BizDomainDO::getName).collect(Collectors.toList());

        LabelDetailVO labelDetailVO = new LabelDetailVO();
        labelDetailVO.setBizDomains(bizDomains);
        labelDetailVO.setCategoryId(categoryId);
        labelDetailVO.setCategoryName(labelCategoryDO.getName());
        labelDetailVO.setTypes(JSONObject.parseArray(labelCategoryDO.getType(), Integer.class));
        return BaseResult.success(labelDetailVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> delete(Long labelId) {
        log.info("标签删除,参数:{}", labelId);
        List<BizLabelDO> bizLabelDOList = bizLabelMapper.get(labelId);
        if (CollectionUtils.isNotEmpty(bizLabelDOList)) {
            throw new BaseBizRuntimeException("标签名称已被引用,不可删除");
        }
        LabelDO labelDO = new LabelDO();
        labelDO.setId(labelId);
        labelDO.setIsDeleted(true);
        labelMapper.update(labelDO);
        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> add(LabelAddReq labelAddReq) {
        log.info("标签新增,参数:{}", labelAddReq);
        List<String> names = labelAddReq.getNames();
        Long categoryId = labelAddReq.getCategoryId();
        List<LabelDO> labelDOList = labelMapper.getByNameInOneCategory(names, categoryId);
        if (!CollectionUtils.isEmpty(labelDOList)) {
            throw new BaseBizRuntimeException("名称为: " + names + " 的标签,已在该标签类别下存在,请修改后重试");
        }
        List<LabelDO> labelDos = names.stream().map(a -> {
            LabelDO o = new LabelDO();
            o.setLabelCategoryId(categoryId);
            o.setName(a);
            return o;
        }).collect(Collectors.toList());
        labelMapper.batchInsert(labelDos);
        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> modify(LabelModifyReq labelModifyReq) {
        log.info("标签修改,参数:{}", labelModifyReq);
        LabelDO labelDO = new LabelDO();
        labelDO.setId(labelModifyReq.getId());
        labelDO.setName(labelModifyReq.getName());
        labelMapper.update(labelDO);
        return BaseResult.success(true);
    }

}
