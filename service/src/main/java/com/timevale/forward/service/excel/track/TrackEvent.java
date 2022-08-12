package com.timevale.forward.service.excel.track;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TrackEvent {
    private String serialNumber;
    private String firstClassify;
    private String secondClassify;
    private String thirdClassify;
    private String fourthClassify;
    private String fifthClassify;
    private String eventNameCn;
    private String eventNameEn;
    private String platform;
    private String explanation;
    private String apiName;
    private String touchMoment;
    private String env;

    private Integer firstRowIndex;
    private Integer lastRowIndex;
    private String fullNameCn;
    private List<String> failInfoList = new ArrayList<>();
    private List<TrackProp> trackPropList = new ArrayList<>();
}
