package com.timevale.forward.service.component.impl;

// 引入必要的包
import com.google.common.collect.Lists;
import com.timevale.forward.dal.condition.ProductDemandGroupListCondition;
import com.timevale.forward.dal.dao.ProductDemandGroupItemMapper;
import com.timevale.forward.dal.dao.ProductDemandMapper;
import com.timevale.forward.dal.dto.ProductDemandMoveDTO;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.facade.api.request.ProductDemandGroupItemMoveReq;
import com.timevale.forward.model.enums.ProductDemandGroupMoveModeEnum;
import com.timevale.forward.model.enums.ProductDemandStatusEnum;
import com.timevale.forward.service.component.ProductDemandGroupItemComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.utils.StringUtil;
import com.timevale.forward.service.utils.date.DateUtil;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.forward.service.utils.position.PositionUtil;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * 产品-分组关系表 组件实现类
 */
@Component // Spring组件注解
@Slf4j
public class ProductDemandGroupItemComponentImpl implements ProductDemandGroupItemComponent {
    // 注入Mapper
    @Resource
    private ProductDemandGroupItemMapper productDemandGroupItemMapper;
    @Resource
    private ProductDemandMapper productDemandMapper;

    @Override
    public void deleteByGroupId(Long groupId) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        String modifyManId = userInfo.getId();
        String modifyMan = userInfo.getFullAlias();
        productDemandGroupItemMapper.deleteByGroupId(groupId, modifyManId, modifyMan);
    }

    // 查询某分组下所有产品需求
    @Override
    public List<ProductDemandGroupItemListDO> listProductDemand(ProductDemandGroupListCondition condition) {
        condition.setName(StringUtil.toLikeStr(condition.getName()));
        condition.setCreateDateStart(DateUtil.getStartOfDay(condition.getCreateDateStart()));
        condition.setCreateDateEnd(DateUtil.getEndOfDay(condition.getCreateDateEnd()));
        condition.setProductDemandQueryExist(condition.judgeProductDemandQueryExist());
        return productDemandGroupItemMapper.listProductDemand(condition);
    }

    @Override
    public List<ProductDemandListDO> listProductDemandBacklog(ProductDemandGroupListCondition condition) {
        condition.setName(StringUtil.toLikeStr(condition.getName()));
        condition.setCreateDateStart(DateUtil.getStartOfDay(condition.getCreateDateStart()));
        condition.setCreateDateEnd(DateUtil.getEndOfDay(condition.getCreateDateEnd()));
        // 排除已废弃，已完成上线
        if (CollectionUtils.isNotEmpty(condition.getStatus())) {
            List<Integer> statusList = condition.getStatus().stream()
                    .filter(e -> !ProductDemandStatusEnum.INVALID.getCode().equals(e) && !ProductDemandStatusEnum.ONLINE.getCode().equals(e))
                    .collect(Collectors.toList());
            condition.setStatus(statusList);
        } else {
            List<Integer> statusList = Arrays.stream(ProductDemandStatusEnum.values())
                    .filter(e -> !ProductDemandStatusEnum.INVALID.equals(e) && !ProductDemandStatusEnum.ONLINE.equals(e))
                    .map(ProductDemandStatusEnum::getCode)
                    .collect(Collectors.toList());
            condition.setStatus(statusList);
        }
        return productDemandGroupItemMapper.listProductDemandBacklog(condition);
    }

    @Override
    public ProductDemandGroupItemDO getByGroupIdAndDemandId(Long groupId, Long demandId) {
        return productDemandGroupItemMapper.getByGroupIdAndDemandId(groupId, demandId);
    }

    @Override
    public ProductDemandGroupItemDO getByDemandId(Long demandId) {
        return productDemandGroupItemMapper.getByDemandId(demandId);
    }

    @Override
    public ProductDemandMoveDTO moveProductDemand(ProductDemandGroupItemMoveReq req) {
        ProductDemandGroupItemDO prevGroupItemDO = null;
        if (req.getPrevId() != null) {
            prevGroupItemDO = productDemandGroupItemMapper.getByGroupIdAndId(req.getTargetGroupId(), req.getPrevId());
            if (prevGroupItemDO == null) {
                throw new BaseBizRuntimeException("产品需求分组的产品需求不存在, 请刷新后重试");
            }
        }
        ProductDemandGroupItemDO nextGroupItemDO = null;
        if (req.getNextId() != null) {
            nextGroupItemDO = productDemandGroupItemMapper.getByGroupIdAndId(req.getTargetGroupId(), req.getNextId());
            if (nextGroupItemDO == null) {
                throw new BaseBizRuntimeException("产品需求分组的产品需求不存在, 请刷新后重试");
            }
        }
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        String modifyManId = userInfo.getId();
        String modifyMan = userInfo.getFullAlias();
        // 定义 getPrevByPosition 和 getNextByPosition 函数
        BiFunction<Long, BigDecimal, BigDecimal> getPrevByPosition = (targetGroupId, position) -> {
            ProductDemandGroupItemDO preByPosition = productDemandGroupItemMapper.getPreByPosition(targetGroupId, position);
            return preByPosition == null ? null : preByPosition.getPosition();
        };
        BiFunction<Long, BigDecimal, BigDecimal> getNextByPosition = (targetGroupId, position) -> {
            ProductDemandGroupItemDO nextByPosition = productDemandGroupItemMapper.getNextByPosition(targetGroupId, position);
            return nextByPosition == null ? null : nextByPosition.getPosition();
        };

        BiFunction<Long, Long, BigDecimal> getPosition = (targetGroupId, id) -> {
            ProductDemandGroupItemDO item = productDemandGroupItemMapper.getByGroupIdAndId(targetGroupId, id);
            return item == null ? null : item.getPosition();
        };

        final ArrayList<Integer> notAllowProductDemandStatuses = Lists.newArrayList(ProductDemandStatusEnum.INVALID.getCode(), ProductDemandStatusEnum.SUSPEND.getCode());
        if (Objects.equals(ProductDemandGroupMoveModeEnum.MOVE_IN.getCode(), req.getMode())) {
            // 查看产品需求是否是当前业务域里的
            ProductDemandDO productDemandDO = productDemandMapper.get(req.getId());
            if (productDemandDO == null) {
                throw new BaseBizRuntimeException("产品需求不存在");
            }
            if (notAllowProductDemandStatuses.contains(productDemandDO.getStatus())) {
                throw new BaseBizRuntimeException(String.format("产品需求状态为%s, 不能操作", ProductDemandStatusEnum.getTextByCode(productDemandDO.getStatus())));
            }
            // 查看产品需求是否已经被拖到分组里
            final Integer countByProductDemandId = productDemandGroupItemMapper.countByProductDemandId(req.getId());
            if (countByProductDemandId > 0) {
                throw new BaseBizRuntimeException("产品需求已被关联到产品需求分组，请刷新后重试");
            }

            // 判断是否在一个业务域内，暂不判断
//            ProductLineDO productLineDO = productLineComponent.getById(productDemandDO.getProductLineId());
//            if (productLineDO == null || !productDemandGroupItemMoveReq.getBizDomainId().equals(productLineDO.getBizDomainId())) {
//                throw new BaseBizRuntimeException("产品需求不属于当前业务域, 不能操作");
//            }
            // 查看产品需求是否已经在分组里
//            ProductDemandGroupItemDO byGroupIdAndDemandId = productDemandGroupItemMapper.getByGroupIdAndDemandId(productDemandGroupItemMoveReq.getTargetGroupId(), productDemandGroupItemMoveReq.getId());
//            if (byGroupIdAndDemandId != null) {
//                throw new BaseBizRuntimeException("产品需求已存在");
//            }
            // 创建
            Pair<BigDecimal, Boolean> position = calculateNewPosition(req.getPrevId(),
                    req.getNextId(),
                    req.getBizDomainGroupId(),
                    req.getTargetGroupId(),
                    getPosition,
                    getPrevByPosition,
                    getNextByPosition
            );
            // position和前后相同，说明需要重新排序
            if (!position.getSecond()) {
                resetPosition(req.getTargetGroupId(), req.getBizDomainGroupId());
                position = calculateNewPosition(req.getPrevId(),
                        req.getNextId(),
                        req.getBizDomainGroupId(),
                        req.getTargetGroupId(),
                        getPosition,
                        getPrevByPosition,
                        getNextByPosition
                );
            }
            val createGroupItemDO = new ProductDemandGroupItemDO().setPosition(position.getFirst())
                    .setProductDemandId(req.getId())
                    .setProductDemandGroupId(req.getTargetGroupId())
                    .setVersion(0L).setIsActive(true);
            createGroupItemDO.setCreateMan(modifyMan);
            createGroupItemDO.setCreateManId(modifyManId);
            try {
                int createPosition = productDemandGroupItemMapper.insert(createGroupItemDO);
                if (createPosition != 1) {
                    log.error("创建产品需求分组产品需求失败:{}", createGroupItemDO);
                    throw new BaseBizRuntimeException("操作失败，请刷新页面重试");
                }
            } catch (DuplicateKeyException e) {
                log.error("创建产品需求分组产品需求失败:{}", createGroupItemDO, e);
                throw new BaseBizRuntimeException("操作失败，请刷新页面重试");
            }
            return new ProductDemandMoveDTO(req.getTargetGroupId(), null, req.getId(), null, false);
        } else if (Objects.equals(ProductDemandGroupMoveModeEnum.MOVE_OUT.getCode(), req.getMode())) {
            if (req.getPrevId() != null || req.getNextId() != null) {
                throw new BaseBizRuntimeException("参数错误");
            }
            final ProductDemandGroupItemDO moveGroupItemDO = productDemandGroupItemMapper.get(req.getId());
            if (moveGroupItemDO == null || !Objects.equals(moveGroupItemDO.getProductDemandGroupId(), req.getTargetGroupId())) {
                throw new BaseBizRuntimeException("产品需求已经不在当前分组，请刷新后重试");
            }
            // 删除
            productDemandGroupItemMapper.delete(req.getId(), modifyManId, modifyMan);
            return new ProductDemandMoveDTO(null, req.getId(), moveGroupItemDO.getProductDemandId(), moveGroupItemDO.getProductDemandGroupId(), false);
        } else {
            final ProductDemandGroupItemDO moveGroupItemDO = productDemandGroupItemMapper.get(req.getId());
            if (moveGroupItemDO == null) {
                throw new BaseBizRuntimeException("产品需求分组的产品需求不存在, 请刷新后重试");
            }
            ProductDemandDO moveProductDemandDO = productDemandMapper.get(moveGroupItemDO.getProductDemandId());
            if (moveProductDemandDO == null) {
                throw new BaseBizRuntimeException("产品需求不存在");
            }
            if (notAllowProductDemandStatuses.contains(moveProductDemandDO.getStatus())) {
                throw new BaseBizRuntimeException(String.format("产品需求状态为%s, 不能操作", ProductDemandStatusEnum.getTextByCode(moveProductDemandDO.getStatus())));
            }
            if (Objects.equals(req.getTargetGroupId(), moveGroupItemDO.getProductDemandGroupId())) {
                ProductDemandGroupItemDO targetNextGroupItemDO = productDemandGroupItemMapper.getNextByPosition(req.getTargetGroupId(), moveGroupItemDO.getPosition());
                ProductDemandGroupItemDO targetPreGroupItemDO = productDemandGroupItemMapper.getPreByPosition(req.getTargetGroupId(), moveGroupItemDO.getPosition());
                boolean preConditionEqual = isProductGroupItemEqual(prevGroupItemDO, targetPreGroupItemDO);
                boolean nextConditionEqual = isProductGroupItemEqual(nextGroupItemDO, targetNextGroupItemDO);
                if (preConditionEqual && nextConditionEqual) {
                    log.info("产品需求位置未变化，无需移动");
                    return new ProductDemandMoveDTO(null, null, null, null, true);
//                    throw new BaseBizRuntimeException("产品需求位置未变化，无需移动");
                }
            }
            // 更新位置或目标分组id
            Pair<BigDecimal, Boolean> position = calculateNewPosition(req.getPrevId(),
                    req.getNextId(),
                    req.getBizDomainGroupId(),
                    req.getTargetGroupId(),
                    getPosition,
                    getPrevByPosition,
                    getNextByPosition
            );
            // position和前后相同，说明需要重新排序
            if (!position.getSecond()) {
                resetPosition(req.getTargetGroupId(), req.getBizDomainGroupId());
                position = calculateNewPosition(req.getPrevId(),
                        req.getNextId(),
                        req.getBizDomainGroupId(),
                        req.getTargetGroupId(),
                        getPosition,
                        getPrevByPosition,
                        getNextByPosition
                );
            }
            val updateGroupItemDO = new ProductDemandGroupItemDO().setPosition(position.getFirst())
                    .setVersion(moveGroupItemDO.getVersion())
                    .setProductDemandGroupId(req.getTargetGroupId());
            updateGroupItemDO.setId(moveGroupItemDO.getId());
            updateGroupItemDO.setModifyMan(modifyMan);
            updateGroupItemDO.setModifyManId(modifyManId);
            try {
                int updatePosition = productDemandGroupItemMapper.updatePosition(updateGroupItemDO);
                if (updatePosition != 1) {
                    log.error("更新产品需求分组产品需求位置失败:{}", updateGroupItemDO);
                    throw new BaseBizRuntimeException("操作失败，请刷新页面重试");
                }
            } catch (DuplicateKeyException e) {
                log.error("更新产品需求分组产品需求位置失败:{}", updateGroupItemDO, e);
                throw new BaseBizRuntimeException("操作失败，请刷新页面重试");
            }
            return new ProductDemandMoveDTO(req.getTargetGroupId(), req.getId(), moveGroupItemDO.getProductDemandId(), moveGroupItemDO.getProductDemandGroupId(), false);
        }
    }

    @Override
    public Pair<BigDecimal, Boolean> calculateNewPosition(Long prevId,
                                Long nextId,
                                Long bizDomainId,
                                Long positionTarget,
                                BiFunction<Long, Long, BigDecimal> getPosition,
                                BiFunction<Long, BigDecimal, BigDecimal> getPrevByPosition,
                                BiFunction<Long, BigDecimal, BigDecimal> getNextByPosition) {
        BigDecimal position;
        if (prevId == null && nextId == null) {
            // 前后都为空，直接添加到第一个
            position = PositionUtil.generate(bizDomainId.toString(), System.currentTimeMillis());
        } else if (prevId == null && nextId != null) {
            BigDecimal nextPosition = getPosition.apply(positionTarget, nextId);
            // 前面为空，后面不为空
            BigDecimal nextPrePosition = getPrevByPosition.apply(positionTarget, nextPosition);
            // 如果后面不是第一个（前面还存在）则(next.positon + next.pre.position)/2
            if (nextPrePosition != null) {
                position = nextPosition.add(nextPrePosition).divide(BigDecimal.valueOf(2), 10, BigDecimal.ROUND_HALF_UP);
                // 如果position和前后相同，说明需要重新排序
                if (nextPosition.subtract(position).abs().compareTo(CommonConstant.EPSILON) < 0 ||
                    position.subtract(nextPrePosition).abs().compareTo(CommonConstant.EPSILON) < 0) {
                    return Pair.of(position, false);
                }
            } else {
                // 如果后面是第一个（前面不存在) 则直接添加到第一个
                position = PositionUtil.generate(bizDomainId.toString(), System.currentTimeMillis());
            }
        } else {
            BigDecimal prevPosition = getPosition.apply(positionTarget, prevId);
            // 前面不为空
            BigDecimal preNextPosition = getNextByPosition.apply(positionTarget, prevPosition);
            // 如果前面不是最后一个（后面还存在）则(pre.position + pre.next.position)/2
            if (preNextPosition != null) {
                position = prevPosition.add(preNextPosition).divide(BigDecimal.valueOf(2), 10, BigDecimal.ROUND_HALF_UP);
                // 如果position和前后相同，说明需要重新排序
                if (preNextPosition.subtract(position).abs().compareTo(CommonConstant.EPSILON) < 0 ||
                    position.subtract(prevPosition).abs().compareTo(CommonConstant.EPSILON) < 0) {
                    return Pair.of(position, false);
                }
            } else {
                // 如果前面是最后一个（后面不存在） 则pre.position - 5000000
                position = prevPosition.subtract(CommonConstant.POSITION_STEP);
            }
        }
        return Pair.of(position, true);
    }

    @Override
    public void resetPosition(Long groupId, Long bizDomainId) {
        log.info("开始对分组{}的需求进行位置重排", groupId);
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        String modifyManId = userInfo.getId();
        String modifyMan = userInfo.getFullAlias();
        // 获取当前分组下的所有（按position倒序）
        List<ProductDemandGroupItemDO> productDemandGroupItemDOS = productDemandGroupItemMapper.getByGroupId(groupId);
        BigDecimal position = PositionUtil.generate(bizDomainId.toString(), System.currentTimeMillis());
        for (ProductDemandGroupItemDO productDemandGroupItemDO : productDemandGroupItemDOS) {
            log.info("分组元素({})的位置重排，{} -》 {}", productDemandGroupItemDO.getId(), productDemandGroupItemDO.getPosition(), position);
            val updateGroupDO = new ProductDemandGroupItemDO().setPosition(position);
            updateGroupDO.setId(productDemandGroupItemDO.getId());
            updateGroupDO.setModifyMan(modifyMan);
            updateGroupDO.setModifyManId(modifyManId);
            productDemandGroupItemMapper.update(updateGroupDO);
            position = position.subtract(CommonConstant.POSITION_STEP);
        }
    }

    private boolean isProductGroupItemEqual(ProductDemandGroupItemDO source, ProductDemandGroupItemDO target) {
        if (source == null && target == null) {
            return true;
        }
        if (source != null && target != null) {
            return Objects.equals(source.getId(), target.getId());
        }
        return false;
    }
} 