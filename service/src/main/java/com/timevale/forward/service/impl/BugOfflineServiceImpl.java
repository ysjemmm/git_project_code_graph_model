package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.PersonMapper;
import com.timevale.forward.dal.dao.ProductLineMapper;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.entity.BugOfflineDO;
import com.timevale.forward.facade.api.client.BugOfflineService;
import com.timevale.forward.facade.api.query.BugOfflineQueryList;
import com.timevale.forward.facade.api.request.BugOfflineAddReq;
import com.timevale.forward.facade.api.request.BugOfflineModifyReq;
import com.timevale.forward.facade.api.result.BugOfflineDetailVO;
import com.timevale.forward.facade.api.result.BugOfflineVO;
import com.timevale.forward.model.enums.AscriptionEnum;
import com.timevale.forward.service.component.FileComponent;
import com.timevale.forward.service.component.PersonComponent;
import com.timevale.forward.service.component.TaskComponent;
import com.timevale.forward.service.copy.BugOfflineCopier;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Slf4j
@RestService
public class BugOfflineServiceImpl implements BugOfflineService {

    @Resource
    private TaskComponent taskComponent;

    @Resource
    private InnerUserPersonClient innerUserPersonClient;

    @Resource
    private PersonMapper personMapper;

    @Resource
    private ProductLineMapper productLineMapper;

    @Resource
    private FileComponent fileComponent;

    @Resource
    private PersonComponent personComponent;

    @Resource
    private ProjectMapper projectMapper;

    @Override
    public BaseResult<PageQueryResult<BugOfflineVO>> list(BugOfflineQueryList bugOfflineQueryList) {
        log.info("线下bug列表接收参数:{}", bugOfflineQueryList);
        if (AscriptionEnum.CURRENT_USER.name().equals(bugOfflineQueryList.getAscription())) {
            //我的
        }else if(AscriptionEnum.RECEIVE.name().equals(bugOfflineQueryList.getAscription())){
            //我收到的
        }else if(AscriptionEnum.TEAM_SUBMIT.name().equals(bugOfflineQueryList.getAscription())){
            //我团队提出的
        }else if(AscriptionEnum.TEAM_RECEIVE.name().equals(bugOfflineQueryList.getAscription())){
            //我团队收到的
        }else if(AscriptionEnum.COPIER.name().equals(bugOfflineQueryList.getAscription())){
            //抄送我的
        }else {
            //全部
        }
        return BaseResult.success(ResultUtil.pageEmpty());
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> add(BugOfflineAddReq bugOfflineAddReq) {
        log.info("线下bug新增接收参数:{}", bugOfflineAddReq);
        BugOfflineDO bugOfflineDO = BugOfflineCopier.INSTANCE.convert(bugOfflineAddReq);
        //1.接收表单参数,状态为:bug打开,经办人所选用户,上一阶段经办人为bug提出人,bug数据入库

        //2.若存在附件,附件数据入库

        //3.若存在抄送人,抄送人数据入库

        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> modify(BugOfflineModifyReq bugOfflineModifyReq) {
        log.info("线下bug修改接收参数:{}", bugOfflineModifyReq);
        //1.接收表单参数,状态不变,经办人为所选用户,上一阶段经办人不变,bug数据入库

        //2.更新附件数据

        //3.更新抄送人数据

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> unHandle(Long id) {
        //1.验证操作人是否是经办人,状态是否是bug打开

        //2.查找bug数据

        //3.赋值:经办人为bug提出人,上一阶段经办人为本次操作人,状态变成待确认

        //4.更新bug数据
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> agree(Long id) {
        //1.验证操作人是否是经办人,状态是否是待确认

        //2.查找bug数据

        //3.赋值:状态变成关闭,经办人与上一阶段经办人不变,

        //4.更新bug数据
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> reject(Long id) {
        //1.验证操作人是否是经办人,状态是否是待确认

        //2.查找bug数据

        //3.赋值:经办人为bug接收人,上一阶段经办人为本次操作人,状态变成bug打开

        //4.更新bug数据
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> delayHandle(Long id) {
        //1.验证操作人是否是经办人,状态是否是bug打开或待修复

        //2.查找bug数据

        //3.赋值:经办人为bug提出人,上一阶段经办人为本次操作人,状态变成延期修复

        //4.更新bug数据
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> doHandle(Long id) {
        //1.验证操作人是否是经办人,状态是否是bug打开

        //2.查找bug数据

        //3.赋值:经办人和上一阶段经办人为本次操作人,状态变成待修复

        //4.更新bug数据
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> passSelf(Long id) {
        //1.验证操作人是否是经办人,状态是否是待修复

        //2.查找bug数据

        //3.赋值:经办人为bug提出人,上一阶段经办人为本次操作人,状态变成待验收

        //4.更新bug数据
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> accepted(Long id) {
        //1.验证操作人是否是经办人,状态是否是待验收

        //2.查找bug数据

        //3.赋值:经办人和上一阶段经办人不变(自测通过时的数据),状态变成完成

        //4.更新bug数据
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> acceptFailed(Long id) {
        //1.验证操作人是否是经办人,状态是否是待验收

        //2.查找bug数据

        //3.赋值:经办人为bug接收人(自测通过时的操作人),上一阶段经办人为本次操作人,状态变成bug打开

        //4.更新bug数据
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> reopen(Long id) {
        //1.验证操作人是否是经办人,状态是否是完成或关闭

        //2.查找bug数据

        //3.赋值:经办人为bug接收人(验收通过或同意时记录的上一阶段操作人),上一阶段经办人为本次操作人,状态变成bug打开

        //4.更新bug数据
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<BugOfflineDetailVO> get(Long id) {
        log.info("任务查看接收参数:{}", id);
        BugOfflineDetailVO bugOfflineDetailVO = new BugOfflineDetailVO();
        //1.检查bug数据是否存在

        //2.查询bug表

        //3.查询产品线表

        //4.查询项目表

        //5.查询附件表

        //6.查询抄送人表

        //7.查询评论表

        //8.查询bug日志表
        return BaseResult.success(bugOfflineDetailVO);
    }

    @Override
    public BaseResult<Boolean> delete(Long id) {
        return BaseResult.success(true);
    }


}
