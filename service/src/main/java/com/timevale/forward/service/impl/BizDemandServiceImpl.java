package com.timevale.forward.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Maps;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.BizDemandListCondition;
import com.timevale.forward.dal.condition.ProductBizDemandCondition;
import com.timevale.forward.dal.dao.BizDemandMapper;
import com.timevale.forward.dal.dao.ProductBizDemandMapper;
import com.timevale.forward.dal.dao.ProductLineMapper;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.facade.api.client.BizDemandService;
import com.timevale.forward.facade.api.query.BizDemandQueryList;
import com.timevale.forward.facade.api.request.*;
import com.timevale.forward.facade.api.result.BizDemandDetailVO;
import com.timevale.forward.facade.api.result.BizDemandVO;
import com.timevale.forward.facade.api.result.FileVO;
import com.timevale.forward.facade.api.result.PersonVO;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.service.component.FileComponent;
import com.timevale.forward.service.component.MessageComponent;
import com.timevale.forward.service.component.PersonComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.BizDemandCopier;
import com.timevale.forward.service.copy.FileCopier;
import com.timevale.forward.service.copy.PersonCopier;
import com.timevale.forward.service.integration.inneruser.InnerGroupClient;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.forward.service.utils.DateUtil;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.StringUtil;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import com.timevale.security.facade.response.GroupResponse;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.LongValue;
import org.apache.el.parser.BooleanNode;
import org.assertj.core.util.Lists;
import org.assertj.core.util.Sets;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author by YangXu
 * @date 2021/12/14 15:05
 */
@Slf4j
@RestService
public class BizDemandServiceImpl implements BizDemandService {

    @Resource
    BizDemandMapper bizDemandMapper;

    @Resource
    ProductLineMapper productLineMapper;

    @Resource
    ProjectMapper projectMapper;

    @Resource
    ProductBizDemandMapper productBizDemandMapper;

    @Resource
    InnerUserPersonClient innerUserPersonClient;

    @Resource
    InnerGroupClient innerGroupClient;

    @Resource
    PersonComponent personComponent;

    @Resource
    FileComponent fileComponent;

    @Resource
    MessageComponent messageComponent;


    @Override
    public BaseResult<PageQueryResult<BizDemandVO>> list(BizDemandQueryList bizDemandQueryList) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        // 开始分页
        PageHelper.startPage(bizDemandQueryList.pageNum, bizDemandQueryList.pageSize, CommonConstant.DEFAULT_ORDER_BY);

        // 转换查询条件
        BizDemandListCondition bizDemandListCondition = BizDemandCopier.INSTANCE.convert(bizDemandQueryList);
        // 通配符处理
        bizDemandListCondition.setName(StringUtil.toLikeStr(bizDemandListCondition.getName()));
        // 日期处理
        bizDemandListCondition.setCreateDateStart(DateUtil.getStartOfDay(bizDemandListCondition.getCreateDateStart()));
        bizDemandListCondition.setCreateDateEnd(DateUtil.getEndOfDay(bizDemandListCondition.getCreateDateEnd()));

        // 根据tabs添加不同的效果
        String ascription = bizDemandQueryList.getAscription();
        if(ascription.equals(AscriptionEnum.CURRENT_USER.toString())){
            bizDemandListCondition.setCreateManIdList(Lists.newArrayList(userInfo.getId()));
        }else if(ascription.equals(AscriptionEnum.RECEIVE.toString())){
            bizDemandListCondition.setReceiveManIdList(Lists.newArrayList(userInfo.getId()));
        }else if(ascription.equals(AscriptionEnum.COPIER.toString())){
            bizDemandListCondition.setCopier(userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName());
        }else {
            List<String> teamMember = innerUserPersonClient.getAllMyStaffWithSelf(userInfo.getId());
            if(ascription.equals(AscriptionEnum.TEAM_SUBMIT.toString())){
                if(bizDemandListCondition.getCreateManIdList().isEmpty()){
                    bizDemandListCondition.setCreateManIdList(teamMember);
                }
            }else if(ascription.equals(AscriptionEnum.TEAM_RECEIVE.toString())){
                if(bizDemandListCondition.getReceiveManIdList().isEmpty()){
                    bizDemandListCondition.setReceiveManIdList(teamMember);
                }
            }
        }

        Set<Long> queryDeptIdSet =  Sets.newHashSet(bizDemandQueryList.getDeptIdList());
        Map<Long, String> deptMap = Maps.newHashMap();
        GroupResponse rootNode = innerGroupClient.getGroupListTree(true);

