package com.timevale.forward.dal.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BugOfflineCountDTO {
    private String userId;
    private String userName;
    private Long waitRepairCount;
    private Long urgentRepairCount;
}
