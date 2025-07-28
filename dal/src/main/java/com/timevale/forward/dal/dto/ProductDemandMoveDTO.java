package com.timevale.forward.dal.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductDemandMoveDTO {

   // 目标分组id
   private Long targetGroupId;

   // 拖动ID, mode为moveIn时为null
   private Long moveItemId;

   // 产品需求id
   private Long productDemandId;

   // 原分组id
   private Long moveGroupId;

   // 忽略移动
   private Boolean ignore = false;

}