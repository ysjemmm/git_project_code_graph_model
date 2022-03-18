package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.ProductLineMapper;
import com.timevale.forward.dal.dao.TroubleTicketMapper;
import com.timevale.forward.dal.entity.FileDO;
import com.timevale.forward.dal.entity.ProductLineDO;
import com.timevale.forward.dal.entity.TroubleTicketDO;
import com.timevale.forward.facade.api.client.TroubleTicketService;
import com.timevale.forward.facade.api.query.TroubleTicketQueryList;
import com.timevale.forward.facade.api.request.FileAddReq;
import com.timevale.forward.facade.api.request.TroubleTicketAddReq;
import com.timevale.forward.facade.api.request.TroubleTicketDeleteReq;
import com.timevale.forward.facade.api.request.TroubleTicketModifyReq;
import com.timevale.forward.facade.api.result.FileVO;
import com.timevale.forward.facade.api.result.TroubleTicketDetailVO;
import com.timevale.forward.facade.api.result.TroubleTicketVO;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.service.component.BizDemandComponent;
import com.timevale.forward.service.component.FileComponent;
import com.timevale.forward.service.copy.FileCopier;
import com.timevale.forward.service.copy.TroubleTicketCopier;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.security.facade.response.GroupResponse;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * @author by YangXu
 * @date 2022/03/16 17:54
 */
@Slf4j
@RestService
public class TroubleTicketServiceImpl implements TroubleTicketService {

    @Resource
    private TroubleTicketMapper troubleTicketMapper;

    @Resource
    private FileComponent fileComponent;

    @Resource
    private ProductLineMapper productLineMapper;

    @Resource
    private BizDemandComponent bizDemandComponent;

    @Override
    public BaseResult<Boolean> add(TroubleTicketAddReq troubleTicketAddReq) {
        // 转换后行插入数据
        TroubleTicketDO troubleTicketDO = TroubleTicketCopier.INSTANCE.convert(troubleTicketAddReq);
        troubleTicketMapper.insert(troubleTicketDO);

        // 添加附件
        List<FileAddReq> fileList = troubleTicketAddReq.getFileList();
        fileComponent.add(fileList, troubleTicketDO.getId(), FileTypeEnum.TROUBLE_TICKET.getCode());

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> modify(TroubleTicketModifyReq troubleTicketModifyReq) {
        Long id = troubleTicketModifyReq.getId();
        TroubleTicketDO oldTroubleTicketDO = troubleTicketMapper.selectById(id);
        if(oldTroubleTicketDO == null){
            throw new BaseBizRuntimeException("不存在对应的故障工单");
        }

        // 故障单信息
        TroubleTicketDO newTroubleTicketDO = TroubleTicketCopier.INSTANCE.convert(troubleTicketModifyReq);
        troubleTicketMapper.update(newTroubleTicketDO);

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

        // 逻辑删除
        troubleTicketDO.setIsDeleted(true);
        troubleTicketMapper.update(troubleTicketDO);

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<List<TroubleTicketVO>> list(TroubleTicketQueryList troubleTicketQueryList) {
        return null;
    }
}
