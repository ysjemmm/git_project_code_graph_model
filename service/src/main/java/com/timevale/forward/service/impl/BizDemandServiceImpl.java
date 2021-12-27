package com.timevale.forward.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.BizDemandListCondition;
import com.timevale.forward.dal.dao.BizDemandMapper;
import com.timevale.forward.dal.dao.ProductBizDemandMapper;
import com.timevale.forward.dal.dao.ProductLineMapper;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.facade.api.client.BizDemandService;
import com.timevale.forward.facade.api.query.BizDemandQueryList;
import com.timevale.forward.facade.api.request.BizDemandAddReq;
import com.timevale.forward.facade.api.request.BizDemandModifyReq;
import com.timevale.forward.facade.api.request.BizDemandTransferReq;
import com.timevale.forward.facade.api.request.FileAddReq;
import com.timevale.forward.facade.api.result.BizDemandDetailVO;
import com.timevale.forward.facade.api.result.BizDemandVO;
import com.timevale.forward.facade.api.result.FileVO;
import com.timevale.forward.facade.api.result.PersonVO;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.service.component.FileComponent;
import com.timevale.forward.service.component.PersonComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.BizDemandCopier;
import com.timevale.forward.service.copy.FileCopier;
import com.timevale.forward.service.copy.PersonCopier;
import com.timevale.forward.service.integration.erp.ErpMessageClient;
import com.timevale.forward.service.integration.erp.model.ActionCardMsg;
import com.timevale.forward.service.integration.erp.model.MarkdownMsg;
import com.timevale.forward.service.integration.inneruser.InnerGroupClient;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
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
    ProductBizDemandMapper productBizDemandMapper;

    @Resource
    ProductLineMapper productLineMapper;

    @Resource
    ErpMessageClient erpMessageClient;

    @Resource
    InnerUserPersonClient innerUserPersonClient;

    @Resource
    InnerGroupClient innerGroupClient;

    @Resource
    PersonComponent personComponent;

    @Resource
    FileComponent fileComponent;


    @Override
    public BaseResult<PageQueryResult<BizDemandVO>> list(BizDemandQueryList bizDemandQueryList) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        // 开始分页
        PageHelper.startPage(bizDemandQueryList.pageNum, bizDemandQueryList.pageSize);

        // 转换查询条件,根据tabs添加不同的效果
        BizDemandListCondition bizDemandListCondition = BizDemandCopier.INSTANCE.convert(bizDemandQueryList);

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

        // 查询并转换
        List<BizDemandListDO> bizDemandListDOList = bizDemandMapper.selectList(bizDemandListCondition);
        List<BizDemandVO> bizDemandVOList = BizDemandCopier.INSTANCE.convert(bizDemandListDOList);

        // 查询部门信息
        List<Long> deptIdList = bizDemandVOList.stream().map(BizDemandVO::getDeptId).collect(Collectors.toList());
        Map<Long, String> groupInfo = innerGroupClient.batchGetSimpleGroupMap(deptIdList);

        bizDemandVOList.forEach( iter -> {
            iter.setPriorityText(PriorityEnum.getTextByCode(iter.getPriority()));
            iter.setStatusText(BizDemandStatusEnum.getTextByCode(iter.getStatus()));
            iter.setPlanReleaseDateText(PlanReleaseDateEnum.getTextByCode(iter.getPlanReleaseDate()));
            iter.setDeptName(groupInfo.get(iter.getDeptId()));
        });

        // 转换后返回数据
        return BaseResult.success(BizDemandCopier.INSTANCE.convert(ResultUtil.pageSuccess(new PageInfo<>(bizDemandVOList))));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> updateStatus(Long bizDemandId) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        // 修改业务需求状态 —— 作废
        BizDemandDO bizDemandDO = bizDemandMapper.selectById(bizDemandId);
        if(bizDemandDO == null){
            throw new BaseBizRuntimeException("不存在该业务需求");
        }

        // 修改业务需求状态
        bizDemandDO.setStatus(BizDemandStatusEnum.INVALID.getCode());
        bizDemandDO.setModifyMan(userInfo.getAlias());
        bizDemandDO.setModifyManId(userInfo.getId());
        bizDemandMapper.update(bizDemandDO);

        // 取消产品关联
        productBizDemandMapper.deleteByBizDemandId(bizDemandId, userInfo.getAlias(), userInfo.getId());

        // 接收人通知（待实现）
        /*String name = bizDemandDO.getName();
        String createMan = userInfo.getAlias();
        String title = MessageTitleEnum.BIZDEMAND_INVALID.getText();
        List<String> receivers = Lists.newArrayList(bizDemandDO.getReceiveManId());

        String markdown = String.format("%s作废了业务需求：%s", createMan, name);

        ActionCardMsg actionCardMsg = ActionCardMsg.builder()
                .title(title)
                .markdown(markdown)
                .singleTitle("跳转连接")
                .singleUrl("https://www.baidu.com/")
                .receivers(receivers)
                .build();
        erpMessageClient.sendActionCardMsg(actionCardMsg);
*/
        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> add(BizDemandAddReq bizDemandAddReq) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        // 新增业务需求
        BizDemandDO bizDemandDO = BizDemandCopier.INSTANCE.convert(bizDemandAddReq);
        bizDemandDO.setStatus(BizDemandStatusEnum.EVALUATE.getCode());
        bizDemandDO.setCreateMan(userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName());
        bizDemandDO.setCreateManId(userInfo.getId());
        bizDemandMapper.insert(bizDemandDO);

        List<FileAddReq> fileIdList = bizDemandAddReq.getFileList();
        fileComponent.update(fileIdList, bizDemandDO.getId(), FileTypeEnum.BIZ_DEMAND.getCode());

        // 添加抄送人
        personComponent.add(bizDemandAddReq.getRecipientInfoList(), bizDemandDO.getId(), PersonTypeEnum.BIZ_DEMAND_CC.getCode());

        // 接收人通知（待实现）
       /* String createMan = userInfo.getAlias();
        String name = bizDemandAddReq.getName();
        String title = MessageTitleEnum.BIZDEMAND_RECEIVE.getText();
        List<String> receivers = Lists.newArrayList(bizDemandAddReq.getReceiveManInfo().getUserId());

        String markdown = String.format("您收到了%s提交的业务需求：%s，可进入产研项目管理系统查看", createMan, name);

        ActionCardMsg actionCardMsg = ActionCardMsg.builder()
                .title(title)
                .markdown(markdown)
                .singleTitle("跳转连接")
                .singleUrl("https://www.baidu.com/")
                .receivers(receivers)
                .build();
        erpMessageClient.sendActionCardMsg(actionCardMsg);*/

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

        return BaseResult.success(bizDemandDetailVO);
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

        // 筛出新增抄送人，添加抄送人数据
        personComponent.add(bizDemandModifyReq.getRecipientInfoList(), newBizDemandDO.getId(), PersonTypeEnum.BIZ_DEMAND_CC.getCode());

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> agree(Long bizDemandId, Integer planReleaseDate) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        // 修改业务需求状态 —— 接收，添加预期上线时间
        BizDemandDO bizDemandDO = bizDemandMapper.selectById(bizDemandId);
        if(bizDemandDO == null){
            throw new BaseBizRuntimeException("不存在该业务需求");
        }

        bizDemandDO.setStatus(BizDemandStatusEnum.RECEIVED.getCode());
        bizDemandDO.setPlanReleaseDate(planReleaseDate);
        bizDemandDO.setModifyMan(userInfo.getAlias());
        bizDemandDO.setModifyManId(userInfo.getId());
        bizDemandMapper.update(bizDemandDO);

        // 通知需求提交人（待实现）
        /*String name = bizDemandDO.getName();
        String receiver = bizDemandDO.getReceiveMan();
        String title = MessageTitleEnum.BIZDEMAND_FEEDBACK.getText();
        String text = PlanReleaseDateEnum.getTextByCode(planReleaseDate);
        List<String> receivers = Lists.newArrayList(bizDemandDO.getCreateManId());

        String markdown = String.format("%s接收了您提交的业务需求：%s，预期上线时间为%s，可进入产研项目管理系统查看", receiver, name, text);

        ActionCardMsg actionCardMsg = ActionCardMsg.builder()
                .title(title)
                .markdown(markdown)
                .singleTitle("跳转连接")
                .singleUrl("https://www.baidu.com/")
                .receivers(receivers)
                .build();
        erpMessageClient.sendActionCardMsg(actionCardMsg);*/

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> reject(Long bizDemandId, Integer reason) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        // 修改业务需求状态 —— 驳回，添加驳回原因
        BizDemandDO bizDemandDO = bizDemandMapper.selectById(bizDemandId);
        if(bizDemandDO == null){
            throw new BaseBizRuntimeException("不存在该业务需求");
        }

        bizDemandDO.setStatus(BizDemandStatusEnum.REJECT.getCode());
        bizDemandDO.setReason(reason);
        bizDemandDO.setModifyMan(userInfo.getAlias());
        bizDemandDO.setModifyManId(userInfo.getId());
        bizDemandMapper.update(bizDemandDO);

        // 驳回通知（待实现）
        /*String name = bizDemandDO.getName();
        String receiver = bizDemandDO.getReceiveMan();
        String reasonText = BizDemandReasonEnum.getTextByCode(reason);
        List<String> receivers = Lists.newArrayList(bizDemandDO.getCreateManId());

        String markdown = String.format("%s驳回了您提交的业务需求：%s，驳回理由是%s，可进入产研项目管理系统查看", receiver, name, reasonText);

        ActionCardMsg actionCardMsg = ActionCardMsg.builder()
                .title(MessageTitleEnum.BIZDEMAND_FEEDBACK.getText())
                .markdown(markdown)
                .singleTitle("跳转连接")
                .singleUrl("https://www.baidu.com/")
                .receivers(receivers)
                .build();
        erpMessageClient.sendActionCardMsg(actionCardMsg);
*/
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
        if(type.equals(1)){
            erpMessageClient.sendMarkdownMsg(MarkdownMsg.builder()
                    .receivers(Lists.newArrayList("wangxuan"))
                    .title("test")
                    .content("测试内容")
                    .build());
        }else{
            erpMessageClient.sendActionCardMsg(ActionCardMsg.builder()
                    .receivers(Lists.newArrayList("yangxu"))
                    .title("test2")
                    .markdown("测试内容2")
                    .singleTitle("跳转连接文案")
                    .singleUrl("https://weibo.com/")
                    .build());
        }
        return BaseResult.success(true);
    }

}
