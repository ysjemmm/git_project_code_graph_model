package com.timevale.forward.service.component;

import com.timevale.forward.dal.entity.PersonDO;
import com.timevale.forward.facade.api.request.PersonAddReq;

import java.util.List;

public interface PersonComponent {
    /**
     *
     * @param list 人员列表
     * @param mainId 所属主体id
     * @param type 所属主体类型
     */
    void add(List<PersonAddReq> list, Long mainId, Byte type);


    /**
     *
     * @param list 人员列表
     * @param mainId 所属主体id
     * @param type 所属主体类型
     */
    void update(List<PersonAddReq> list, Long mainId, Byte type);

    /**
     *
     * @param mainId 所属主体id
     * @param type 所属主体类型
     * @return 列表
     */
    List<PersonDO> select(Long mainId, Byte type);

}
