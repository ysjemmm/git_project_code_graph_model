package com.timevale.forward.dal.entity;

import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author xiaoyun
 * @date 2022/8/31/031 18:11
 */
/**
    * 项目文档表
    */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectDocument {
    /**
    * 主键id
    */
    private Long id;

    /**
    * 项目id
    */
    private Long projectId;

    /**
    * 文档类型：1.产品需求文档
    */
    private Boolean type;

    /**
    * 文档链接
    */
    private String url;

    /**
    * 删除标记
    */
    private Boolean isDeleted;

    /**
    * 创建人id
    */
    private String createManId;

    /**
    * 创建人
    */
    private String createMan;

    /**
    * 创建时间
    */
    private Date createDate;

    /**
    * 修改人id
    */
    private String modifyManId;

    /**
    * 修改人
    */
    private String modifyMan;

    /**
    * 修改时间
    */
    private Date modifyDate;
}