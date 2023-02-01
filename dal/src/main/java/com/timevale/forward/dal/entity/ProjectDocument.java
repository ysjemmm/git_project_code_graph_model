package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 项目文档表
 * @author xiaoyun
 * @date 2022/8/31/031 18:11
 */
@Data
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
    * 文档类型：1.产品需求文档;11.立项申请报告;12.项目方案报告;13审计计划;14.项目复盘报告;15:运营计划;16:其他
    */
    private Integer type;

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

    /**
     * 文档名称
     */
    private String docName;

    /**
     * 文档所属项目阶段:11:启动阶段;12:规划阶段;13:执行阶段;14:收尾阶段;15:运营阶段
     */
    private Integer stage;
}