package com.timevale.forward.dal.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskOverdueDTO {
    private String userId;

    private String userName;

    private Long overdueCount;

    private Long accumulateOverdueMillis;
}
