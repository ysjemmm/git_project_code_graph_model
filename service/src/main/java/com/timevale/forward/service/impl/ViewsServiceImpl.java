package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.entity.ViewsDO;
import com.timevale.forward.dal.entity.ViewsUserDO;
import com.timevale.forward.facade.api.client.ViewsService;
import com.timevale.forward.facade.api.query.ViewsQueryList;
import com.timevale.forward.facade.api.request.*;
import com.timevale.forward.facade.api.result.ViewsVO;
import com.timevale.forward.model.enums.ViewsUserTypeEnum;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.ViewsCopier;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.common.annotation.RestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * 用户视图服务实现类
 *
 * @author qiyuan
 * @date 2025/08/14 15:00
 */
@Slf4j
@RestService
public class ViewsServiceImpl implements ViewsService {

    @Resource
    private ViewsMapper viewsMapper;
    @Resource
    private ViewsUserMapper viewsUserMapper;

    @Override
    public BaseResult<List<ViewsVO>> list(ViewsQueryList viewsQueryList) {
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> add(ViewsAddReq viewsAddReq) {
        if (viewsAddReq.getName().contains(CommonConstant.BLANK)) {
            throw new BaseBizRuntimeException("视图名称中请勿包含空格");
        }
        ViewsDO views = viewsMapper.getByTypeAndName(viewsAddReq.getType(), viewsAddReq.getName());
        if (views != null) {
            throw new BaseBizRuntimeException("该视图名称已存在,请修改后重试");
        }
        ViewsDO viewsDO = ViewsCopier.INSTANCE.toDO(viewsAddReq);
        viewsMapper.insert(viewsDO);
        ViewsUserDO viewsUserDO = new ViewsUserDO();
        viewsUserDO.setViewsId(viewsDO.getId());
        viewsUserDO.setType(ViewsUserTypeEnum.MINE.getCode());
        viewsUserDO.setPosition(BigDecimal.valueOf(System.currentTimeMillis()));
        viewsUserDO.setHidden(false);
        viewsUserDO.setOwner(viewsDO.getOwner());
        viewsUserDO.setOwnerId(viewsDO.getOwnerId());
        viewsUserMapper.insert(viewsUserDO);
        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> copy(ViewsModifyReq viewsModifyReq) {
        if (viewsModifyReq.getName().contains(CommonConstant.BLANK)) {
            throw new BaseBizRuntimeException("视图名称中请勿包含空格");
        }
        ViewsDO viewsDO = viewsMapper.get(viewsModifyReq.getId());
        if (viewsDO == null) {
            throw new BaseBizRuntimeException("该视图不存在，请刷新后重试");
        }
        ViewsDO views = viewsMapper.getByTypeAndName(viewsDO.getType(), viewsModifyReq.getName());
        if (views != null) {
            throw new BaseBizRuntimeException("该视图名称已存在,请修改后重试");
        }
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        viewsDO.setId(null);
        viewsDO.setOwner(userInfo.getFullAlias());
        viewsDO.setOwnerId(userInfo.getId());
        viewsDO.setName(viewsModifyReq.getName());
        viewsMapper.insert(viewsDO);
        ViewsUserDO viewsUserDO = new ViewsUserDO();
        viewsUserDO.setViewsId(viewsDO.getId());
        viewsUserDO.setType(ViewsUserTypeEnum.MINE.getCode());
        viewsUserDO.setPosition(BigDecimal.valueOf(System.currentTimeMillis()));
        viewsUserDO.setHidden(false);
        viewsUserDO.setOwner(viewsDO.getOwner());
        viewsUserDO.setOwnerId(viewsDO.getOwnerId());
        viewsUserMapper.insert(viewsUserDO);
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> modify(ViewsModifyReq viewsModifyReq) {
        if (viewsModifyReq.getName().contains(CommonConstant.BLANK)) {
            throw new BaseBizRuntimeException("视图名称中请勿包含空格");
        }
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        ViewsDO views = viewsMapper.get(viewsModifyReq.getId());
        if (views == null) {
            throw new BaseBizRuntimeException("该视图不存在，请刷新后重试");
        }
        if (!Objects.equals(views.getOwnerId(), userInfo.getId())) {
            throw new BaseBizRuntimeException("只有创建人可以操作");
        }
        ViewsDO oldViews = viewsMapper.getByTypeAndName(views.getType(), viewsModifyReq.getName());
        if (oldViews != null && !oldViews.getId().equals(views.getId())) {
            throw new BaseBizRuntimeException("该视图称已存在,请修改后重试");
        }

        ViewsDO viewsDO = ViewsCopier.INSTANCE.toDO(viewsModifyReq);
        viewsMapper.update(viewsDO);
        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> delete(ViewsReq viewsReq) {
        ViewsDO views = viewsMapper.get(viewsReq.getId());
        if (views == null) {
            throw new BaseBizRuntimeException("该视图不存在，请刷新后重试");
        }
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        if (!Objects.equals(views.getOwnerId(), userInfo.getId())) {
            throw new BaseBizRuntimeException("只有创建人可以操作");
        }
        viewsMapper.delete(viewsReq.getId(), userInfo.getId(), userInfo.getFullAlias());
        viewsUserMapper.deleteByViewsId(viewsReq.getId(), userInfo.getId(), userInfo.getFullAlias());
        return BaseResult.success(true);
    }

    /**
     * 移除分享视图
     * @param viewsReq 视图请求
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> remove(ViewsReq viewsReq) {
        ViewsUserDO viewsUser = viewsUserMapper.get(viewsReq.getId());
        if (viewsUser == null) {
            throw new BaseBizRuntimeException("该视图不存在，请刷新后重试");
        }
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        if (!Objects.equals(viewsUser.getOwnerId(), userInfo.getId()) || Objects.equals(viewsUser.getType(), ViewsUserTypeEnum.MINE.getCode())) {
            throw new BaseBizRuntimeException("只有操作分享给你的视图");
        }
        viewsUserMapper.delete(viewsReq.getId(), userInfo.getId(), userInfo.getFullAlias());
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> hidden(ViewsReq viewsReq) {
        ViewsUserDO viewsUser = viewsUserMapper.get(viewsReq.getId());
        if (viewsUser == null) {
            throw new BaseBizRuntimeException("该视图不存在，请刷新后重试");
        }
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        if (!Objects.equals(viewsUser.getOwnerId(), userInfo.getId())) {
            throw new BaseBizRuntimeException("只有操作自己创建的和分享给你的视图");
        }
        viewsUser.setHidden(!viewsUser.getHidden());
        viewsUserMapper.update(viewsUser);
        return null;
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> share(ViewsShareReq viewsShareReq) {
        ViewsDO views = viewsMapper.get(viewsShareReq.getId());
        if (views == null) {
            throw new BaseBizRuntimeException("该视图不存在，请刷新后重试");
        }
        for (PersonAddReq user : viewsShareReq.getUsers()) {
            // 查看是否已经存在
            ViewsUserDO viewsUser = viewsUserMapper.getByViewIdAndOwnerId(viewsShareReq.getId(), user.getUserId());
            if (viewsUser == null) {
                ViewsUserDO viewsUserDO = new ViewsUserDO();
                viewsUserDO.setViewsId(viewsShareReq.getId());
                viewsUserDO.setType(ViewsUserTypeEnum.SHARE.getCode());
                viewsUserDO.setOwnerId(user.getUserId());
                viewsUserDO.setOwner(user.getUserName());
                viewsUserDO.setHidden(false);
                viewsUserDO.setPosition(BigDecimal.valueOf(0));
                viewsUserMapper.insert(viewsUserDO);
            }
        }

        return BaseResult.success(true);
    }
}
