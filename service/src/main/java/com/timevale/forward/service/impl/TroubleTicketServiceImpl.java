package com.timevale.forward.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.PersonListCondition;
import com.timevale.forward.dal.condition.TroubleTicketCondition;
import com.timevale.forward.dal.dao.ImprovementMeasureMapper;
import com.timevale.forward.dal.dao.PersonMapper;
import com.timevale.forward.dal.dao.ProductLineMapper;
import com.timevale.forward.dal.dao.TroubleTicketMapper;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.facade.api.client.TroubleTicketService;
import com.timevale.forward.facade.api.query.TroubleTicketQueryList;
import com.timevale.forward.facade.api.request.*;
import com.timevale.forward.facade.api.result.*;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.service.component.BizDemandComponent;
import com.timevale.forward.service.component.FileComponent;
import com.timevale.forward.service.component.ImprovementMeasureComponent;
import com.timevale.forward.service.component.SqlOrderComponent;
import com.timevale.forward.service.component.impl.PersonComponentImpl;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.FileCopier;
import com.timevale.forward.service.copy.PersonCopier;
import com.timevale.forward.service.copy.TroubleTicketCopier;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.aop.LogPoint;
import com.timevale.forward.service.utils.date.DateUtil;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.base.util.CollectionUtils;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import com.timevale.security.facade.response.GroupResponse;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.assertj.core.util.Sets;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author by YangXu
 * @date 2022/03/16 17:54
 */
@Slf4j
@LogPoint
@RestService
public class TroubleTicketServiceImpl implements TroubleTicketService {