        // 如果查询条件有部门id，收集子部门id及所需部门的完整名
        if(!queryDeptIdSet.isEmpty()){
            Boolean containsRootNode = queryDeptIdSet.contains(Long.valueOf(rootNode.getGroupId()));
            for (GroupResponse childNode : rootNode.getChildNode()){
                dfsGroupListTree(childNode, deptMap, queryDeptIdSet, "", containsRootNode);
            }
            // 替换查询部门id条件
            bizDemandListCondition.setDeptIdList(Lists.newArrayList(deptMap.keySet()));
        }

        // 查询并转换
        List<BizDemandListDO> bizDemandListDOList = bizDemandMapper.selectList(bizDemandListCondition);
        List<BizDemandVO> bizDemandVOList = BizDemandCopier.INSTANCE.convert(bizDemandListDOList);

        // 如果查询条件没有部门id，收集完整名
        if(queryDeptIdSet.isEmpty()){
            queryDeptIdSet.addAll(bizDemandVOList.stream().map(BizDemandVO::getDeptId).collect(Collectors.toList()));
            for (GroupResponse childNode : rootNode.getChildNode()){
                dfsGroupListTree(childNode, deptMap, queryDeptIdSet, "", false);
            }
        }

        // 部门名称待修改 ，需要完整名称
        bizDemandVOList.forEach( e -> {
            e.setPriorityText(PriorityEnum.getTextByCode(e.getPriority()));
            e.setStatusText(BizDemandStatusEnum.getTextByCode(e.getStatus()));
            e.setPlanReleaseDateText(PlanReleaseDateEnum.getTextByCode(e.getPlanReleaseDate()));
            e.setDeptName(deptMap.get(e.getDeptId()));
        });

