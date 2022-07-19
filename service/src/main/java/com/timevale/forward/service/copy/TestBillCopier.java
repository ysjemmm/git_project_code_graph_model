package com.timevale.forward.service.copy;

import com.timevale.forward.dal.entity.TestBillDO;
import com.timevale.forward.facade.api.request.TestBillAddReq;
import com.timevale.forward.facade.api.request.TestBillModifyReq;
import com.timevale.forward.facade.api.result.TestBillDocumentVO;
import com.timevale.forward.facade.api.result.TestBillVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * @Date 2022/1/24 17:31
 * @Author 望轩
 */
@Mapper
public interface TestBillCopier {
    TestBillCopier INSTANCE = Mappers.getMapper(TestBillCopier.class);

    /**
     * TestBillDO -> TestBillVO
     *
     * @param testBillDO 源对象
     * @return 目标对象
     */
    TestBillVO convert(TestBillDO testBillDO);

    /**
     * TestBillAddReq -> TestBillDO
     *
     * @param testBillAddReq 源对象
     * @return 目标对象
     */
    TestBillDO transform(TestBillAddReq testBillAddReq);

    /**
     * TestBillModifyReq -> TestBillDO
     *
     * @param testBillModifyReq 源对象
     * @return 目标对象
     */
    TestBillDO change(TestBillModifyReq testBillModifyReq);

    @Mapping(target = "files", ignore = true)
    TestBillDocumentVO convert2Doc(TestBillDO testBill);
}