    @Resource
    private TroubleTicketMapper troubleTicketMapper;
    @Resource
    private FileComponent fileComponent;
    @Resource
    private ImprovementMeasureMapper improvementMeasureMapper;
    @Resource
    private ImprovementMeasureComponent improvementMeasureComponent;
    @Resource
    private PersonComponentImpl personComponent;
    @Resource
    private PersonMapper personMapper;
    @Resource
    private ProductLineMapper productLineMapper;
    @Resource
    private BizDemandComponent bizDemandComponent;
    @Resource
    private SqlOrderComponent sqlOrderComponent;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> add(TroubleTicketAddReq troubleTicketAddReq) {

        if(troubleTicketAddReq.getName().contains(CommonConstant.BLANK)){
            throw new BaseBizRuntimeException("故障单名称中请勿包含空格");
        }

        // 转换后行插入数据
        TroubleTicketDO troubleTicketDO = TroubleTicketCopier.INSTANCE.convert(troubleTicketAddReq);
        troubleTicketMapper.insert(troubleTicketDO);

        // 添加改进措施
        List<ImprovementMeasureAddReq> improvementMeasureAddReqList = troubleTicketAddReq.getImprovementMeasureAddReqList();
        improvementMeasureAddReqList.forEach(e -> e.setTroubleTicketId(troubleTicketDO.getId()));
        improvementMeasureAddReqList.forEach(e -> improvementMeasureComponent.add(e));

        // 添加处理人
        List<PersonAddReq> handlerList = troubleTicketAddReq.getHandlerList();
        personComponent.add(handlerList, troubleTicketDO.getId(), PersonTypeEnum.TROUBLE_TICKET_HANDLER.getCode());

        // 添加附件
        List<FileAddReq> fileList = troubleTicketAddReq.getFileList();
        fileComponent.add(fileList, troubleTicketDO.getId(), FileTypeEnum.TROUBLE_TICKET.getCode());

        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> modify(TroubleTicketModifyReq troubleTicketModifyReq) {

        Long id = troubleTicketModifyReq.getId();
        TroubleTicketDO oldTroubleTicketDO = troubleTicketMapper.selectById(id);
        if(oldTroubleTicketDO == null){
            throw new BaseBizRuntimeException("不存在对应的故障工单");
        }

        // 故障单信息
        TroubleTicketDO newTroubleTicketDO = TroubleTicketCopier.INSTANCE.convert(troubleTicketModifyReq);
        troubleTicketMapper.allUpdate(newTroubleTicketDO);

        // 修改处理人
        List<PersonAddReq> handlerList = troubleTicketModifyReq.getHandlerList();
        personComponent.update(handlerList, newTroubleTicketDO.getId(), PersonTypeEnum.TROUBLE_TICKET_HANDLER.getCode());

        // 更新附件信息
        List<FileAddReq> fileIdList = troubleTicketModifyReq.getFileList();
        fileComponent.update(fileIdList, id, FileTypeEnum.TROUBLE_TICKET.getCode());

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<TroubleTicketDetailVO> get(Long troubleTicketId) {
        // 读取数据，判断是否存在
        TroubleTicketDO troubleTicketDO = troubleTicketMapper.selectById(troubleTicketId);
        if(troubleTicketDO == null){
            throw new BaseBizRuntimeException("不存在对应的故障工单");
        }
        ProductLineDO productLineDO = productLineMapper.selectById(troubleTicketDO.getProductLineId());
        if(productLineDO == null){
            throw new BaseBizRuntimeException("不存在对应的产品线");
        }

        // 转换格式
        TroubleTicketDetailVO ticketDetailVO = TroubleTicketCopier.INSTANCE.convert(troubleTicketDO);

        // 填充描述数据
        ticketDetailVO.setProductLineName(productLineDO.getName());
        ticketDetailVO.setTypeName(TroubleTicketTypeEnum.getTextByCode(ticketDetailVO.getType()));
        ticketDetailVO.setReasonName(TroubleTicketReasonEnum.getTextByCode(troubleTicketDO.getReason()));
        ticketDetailVO.setTroubleRankName(TroubleTicketRankEnum.getTextByCode(ticketDetailVO.getTroubleRank()));
        ticketDetailVO.setDuringTimeName(TroubleTicketDuringTimeEnum.getTextByCode(ticketDetailVO.getDuringTime()));
        ticketDetailVO.setInfluenceScopeName(TroubleTicketInfluenceScopeEnum.getTextByCode(ticketDetailVO.getInfluenceScope()));

        // 获取部门链，获得部门完整链名
        String deptChainName = bizDemandComponent.getDeptChainName(ticketDetailVO.getDutyTeam());
        ticketDetailVO.setDutyTeamName(deptChainName);

        // 添加处理人信息
        List<PersonDO> handlerDOList = personComponent.select(troubleTicketId, PersonTypeEnum.TROUBLE_TICKET_HANDLER.getCode());
        List<PersonVO> handlerVOList = PersonCopier.INSTANCE.transform(handlerDOList);
        ticketDetailVO.setHandlerList(handlerVOList);

        // 添加附件信息
        List<FileDO> fileDOList = fileComponent.select(troubleTicketId, FileTypeEnum.TROUBLE_TICKET.getCode());
        List<FileVO> fileVOList = FileCopier.INSTANCE.transform(fileDOList);
        ticketDetailVO.setFileVOList(fileVOList);

        return BaseResult.success(ticketDetailVO);
    }

    @Override
    public BaseResult<Boolean> delete(TroubleTicketDeleteReq troubleTicketDeleteReq) {
        Long id = troubleTicketDeleteReq.getId();
        TroubleTicketDO troubleTicketDO = troubleTicketMapper.selectById(id);
        if(troubleTicketDO == null){
            throw new BaseBizRuntimeException("不存在对应的故障工单");
        }

        // 删除对应的改进措施和待办
        List<ImprovementMeasureDO> improvementMeasureDOList = improvementMeasureMapper.selectByTroubleTicketId(id);
        improvementMeasureDOList.parallelStream().forEach(e -> {
            improvementMeasureComponent.delete(e.getId());
            improvementMeasureComponent.deleteTodoTask(e);
        });

        // 逻辑删除
        troubleTicketDO.setIsDeleted(true);
        troubleTicketMapper.update(troubleTicketDO);

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<PageQueryResult<TroubleTicketVO>> list(TroubleTicketQueryList troubleTicketQueryList) {
        String userId = LocalSessionUtils.getUserInfo().getId();

        // 条件转换
        TroubleTicketCondition troubleTicketCondition = TroubleTicketCopier.INSTANCE.convert(troubleTicketQueryList);

        // 日期处理
        troubleTicketCondition.setOccurrenceTimeStart(DateUtil.getStartOfDay(troubleTicketCondition.getOccurrenceTimeStart()));
        troubleTicketCondition.setOccurrenceTimeEnd(DateUtil.getEndOfDay(troubleTicketCondition.getOccurrenceTimeEnd()));

        // tab页面条件
        String ascription = troubleTicketQueryList.getAscription();
        if(AscriptionEnum.CURRENT_USER.toString().equals(ascription)){
            troubleTicketCondition.setCreateMandIdList(Lists.newArrayList(userId));
        }else if(AscriptionEnum.RECEIVE.toString().equals(ascription)){
            troubleTicketQueryList.getHandlerIdList().add(userId);
        }

        // 添加处理人判断
        List<PersonDO> personDOList = personMapper.select(PersonListCondition.builder()
                .type(PersonTypeEnum.TROUBLE_TICKET_HANDLER.getCode())
                .build());

        List<String> handlerIdList = troubleTicketQueryList.getHandlerIdList();
        if(!CollectionUtils.isEmpty(handlerIdList)){
            Set<String> handlerIdSet = new HashSet<>(handlerIdList);
            List<Long> troubleTicketIdList = personDOList.stream()
                    .filter(e -> handlerIdSet.contains(e.getUserId()))
                    .map(PersonDO::getMainId)
                    .distinct()
                    .collect(Collectors.toList());
            if(CollectionUtils.isEmpty(troubleTicketIdList)){
                return BaseResult.success(ResultUtil.pageEmpty());
            }
            troubleTicketCondition.setTroubleTicketIdList(troubleTicketIdList);
        }

        // 故障定级-未定级,特殊处理
        List<Integer> troubleRankList = troubleTicketCondition.getTroubleRankList();
        boolean contain = CollectionUtils.isNotEmpty(troubleRankList) &&  troubleRankList.contains(TroubleTicketRankEnum.UN_CERTAIN.getCode());
        troubleTicketCondition.setTroubleRankIsNull(contain);

        Map<Long, GroupResponse> deptNodeMap = new HashMap<>();
        Set<Long> queryDeptIdSet = Sets.newHashSet(troubleTicketCondition.getDutyTeamList());

        // 如果查询条件有部门id，收集子部门id及所需部门的完整名
        if (CollectionUtils.isNotEmpty(queryDeptIdSet)) {
            deptNodeMap = bizDemandComponent.getGroupListTreeMap(new ArrayList<>(queryDeptIdSet));
            // 替换查询部门id条件
            troubleTicketCondition.setDutyTeamList(Lists.newArrayList(deptNodeMap.keySet()));
        }

        // 列表排序规则
        String collation;
        String orderFiled = troubleTicketQueryList.getOrderFiled();
        Integer orderCollation = troubleTicketQueryList.getOrderCollation();
        if (Objects.equals(orderFiled, "troubleRank")) {
            collation = "IF(trouble_rank IS NULL, 200, IF(trouble_rank < 0, -10 * trouble_rank, trouble_rank))";
            if (Objects.equals(OrderCollationEnum.DESC.getCode(), orderCollation)) {
                collation += "desc";
            }
            collation += ", modify_date desc, id desc";
        } else {
            collation = sqlOrderComponent.build(orderFiled, orderCollation);
        }

        // 分页查询
        PageHelper.startPage(troubleTicketQueryList.pageNum, troubleTicketQueryList.pageSize, collation);
        List<TroubleTicketListDO> troubleTicketDOList = troubleTicketMapper.selectList(troubleTicketCondition);

        if(CollectionUtils.isEmpty(troubleTicketDOList)){
            return BaseResult.success(ResultUtil.pageEmpty());
        }

        // 如果查询条件没有部门id，收集完整名
        if (CollectionUtils.isEmpty(queryDeptIdSet)) {
            queryDeptIdSet.addAll(troubleTicketDOList.stream().map(TroubleTicketListDO::getDutyTeam).collect(Collectors.toList()));
            deptNodeMap = bizDemandComponent.getGroupListTreeMap(Lists.newArrayList(queryDeptIdSet));
        }

        // 结果集转换
        List<TroubleTicketVO> troubleTicketVOList = troubleTicketDOList.stream()
                .map(TroubleTicketCopier.INSTANCE::convert).collect(Collectors.toList());

        // 查询处理人
        Map<Long, List<PersonDO>> personMap = personDOList.stream().collect(Collectors.groupingBy(PersonDO::getMainId));
        troubleTicketVOList.forEach(e -> {
            List<PersonDO> handlerDOList = personMap.get(e.getId());
            List<PersonVO> handlerVOList = PersonCopier.INSTANCE.transform(handlerDOList);
            e.setHandlerList(handlerVOList);
        });

        // 描述数据填充
        for (TroubleTicketVO e : troubleTicketVOList) {
            e.setIsMonitorDetectText(YesOrNoEnum.getTextByCode(e.getIsMonitorDetect()));
            e.setTroubleRankName(TroubleTicketRankEnum.getTextByCode(e.getTroubleRank()));

            if(e.getDutyTeam() == null){
                continue;
            }
            GroupResponse response = deptNodeMap.get(e.getDutyTeam());
            if (response == null) {
                e.setDutyTeamName("");
                log.info("没有找到部门,id为:{}", e.getDutyTeam());
            } else {
                e.setDutyTeamName(response.getGroupName());
                e.setDutyTeamFlag(response.getDeleteFlag());
            }
        }

        // 返回分页数据
        PageInfo<TroubleTicketListDO> pageInfo = new PageInfo<>(troubleTicketDOList);
        PageQueryResult<TroubleTicketVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(troubleTicketVOList);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);

        return BaseResult.success(pageQueryResult);
    }
}