        // 转换后返回数据
        return BaseResult.success(BizDemandCopier.INSTANCE.convert(ResultUtil.pageSuccess(new PageInfo<>(bizDemandVOList))));
    }

    /**
     * 深搜部门树
     *
     * @param node           节点
     * @param deptMap        部门信息id和名称的映射
     * @param queryDeptIdSet 包含的id
     * @param name           部门完整名称
     * @param isInsert       判断是否可直接插入
     */
    void dfsGroupListTree(GroupResponse node, Map<Long, String> deptMap, Set<Long> queryDeptIdSet, String name, Boolean isInsert){
        name = name + node.getGroupName();
        Long deptId = Long.valueOf(node.getGroupId());
        if(isInsert || queryDeptIdSet.contains(deptId)){
            isInsert = true;
            deptMap.put(deptId, name);
        }
        // 如果为叶节点直接返回
        if(node.getChildNode() == null){return;}

        name = name + CommonConstant.JOIN_LINE;
        for (GroupResponse childNode : node.getChildNode()) {
            dfsGroupListTree(childNode, deptMap, queryDeptIdSet, name, isInsert);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> updateStatus(BizDemandUpdateStatusReq bizDemandUpdateStatusReq) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        // 修改业务需求状态 —— 作废
        Long bizDemandId = bizDemandUpdateStatusReq.getBizDemandId();
        BizDemandDO bizDemandDO = bizDemandMapper.selectById(bizDemandId);
        if(bizDemandDO == null){
            throw new BaseBizRuntimeException("不存在该业务需求");
        }

        // 修改业务需求状态
        bizDemandDO.setStatus(BizDemandStatusEnum.INVALID.getCode());
        bizDemandDO.setModifyManId(userInfo.getId());
        bizDemandDO.setModifyMan(userInfo.getAlias());
        bizDemandMapper.update(bizDemandDO);

        // 取消产品关联
        productBizDemandMapper.deleteByBizDemandId(bizDemandId, userInfo.getAlias(), userInfo.getId());

        // 接收人通知
        messageComponent.bizDemandInvalidMsg(
                userInfo.getId(),
                bizDemandDO.getReceiveManId(),
                bizDemandDO.getName()
        );

        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> add(BizDemandAddReq bizDemandAddReq) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        // 判断主题是否唯一
        BizDemandDO checkBizDemandDO = bizDemandMapper.selectByName(bizDemandAddReq.getName());
        if(checkBizDemandDO != null){
            throw new BaseBizRuntimeException("已经有相同的主题名称");
        }

        // 新增业务需求
        BizDemandDO bizDemandDO = BizDemandCopier.INSTANCE.convert(bizDemandAddReq);
        bizDemandDO.setStatus(BizDemandStatusEnum.EVALUATE.getCode());
        bizDemandDO.setCreateMan(userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName());
        bizDemandDO.setCreateManId(userInfo.getId());
        bizDemandMapper.insert(bizDemandDO);

        List<FileAddReq> fileIdList = bizDemandAddReq.getFileList();
        if(!fileIdList.isEmpty()){
            fileComponent.update(fileIdList, bizDemandDO.getId(), FileTypeEnum.BIZ_DEMAND.getCode());
        }

        // 添加抄送人
        List<PersonAddReq> recipientInfoList = bizDemandAddReq.getRecipientInfoList();
        if(!recipientInfoList.isEmpty()){
            personComponent.add(recipientInfoList, bizDemandDO.getId(), PersonTypeEnum.BIZ_DEMAND_CC.getCode());
        }

        // 接收人通知
        messageComponent.bizDemandToReceiveMsg(
                userInfo.getId(),
                bizDemandDO.getReceiveManId(),
                bizDemandDO.getName()
        );

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<BizDemandDetailVO> getBizDemandById(Long bizDemandId) {
        BizDemandDO bizDemandDO = bizDemandMapper.selectById(bizDemandId);
        if(bizDemandDO == null){
            throw new BaseBizRuntimeException("不存在该业务需求");
        }

        // 获取对应附件列表
        List<FileDO> fileDOList = fileComponent.select(bizDemandId, FileTypeEnum.BIZ_DEMAND.getCode());
        List<FileVO> fileVOList = FileCopier.INSTANCE.transform(fileDOList);

        // 获取对应抄送人
        List<PersonDO> personDOList = personComponent.select(bizDemandId, PersonTypeEnum.BIZ_DEMAND_CC.getCode());
        List<PersonVO> personVOList = PersonCopier.INSTANCE.transform(personDOList);

        // 获取对应产品线
        ProductLineDO productLineDO = productLineMapper.selectById(bizDemandDO.getProductLineId());

        // 信息填充
        BizDemandDetailVO bizDemandDetailVO = BizDemandCopier.INSTANCE.convert(bizDemandDO);

        bizDemandDetailVO.setFileList(fileVOList);
        bizDemandDetailVO.setRecipientInfoList(personVOList);

        bizDemandDetailVO.setProductLineName(productLineDO.getName());
        bizDemandDetailVO.setPriorityText(PriorityEnum.getTextByCode(bizDemandDetailVO.getPriority()));
        bizDemandDetailVO.setReasonText(BizDemandReasonEnum.getTextByCode(bizDemandDetailVO.getReason()));
        bizDemandDetailVO.setStatusText(BizDemandStatusEnum.getTextByCode(bizDemandDetailVO.getStatus()));
        bizDemandDetailVO.setPlanReleaseDateText(PlanReleaseDateEnum.getTextByCode(bizDemandDetailVO.getPlanReleaseDate()));

        bizDemandDetailVO.setDeptName(innerGroupClient.getSimpleGroup(bizDemandDO.getDeptId()).getGroupName());

        //获取项目发布时间
        bizDemandDetailVO.setEndDate(getProjectEndDate(bizDemandId));

        return BaseResult.success(bizDemandDetailVO);
    }

    /**
     * 得到业务需求关联的产品需求关联的项目的发布日期
     *
     * @param bizDemandId 业务需求id
     * @return Date
     */
    public Date getProjectEndDate(Long bizDemandId){
        // 获取该业务需求所关联的产品需求
        List<ProductBizDemandDO> productBizDemandDOList = productBizDemandMapper.select(ProductBizDemandCondition.builder()
                .bizDemandId(bizDemandId)
                .isDeleted(false)
                .build());
        List<Long> productDemandIdList = productBizDemandDOList.stream().map(ProductBizDemandDO::getProductDemandId).collect(Collectors.toList());

        // 获取关联的产品需求相关的项目
        List<ProjectDO> projectDOList = projectMapper.selectByProductDemandIdList(productDemandIdList);
        if(projectDOList.isEmpty()){return null;}

        Date result = projectDOList.get(0).getPlanEndDate();
        for (ProjectDO projectDO : projectDOList) {
            Date projectEndDate = projectDO.getActualEndDate() == null? projectDO.getPlanEndDate(): projectDO.getActualEndDate();
            result = result.after(projectEndDate)? result: projectEndDate;
        }
        return result;
    }

    @Override
    public BaseResult<Boolean> modify(BizDemandModifyReq bizDemandModifyReq) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        // 修改业务需求
        BizDemandDO oldBizDemandDO = bizDemandMapper.selectById(bizDemandModifyReq.getId());
        if(oldBizDemandDO == null){
            throw new BaseBizRuntimeException("不存在该业务需求");
        }

        BizDemandDO newBizDemandDO = BizDemandCopier.INSTANCE.convert(bizDemandModifyReq);
        newBizDemandDO.setModifyMan(userInfo.getAlias());
        newBizDemandDO.setModifyManId(userInfo.getId());
        bizDemandMapper.update(newBizDemandDO);

        // 添加抄送人数据
        List<PersonAddReq> recipientInfoList = bizDemandModifyReq.getRecipientInfoList();
        if(!recipientInfoList.isEmpty()){
            personComponent.add(recipientInfoList, bizDemandModifyReq.getId(), PersonTypeEnum.BIZ_DEMAND_CC.getCode());
        }

        // 添加附件
        List<FileAddReq> fileIdList = bizDemandModifyReq.getFileList();
        if(!fileIdList.isEmpty()){
            fileComponent.update(fileIdList, bizDemandModifyReq.getId(), FileTypeEnum.BIZ_DEMAND.getCode());
        }

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> agree(BizDemandAgreeReq bizDemandAgreeReq) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        // 修改业务需求状态 —— 接收，添加预期上线时间
        Long bizDemandId = bizDemandAgreeReq.getBizDemandId();
        Integer planReleaseDate = bizDemandAgreeReq.getPlanReleaseDate();

        BizDemandDO bizDemandDO = bizDemandMapper.selectById(bizDemandId);
        if(bizDemandDO == null){
            throw new BaseBizRuntimeException("不存在该业务需求");
        }

        bizDemandDO.setStatus(BizDemandStatusEnum.RECEIVED.getCode());
        bizDemandDO.setPlanReleaseDate(planReleaseDate);
        bizDemandDO.setModifyMan(userInfo.getAlias());
        bizDemandDO.setModifyManId(userInfo.getId());
        bizDemandMapper.update(bizDemandDO);

        // 通知需求提交人
        messageComponent.bizDemandReceivedMsg(
                userInfo.getId(),
                bizDemandDO.getCreateManId(),
                bizDemandDO.getName(),
                PlanReleaseDateEnum.getTextByCode(bizDemandDO.getPlanReleaseDate())
        );

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> reject(BizDemandRejectReq bizDemandRejectReq) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        // 修改业务需求状态 —— 驳回，添加驳回原因
        Long bizDemandId = bizDemandRejectReq.getBizDemandId();
        Integer reason = bizDemandRejectReq.getReason();

        BizDemandDO bizDemandDO = bizDemandMapper.selectById(bizDemandId);
        if(bizDemandDO == null){
            throw new BaseBizRuntimeException("不存在该业务需求");
        }

        bizDemandDO.setReason(reason);
        bizDemandDO.setPlanReleaseDate(null);
        bizDemandDO.setStatus(BizDemandStatusEnum.REJECT.getCode());
        bizDemandDO.setModifyMan(userInfo.getAlias());
        bizDemandDO.setModifyManId(userInfo.getId());
        bizDemandMapper.update(bizDemandDO);

        // 驳回通知
        messageComponent.bizDemandRejectMsg(
                userInfo.getId(),
                bizDemandDO.getCreateManId(),
                bizDemandDO.getName(),
                BizDemandReasonEnum.getTextByCode(bizDemandDO.getReason())
        );
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> transfer(BizDemandTransferReq bizDemandTransferReq) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        // 转交：修改接收人
        BizDemandDO bizDemandDO = bizDemandMapper.selectById(bizDemandTransferReq.getId());
        if(bizDemandDO == null){
            throw new BaseBizRuntimeException("不存在该业务需求");
        }

        bizDemandDO.setReceiveMan(bizDemandTransferReq.getReceiveMan());
        bizDemandDO.setReceiveManId(bizDemandTransferReq.getReceiveManId());
        bizDemandDO.setModifyMan(userInfo.getAlias());
        bizDemandDO.setModifyManId(userInfo.getId());
        bizDemandMapper.update(bizDemandDO);

        // 转交人通知（待实现）
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> testNotice(Integer type) {
        if(type == 1){
            Date date = new Date();
            SimpleDateFormat sdf = new SimpleDateFormat("yyMMddE HH:mm:ss");
            System.out.println(sdf.format(DateUtil.getStartOfDay(date)));
            System.out.println(sdf.format(DateUtil.getEndOfDay(date)));
        }else{
            System.out.println(innerGroupClient.getAllSubSimpleGroupList(1L));
        }
        return BaseResult.success(true);
    }

}
