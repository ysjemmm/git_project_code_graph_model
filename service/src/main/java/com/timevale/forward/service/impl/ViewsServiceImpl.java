package com.timevale.forward.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.ValueFilter;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.LabelCategoryListCondition;
import com.timevale.forward.dal.condition.ViewsListCondition;
import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.entity.LabelCategoryDO;
import com.timevale.forward.dal.entity.ViewsDO;
import com.timevale.forward.dal.entity.ViewsUserDO;
import com.timevale.forward.dal.entity.ViewsUserListDO;
import com.timevale.forward.facade.api.client.ViewsService;
import com.timevale.forward.facade.api.query.ViewsGroupQuery;
import com.timevale.forward.facade.api.query.ViewsQueryList;
import com.timevale.forward.facade.api.request.*;
import com.timevale.forward.facade.api.result.ViewsGroupVO;
import com.timevale.forward.facade.api.result.ViewsUserListVO;
import com.timevale.forward.model.enums.ViewsGroupFieldEnum;
import com.timevale.forward.model.enums.ViewsTypeEnum;
import com.timevale.forward.model.enums.ViewsUserTypeEnum;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.ViewsCopier;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.common.annotation.RestService;
import org.springframework.data.util.Pair;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.assertj.core.util.Lists;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

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
    @Resource
    private LabelCategoryMapper labelCategoryMapper;

    private final ValueFilter valueFilter = (object, name, value) -> {
        if (value == null) {
            return null;  // 过滤 null
        } else if (value instanceof String) {  // 先检查是否是 String
            String str = (String) value;  // 手动强转
            if (str.isEmpty()) {
                return null;  // 过滤空字符串 ""
            }
        } else if (value instanceof Collection<?>) {  // 检查是否是 Collection（List、Set）
            Collection<?> collection = (Collection<?>) value;
            if (collection.isEmpty()) {
                return null;  // 过滤空集合
            }
        } else if (value instanceof Map<?, ?>) {  // 检查是否是 Map
            Map<?, ?> map = (Map<?, ?>) value;
            if (map.isEmpty()) {
                return null;  // 过滤空 Map（{}）
            }
        } else if (value.getClass().isArray()) {  // 检查是否是数组
            if (java.lang.reflect.Array.getLength(value) == 0) {
                return null;  // 过滤空数组（如 new String[0]）
            }
        }
        return value;  // 其他情况正常返回
    };

    @Override
    public BaseResult<List<ViewsUserListVO>> list(ViewsQueryList viewsQueryList) {
        ViewsListCondition viewsListCondition = ViewsCopier.INSTANCE.convert(viewsQueryList);
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        viewsListCondition.setOwnerId(userInfo.getId());
        List<ViewsUserListDO> viewsUserDOList = viewsUserMapper.list(viewsListCondition);
        if (Objects.equals(viewsQueryList.getOwnerType(), ViewsUserTypeEnum.MINE.getCode())) {
            // 查询共享使用者
            for (ViewsUserListDO viewsUserListDO : viewsUserDOList) {
                List<ViewsUserDO> viewsUserDOS = viewsUserMapper.getByViewId(viewsUserListDO.getViewsId());
                List<String> shareUsers = Optional.ofNullable(viewsUserDOS)
                        .orElseGet(Collections::emptyList)
                        .stream()
                        .filter(Objects::nonNull)  // 过滤掉 null 的 ViewsUserDO
                        .filter(e -> {
                            String ownerId = e.getOwnerId();
                            return ownerId != null && !Objects.equals(ownerId, userInfo.getId());
                        })
                        .map(ViewsUserDO::getOwner)  // 确保 ownerId 不为 null
                        .collect(Collectors.toList());
                viewsUserListDO.setShareUsers(shareUsers);
            }
        }
        List<ViewsUserListVO> viewsUserVOList = new ArrayList<>();
        for (ViewsUserListDO viewsUserListDO : viewsUserDOList) {
            ViewsUserListVO viewsUserListVO = ViewsCopier.INSTANCE.convert(viewsUserListDO);
            viewsUserListVO.setGroupFields(JSON.parseArray(viewsUserListDO.getGroupField(), ViewsGroupFieldReq.class));
            ViewsTypeEnum viewsTypeEnum = ViewsTypeEnum.getByCode(viewsUserListDO.getViewsType());
            if (viewsTypeEnum != null) {
                switch (viewsTypeEnum) {
                    case BIZ_DEMAND:
                        viewsUserListVO.setFilterConditions(JSON.parseObject(viewsUserListDO.getFilterCondition(), ViewsBizDemandReq.class));
                        break;
                    case PRODUCT_DEMAND:
                        viewsUserListVO.setFilterConditions(JSON.parseObject(viewsUserListDO.getFilterCondition(), ViewsProductDemandReq.class));
                        break;
                }
            }
            viewsUserVOList.add(viewsUserListVO);
        }
        return BaseResult.success(viewsUserVOList);
    }

    @Override
    public BaseResult<Boolean> save(ViewsSaveReq viewsSaveReq) {
        Pair<ViewsDO, ViewsUserDO> viewsPair = getViewsDo(viewsSaveReq.getId());
        ViewsDO views = viewsPair.getFirst();
        checkOperationPermission(views.getOwnerId());
        ViewsDO viewsDO = new ViewsDO();
        viewsDO.setId(views.getId());
        viewsDO.setGroupField(CollectionUtils.isEmpty(viewsSaveReq.getGroupFields()) ? "" : JSON.toJSONString(viewsSaveReq.getGroupFields(), valueFilter));
        viewsDO.setFilterCondition(viewsSaveReq.getFilterList() == null ? "" : JSON.toJSONString(viewsSaveReq.getFilterList(), valueFilter));
        viewsMapper.update(viewsDO);
        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> add(ViewsAddReq viewsAddReq) {
        validName(viewsAddReq.getType(), viewsAddReq.getName(), null);
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
        Pair<ViewsDO, ViewsUserDO> viewsPair = getViewsDo(viewsModifyReq.getId());
        ViewsDO viewsDO = viewsPair.getFirst();
        validName(viewsDO.getType(), viewsModifyReq.getName(), null);

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
        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> modify(ViewsModifyReq viewsModifyReq) {
        Pair<ViewsDO, ViewsUserDO> viewsPair = getViewsDo(viewsModifyReq.getId());
        ViewsDO views = viewsPair.getFirst();
        checkOperationPermission(views.getOwnerId());
        validName(views.getType(), viewsModifyReq.getName(), views.getId());
        ViewsDO viewsDO = ViewsCopier.INSTANCE.toDO(viewsModifyReq);
        viewsDO.setId(views.getId());
        viewsMapper.update(viewsDO);
        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> delete(ViewsReq viewsReq) {
        Pair<ViewsDO, ViewsUserDO> viewsPair = getViewsDo(viewsReq.getId());
        ViewsDO views = viewsPair.getFirst();
        checkOperationPermission(views.getOwnerId());
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        viewsMapper.delete(views.getId(), userInfo.getId(), userInfo.getFullAlias());
        viewsUserMapper.deleteByViewsId(views.getId(), userInfo.getId(), userInfo.getFullAlias());
        return BaseResult.success(true);
    }

    /**
     * 移除分享视图
     *
     * @param viewsReq 视图请求
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> remove(ViewsReq viewsReq) {
        Pair<ViewsDO, ViewsUserDO> viewsPair = getViewsDo(viewsReq.getId());
        ViewsUserDO viewsUser = viewsPair.getSecond();
        if (Objects.equals(viewsUser.getType(), ViewsUserTypeEnum.MINE.getCode())) {
            throw new BaseBizRuntimeException("只有操作分享给你的视图");
        }
        checkOperationPermission(viewsUser.getOwnerId());
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        viewsUserMapper.delete(viewsReq.getId(), userInfo.getId(), userInfo.getFullAlias());
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> hidden(ViewsReq viewsReq) {
        Pair<ViewsDO, ViewsUserDO> viewsPair = getViewsDo(viewsReq.getId());
        ViewsUserDO viewsUser = viewsPair.getSecond();
        checkOperationPermission(viewsUser.getOwnerId());
        viewsUser.setHidden(!viewsUser.getHidden());
        viewsUserMapper.update(viewsUser);
        return null;
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> share(ViewsShareReq viewsShareReq) {
        Pair<ViewsDO, ViewsUserDO> viewsPair = getViewsDo(viewsShareReq.getId());
        ViewsDO views = viewsPair.getFirst();
        checkOperationPermission(views.getOwnerId());
        for (PersonAddReq user : viewsShareReq.getUsers()) {
            // 查看是否已经存在
            ViewsUserDO viewsUser = viewsUserMapper.getByViewIdAndOwnerId(views.getId(), user.getUserId());
            if (viewsUser == null) {
                ViewsUserDO viewsUserDO = new ViewsUserDO();
                viewsUserDO.setViewsId(views.getId());
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

    @Override
    public BaseResult<List<ViewsGroupVO>> groupConditions(ViewsGroupQuery viewsGroupQuery) {
        ViewsTypeEnum viewsTypeEnum = ViewsTypeEnum.getByCode(viewsGroupQuery.getType());
        if (viewsTypeEnum == null) {
            throw new BaseBizRuntimeException("业务类型不存在");
        }
        List<ViewsGroupFieldEnum> viewsGroupFields = ViewsGroupFieldEnum.getByViewsType(viewsTypeEnum);
        List<ViewsGroupVO> list = new ArrayList<>();
        for (ViewsGroupFieldEnum viewsGroupField : viewsGroupFields) {
            if (viewsGroupField.getType() == 1) { // 标签类别
                LabelCategoryListCondition condition = LabelCategoryListCondition.builder().types(Lists.newArrayList(viewsTypeEnum.getCode())).build();
                List<LabelCategoryDO> labelCategroryList = labelCategoryMapper.list(condition);
                for (LabelCategoryDO labelCategoryDO : labelCategroryList) {
                    list.add(ViewsGroupVO.builder().key(labelCategoryDO.getId().toString()).name(labelCategoryDO.getName()).type(viewsGroupField.getType()).build());
                }
            } else {
                list.add(ViewsGroupVO.builder().key(viewsGroupField.getKey()).name(viewsGroupField.getName()).type(viewsGroupField.getType()).build());
            }
        }
        return BaseResult.success(list);
    }

    private Pair<ViewsDO, ViewsUserDO> getViewsDo(Long viewsUserId) {
        ViewsUserDO viewsUser = viewsUserMapper.get(viewsUserId);
        if (viewsUser == null) {
            throw new BaseBizRuntimeException("该视图不存在,请刷新后重试");
        }
        ViewsDO views = viewsMapper.get(viewsUser.getViewsId());
        if (views == null) {
            throw new BaseBizRuntimeException("该视图不存在，请刷新后重试");
        }
        return Pair.of(views, viewsUser);
    }

    private void validName(Integer type, String name, Long id) {
        if (name.contains(CommonConstant.BLANK)) {
            throw new BaseBizRuntimeException("视图名称中请勿包含空格");
        }
        ViewsDO oldViews = viewsMapper.getByTypeAndName(type, name);
        if (oldViews != null && !oldViews.getId().equals(id)) {
            throw new BaseBizRuntimeException("该视图名称已存在,请修改后重试");
        }
    }

    private void checkOperationPermission(String ownerId) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        if (!Objects.equals(ownerId, userInfo.getId())) {
            throw new BaseBizRuntimeException("你没有权限操作");
        }
    }
}
