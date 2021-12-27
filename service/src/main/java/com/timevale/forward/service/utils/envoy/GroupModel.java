package com.timevale.forward.service.utils.envoy;

import lombok.Data;

import java.util.List;

/**
 * @author jingchun
 * created on 2021/10/21
 */
@Data
public class GroupModel {
    private String groupId;
    private String groupName;
    private String parentId;
    private String parentName;
    private List<String> managerList;
}
