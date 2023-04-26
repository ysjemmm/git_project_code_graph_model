package generator.domain;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * 项目-业务需求关系表(1-N)
 * @TableName project_biz_demand
 */
@Getter
@Setter
@Accessors(chain = true)
public class ProjectBizDemandDO implements Serializable {
    /**
     * 主键id
     */
    private Long id;

    /**
     * 项目id
     */
    private Long projectId;

    /**
     * 业务需求id
     */
    private Long bizDemandId;

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

    private static final long serialVersionUID = 1L;
}