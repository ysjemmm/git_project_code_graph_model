package com.timevale.forward.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.BizDomainCondition;
import com.timevale.forward.dal.dao.BizDomainMapper;
import com.timevale.forward.dal.dao.ProductLineMapper;
import com.timevale.forward.dal.entity.BizDomainDO;
import com.timevale.forward.dal.entity.ProductLineDO;
import com.timevale.forward.facade.api.client.BizDomainService;
import com.timevale.forward.facade.api.query.BizDomainQueryList;
import com.timevale.forward.facade.api.request.BizDomainAddReq;
import com.timevale.forward.facade.api.request.BizDomainModifyReq;
import com.timevale.forward.facade.api.result.BizDomainVO;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.BizDomainCopier;
import com.timevale.forward.service.job.InnerProjectRiskBackJob;
import com.timevale.forward.service.job.InnerProjectRiskJob;
import com.timevale.forward.service.mq.listener.DrcRiskListener;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.aop.LogPoint;
import com.timevale.framework.mq.client.producer.Msg;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.assertj.core.util.Lists;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author by YangXu
 * @date 2021/12/13 17:10
 */
@Slf4j
@LogPoint
@RestService
public class BizDomainServiceImpl implements BizDomainService {

    @Resource
    private BizDomainMapper bizDomainMapper;
    @Resource
    private ProductLineMapper productLineMapper;

    @Resource
    private DrcRiskListener riskListener;
    @Resource
    private InnerProjectRiskJob innerProjectRiskJob;
    @Resource
    private InnerProjectRiskBackJob innerProjectRiskBackJob;

    @Override
    public BaseResult<List<BizDomainVO>> bizDomainList(Integer type) {
        try {
            if (type == 0) {
                innerProjectRiskJob.execute("");
            } else if (type == 1) {
                innerProjectRiskBackJob.execute("");
            } else {
                String message = "{\"action\":\"INSERT\",\"dbName\":\"info_forward\",\"tableName\":\"task\",\"rowKey\":\"id\",\"after\":{\"create_man_id\":\"yangxu\",\"modify_man\":\"\",\"todo\":\"0\",\"is_deleted\":\"0\",\"stage\":\"14\",\"project_id\":\"3082\",\"product_line_id\":\"0\",\"create_man\":\"杨絮-蔡炳旭\",\"modify_man_id\":\"\",\"name\":\"杨絮里程碑风险-任务6\",\"plan_end_date\":\"2023-02-02 09:00:00\",\"id\":\"4229\",\"plan_use_time\":\"8.0\",\"create_date\":\"2023-02-11 21:55:50\",\"plan_start_date\":\"2023-02-01 09:00:00\",\"modify_date\":\"2023-02-11 21:55:50\",\"status\":\"0\",\"desc\":\"\"},\"content\":{\"create_man_id\":\"yangxu\",\"modify_man\":\"\",\"todo\":\"0\",\"is_deleted\":\"0\",\"stage\":\"14\",\"project_id\":\"3082\",\"product_line_id\":\"0\",\"create_man\":\"杨絮-蔡炳旭\",\"modify_man_id\":\"\",\"name\":\"杨絮里程碑风险-任务6\",\"plan_end_date\":\"2023-02-02 09:00:00\",\"id\":\"4229\",\"plan_use_time\":\"8.0\",\"create_date\":\"2023-02-11 21:55:50\",\"plan_start_date\":\"2023-02-01 09:00:00\",\"modify_date\":\"2023-02-11 21:55:50\",\"status\":\"0\",\"desc\":\"\"},\"gtId\":\"74ce2a37-03bd-11e9-bb3e-7cd30ae3fe5c:1-141639153,48782717-9bd8-11e8-95c3-6c92bf21bbbd:1-22460378,4e3863e2-5a65-11ec-8050-98039b9cee40:1-4888966419,4e74b1c7-f87c-11e8-b1db-008cfac1c3c4:1-1817190,d3f9f7ad-a17c-11ea-8539-7cd30ae00680:1-282505862,fbbea55f-a17c-11ea-853a-7cd30adae8ac:1-1741140951,9042d989-5a64-11ec-804b-0c42a1b78f5e:1-684761086,d22f796d-72da-11e9-8fce-7cd30adfe86a:1-151622494\",\"originalSql\":\"\"}";
                Msg msg = new Msg();
                msg.setMsgId("1");
                msg.setBody(message.getBytes());

                riskListener.receive(Lists.newArrayList(msg));
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        List<BizDomainVO> result = BizDomainCopier.INSTANCE.convert(bizDomainMapper.selectAllBizDomain());
        return BaseResult.success(result);
    }

    @Override
    public BaseResult<PageQueryResult<BizDomainVO>> bizDomainList(BizDomainQueryList bizDomainQueryList) {
        BizDomainCondition condition = BizDomainCopier.INSTANCE.convert(bizDomainQueryList);
        PageHelper.startPage(bizDomainQueryList.pageNum, bizDomainQueryList.pageSize, CommonConstant.DEFAULT_ORDER_BY);

        List<BizDomainDO> bizDomainDOList = bizDomainMapper.selectByCondition(condition);
        List<BizDomainVO> bizDomainVOList = BizDomainCopier.INSTANCE.convert(bizDomainDOList);
        PageInfo<BizDomainDO> pageInfo = new PageInfo<>(bizDomainDOList);
        PageQueryResult<BizDomainVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(bizDomainVOList);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);
        return BaseResult.success(pageQueryResult);
    }

    @Override
    public BaseResult<Boolean> add(BizDomainAddReq bizDomainAddReq) {
        BizDomainDO bizDomainDO = BizDomainCopier.INSTANCE.convert(bizDomainAddReq);
        bizDomainMapper.insert(bizDomainDO);
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> update(BizDomainModifyReq bizDomainModifyReq) {
        BizDomainDO bizDomainDO = BizDomainCopier.INSTANCE.convert(bizDomainModifyReq);
        bizDomainMapper.update(bizDomainDO);
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> delOrUnDelete(Long bizDomainId) {
        BizDomainDO bizDomainDO = bizDomainMapper.selectById(bizDomainId);
        if(!bizDomainDO.getIsDeleted()){
            List<ProductLineDO> productLineDOList = productLineMapper.getBizDomainId(bizDomainId);
            if(CollectionUtils.isNotEmpty(productLineDOList)){
                List<String> names = productLineDOList.stream().map(ProductLineDO::getName).collect(Collectors.toList());
                throw new BaseBizRuntimeException("该业务域下存在产品线:"+names+",不能删除");
            }
            bizDomainDO.setIsDeleted(true);
            bizDomainMapper.update(bizDomainDO);
            return BaseResult.success(true);
        }
        //恢复
        bizDomainDO.setIsDeleted(false);
        bizDomainMapper.update(bizDomainDO);
        return BaseResult.success(true);
    }
}
