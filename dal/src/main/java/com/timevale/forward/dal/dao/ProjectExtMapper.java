package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.ProjectDO;
import org.apache.ibatis.annotations.Insert;

public interface ProjectExtMapper {

    // @Insert("<script>insert into project\n" +
    //         "(\n" +
    //         "    <if test=\"name != null\">`name`,</if>\n" +
    //         "    <if test=\"type != null\">type,</if>\n" +
    //         "    <if test=\"kind != null\">kind,</if>\n" +
    //         "    <if test=\"desc != null\">`desc`,</if>\n" +
    //         "    <if test=\"level != null\">`level`,</if>\n" +
    //         "    <if test=\"status != null\">`status`,</if>\n" +
    //         "    <if test=\"priority != null\">priority,</if>\n" +
    //         "    <if test=\"isWithGoal != null\">is_with_goal,</if>\n" +
    //         "    <if test=\"customerDev != null\">customer_dev,</if>\n" +
    //         "    <if test=\"pm != null\">pm,</if>\n" +
    //         "    <if test=\"pmId != null\">pm_id,</if>\n" +
    //         "    <if test=\"sr != null\">sr,</if>\n" +
    //         "    <if test=\"srId != null\">`sr_id`,</if>\n" +
    //         "    <if test=\"principal != null\">principal,</if>\n" +
    //         "    <if test=\"principalId != null\">principal_id,</if>\n" +
    //         "    <if test=\"otnPrincipal != null\">otn_principal,</if>\n" +
    //         "    <if test=\"otnPrincipalId != null\">otn_principal_id,</if>\n" +
    //         "    <if test=\"planStartDate != null\">plan_start_date,</if>\n" +
    //         "    <if test=\"planEndDate != null\">plan_end_date,</if>\n" +
    //         "    <if test=\"pjEstablishStartDate != null\">pj_establish_start_date,</if>\n" +
    //         "    <if test=\"pjEstablishPublishDate != null\">pj_establish_publish_date,</if>\n" +
    //         "    <if test=\"isAcceptance != null\">is_acceptance,</if>\n" +
    //         "    <if test=\"isPlatformPublish != null\">is_platform_publish,</if>\n" +
    //         "    <if test=\"resourceAssessment != null\">resource_assessment,</if>\n" +
    //         "    <if test=\"createMan != null\">create_man,</if>\n" +
    //         "    <if test=\"createManId != null\">create_man_id,</if>\n" +
    //         "    <if test=\"true\">parent_ids</if>)\n" +
    //         "VALUES(\n" +
    //         "    <if test=\"name != null\">#{name},</if>\n" +
    //         "    <if test=\"type != null\">#{type},</if>\n" +
    //         "    <if test=\"kind != null\">#{kind},</if>\n" +
    //         "    <if test=\"desc != null\">#{desc},</if>\n" +
    //         "    <if test=\"level != null\">#{level},</if>\n" +
    //         "    <if test=\"status != null\">#{status},</if>\n" +
    //         "    <if test=\"priority != null\">#{priority},</if>\n" +
    //         "    <if test=\"isWithGoal != null\">#{isWithGoal},</if>\n" +
    //         "    <if test=\"customerDev != null\">#{customerDev},</if>\n" +
    //         "    <if test=\"pm != null\">#{pm},</if>\n" +
    //         "    <if test=\"pmId != null\">#{pmId},</if>\n" +
    //         "    <if test=\"sr != null\">#{sr},</if>\n" +
    //         "    <if test=\"srId != null\">#{srId},</if>\n" +
    //         "    <if test=\"principal != null\">#{principal},</if>\n" +
    //         "    <if test=\"principalId != null\">#{principalId},</if>\n" +
    //         "    <if test=\"otnPrincipal != null\">#{otnPrincipal},</if>\n" +
    //         "    <if test=\"otnPrincipalId != null\">#{otnPrincipalId},</if>\n" +
    //         "    <if test=\"planStartDate != null\">#{planStartDate},</if>\n" +
    //         "    <if test=\"planEndDate != null\">#{planEndDate},</if>\n" +
    //         "    <if test=\"pjEstablishStartDate != null\">#{pjEstablishStartDate},</if>\n" +
    //         "    <if test=\"pjEstablishPublishDate != null\">#{pjEstablishPublishDate},</if>\n" +
    //         "    <if test=\"isAcceptance != null\">#{isAcceptance},</if>\n" +
    //         "    <if test=\"isPlatformPublish != null\">#{isPlatformPublish},</if>\n" +
    //         "    <if test=\"resourceAssessment != null\">#{resourceAssessment},</if>\n" +
    //         "    <if test=\"createMan != null\">#{createMan},</if>\n" +
    //         "    <if test=\"createManId != null\">#{createManId},</if>\n" +
    //         "    <if test=\"true\">\n" +
    //         "                concat(',', (select `AUTO_INCREMENT`\n" +
    //         "                from information_schema.TABLES\n" +
    //         "                where TABLE_SCHEMA = 'info_forward'\n" +
    //         "                and TABLE_NAME = 'project'), ',')\n</if>" +
    //         ")" +
    //         "</script>")
    // public void insert(ProjectDO projectDO);
}
