package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.SearchConditionMapper;
import com.timevale.forward.dal.entity.BaseDO;
import com.timevale.forward.dal.entity.SearchConditionDO;
import com.timevale.forward.facade.api.client.SearchConditionService;
import com.timevale.forward.facade.api.query.SearchConditionQueryList;
import com.timevale.forward.facade.api.request.SearchConditionAddReq;
import com.timevale.forward.facade.api.request.SearchConditionDefaultReq;
import com.timevale.forward.facade.api.request.SearchConditionDeleteReq;
import com.timevale.forward.facade.api.request.SearchConditionModifyReq;
import com.timevale.forward.facade.api.request.SearchConditionShareReq;
import com.timevale.forward.facade.api.result.SearchConditionVO;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.SearchConditionCopier;
import com.timevale.forward.service.utils.aop.LogPoint;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.common.annotation.RestService;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


/**
 * @author by YangXu
 * @date 2022/04/21 17:10
 */
@Slf4j
@LogPoint
@RestService
public class SearchConditionServiceImpl implements SearchConditionService {

    @Resource
    SearchConditionMapper searchConditionMapper;

    @Override
    public BaseResult<List<SearchConditionVO>> list(SearchConditionQueryList searchConditionQueryList) {
        // 参数
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        Integer model = searchConditionQueryList.getModel();
        Integer tabType = searchConditionQueryList.getTabType();

        // 查询并转换
        List<SearchConditionDO> searchConditionDOList = searchConditionMapper.select(model, tabType, userInfo.getId());
        List<SearchConditionVO> searchConditionVOList = searchConditionDOList.stream().map(SearchConditionCopier.INSTANCE::convert).collect(Collectors.toList());

        boolean isDefault = searchConditionVOList.stream().anyMatch(SearchConditionVO::getIsDefault);
        if(isDefault){
            searchConditionVOList.sort((a, b) -> b.getIsDefault().compareTo(a.getIsDefault()));
        }

        // 系统默认条件
        SearchConditionVO systemDefault = new SearchConditionVO();
        systemDefault.setId(0L);
        systemDefault.setContent("");
        systemDefault.setModel(model);
        systemDefault.setNotDelete(true);
        systemDefault.setTabType(tabType);
        systemDefault.setName(CommonConstant.SYSTEM_DEFAULT);
        systemDefault.setIsDefault(!isDefault);

        // 如果有默认条件，则排在默认条件之后
        int index = isDefault ? 1 : 0;
        searchConditionVOList.add(index, systemDefault);

        return BaseResult.success(searchConditionVOList);
    }

    @Override
    public BaseResult<Boolean> add(SearchConditionAddReq searchConditionAddReq) {
        // 参数
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        Integer model = searchConditionAddReq.getModel();
        Integer tabType = searchConditionAddReq.getTabType();
        String name = searchConditionAddReq.getName();

        // 查询
        List<SearchConditionDO> searchConditionDOList = searchConditionMapper.select(model, tabType, userInfo.getId());

        // 限制10条
        if(searchConditionDOList.size() == 10){
            throw new BaseBizRuntimeException("您的查询条件已超过10条，不可再添加");
        }

        // 名称唯一
        boolean match = searchConditionDOList.stream().anyMatch(e -> e.getName().equals(name));
        if(match){
            throw new BaseBizRuntimeException("名称重复");
        }

        // 转换后插入
        SearchConditionDO searchConditionDO = SearchConditionCopier.INSTANCE.convert(searchConditionAddReq);
        searchConditionMapper.insert(searchConditionDO);

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> update(SearchConditionModifyReq searchConditionModifyReq) {
        // 转换后更新
        SearchConditionDO searchConditionDO = SearchConditionCopier.INSTANCE.convert(searchConditionModifyReq);
        searchConditionMapper.update(searchConditionDO);

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> delete(SearchConditionDeleteReq searchConditionDeleteReq) {
        Long id = searchConditionDeleteReq.getId();

        // 修改删除标记,更新
        SearchConditionDO searchConditionDO = new SearchConditionDO();
        searchConditionDO.setId(id);
        searchConditionDO.setIsDeleted(true);
        searchConditionMapper.update(searchConditionDO);

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> setDefault(SearchConditionDefaultReq searchConditionDefaultReq) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        Long id = searchConditionDefaultReq.getId();
        Integer model = searchConditionDefaultReq.getModel();
        Integer tabType = searchConditionDefaultReq.getTabType();

        // 取消原有默认条件
        List<SearchConditionDO> conditionDOList = searchConditionMapper.select(model, tabType, userInfo.getId());

        Optional<Long> first = conditionDOList.stream().filter(SearchConditionDO::getIsDefault).map(BaseDO::getId).findFirst();
        if(first.isPresent()){
            Long cancelId = first.get();

            SearchConditionDO conditionDO = new SearchConditionDO();
            conditionDO.setId(cancelId);
            conditionDO.setIsDefault(false);
            searchConditionMapper.update(conditionDO);
        }

        // 更新原有数据,id == 0 则为系统默认，无需更新
        if(id != 0){
            SearchConditionDO conditionDO = new SearchConditionDO();
            conditionDO.setId(id);
            conditionDO.setIsDefault(true);
            searchConditionMapper.update(conditionDO);
        }

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> share(SearchConditionShareReq searchConditionShareReq) {
        Long id = searchConditionShareReq.getId();
        List<String> userIds = searchConditionShareReq.getUserIds();

        // 查询原始预设
        SearchConditionDO source = searchConditionMapper.selectById(id);
        if (source == null) {
            throw new BaseBizRuntimeException("查询条件不存在");
        }

        // 为每个目标用户复制一份
        for (String userId : userIds) {
            // 查询目标用户已有的预设，超过10条则跳过
            List<SearchConditionDO> existList = searchConditionMapper.select(source.getModel(), source.getTabType(), userId);
            if (existList.size() >= 10) {
                continue;
            }
            // 名称去重：若已存在同名则加后缀
            String name = source.getName();
            boolean nameExists = existList.stream().anyMatch(e -> e.getName().equals(name));
            if (nameExists) {
                name = name + "(分享)";
            }

            SearchConditionDO newDO = new SearchConditionDO();
            newDO.setName(name);
            newDO.setModel(source.getModel());
            newDO.setTabType(source.getTabType());
            newDO.setContent(source.getContent());
            newDO.setIsDefault(false);
            newDO.setBelongManId(userId);
            newDO.setBelongMan(userId);
            searchConditionMapper.insert(newDO);
        }

        return BaseResult.success(true);
    }
}
