package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.BizPermissionOwnerDO;
import lombok.NonNull;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

@Mapper
public interface BizPermissionOwnerMapper {

    /**
     * 新增单条信息
     *
     * @param bizPermissionOwner 业务需求DO
     * @return int
     */
    int insert(BizPermissionOwnerDO bizPermissionOwner);

    /**
     * 批量新增信息
     *
     * @param bizPermissionOwners 业务需求DO
     * @return int
     */
    int batchInsert(@Param("bizPermissionOwners") Collection<BizPermissionOwnerDO> bizPermissionOwners);


    /**
     * 批量删除
     *
     * @param ids id列表
     * @return int
     */
    int batchDeleteByIds(Collection<Long> ids);

    /**
     * 根据权限作用域查询
     *
     * @param permissionScopes 权限范围
     * @return list
     */
    List<BizPermissionOwnerDO> listByPermissionScopes (@Param("permissionScopes") Collection<String> permissionScopes);

    /**
     * 根据权限类型 and 作用域查询
     *
     * @param permissionType 权限类型
     * @param permissionScope 权限范围
     * @param ownerId 负责人ID
     * @return list
     */
    List<BizPermissionOwnerDO> listByPermissionTypeAndScopeAndOwnerIdIn (@Param("permissionType") Long permissionType,
                                                                         @Param("permissionScope") String permissionScope,
                                                                         @Param("ownerId") String ownerId,
                                                                         @Param("scopeBizIds") Collection<Long> scopeBizIds);
    /**
     * 批量删除权限关系
     *
     * @param permissionType 权限类型
     * @param permissionScope 权限范围
     * @param ownerIds 负责人ids
     * @return int
     */
    int batchDeletePermissionOwners (@Param("permissionType") @NonNull Long permissionType,
                                     @Param("permissionScope") @NonNull String permissionScope,
                                     @Param("ownerIds") @NonNull Collection<String> ownerIds);

}
