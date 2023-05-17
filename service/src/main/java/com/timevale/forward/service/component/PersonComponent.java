package com.timevale.forward.service.component;

import com.timevale.forward.dal.entity.PersonDO;
import com.timevale.forward.facade.api.request.PersonAddReq;

import java.util.Collection;
import java.util.List;

public interface PersonComponent {
    /**
     *
     * @param list 人员列表
     * @param mainId 所属主体id
     * @param type 所属主体类型
     */
    void add(Collection<PersonAddReq> list, Long mainId, Integer type);

    /**
     * 添加
     *
     * @param list        列表
     * @param mainId      主要id
     * @param type        类型
     * @param personLevel 人水平
     */
    void add(Collection<PersonAddReq> list, Long mainId, Integer type, Integer personLevel);


    /**
     *
     * @param list 人员列表
     * @param mainId 所属主体id
     * @param type 所属主体类型
     */
    void update(List<PersonAddReq> list, Long mainId, Integer type);

    /**
     *
     * @param list 人员列表
     * @param mainId 所属主体id
     * @param type 所属主体类型
     */
    void update(List<PersonAddReq> list, Long mainId, Integer type, Integer personLevel);

    /**
     *
     * @param mainId 所属主体id
     * @param type 所属主体类型
     * @return 列表
     */
    List<PersonDO> select(Long mainId, Integer type);

    /**
     *
     * @param mainId 所属主体id
     * @param type 所属主体类型
     * @return 列表
     */
    List<PersonDO> select(Long mainId, Integer type, Integer personLevel);

    /**
     *
     * @param list 人员列表
     * @param mainId 所属主体id
     * @param type 所属主体类型
     */
    List<PersonDO> addIfNotExisted(Collection<PersonAddReq> list, Long mainId, Integer type);

    /**
     *
     * @param list        列表
     * @param mainId      主要id
     * @param type        类型
     * @param personLevel 人水平
     * @return {@link List}<{@link PersonDO}>
     */
    List<PersonDO> addIfNotExisted(List<PersonAddReq> list, Long mainId, Integer type, Integer personLevel);

    /**
     * 存在
     *
     * @param userId 用户id
     * @param mainId 主要id
     * @param type   类型
     * @return boolean
     */
    boolean exist(String userId, Long mainId, Integer type);

    void duplicateRemove(List<PersonAddReq> list, PersonAddReq duplicate);
}
