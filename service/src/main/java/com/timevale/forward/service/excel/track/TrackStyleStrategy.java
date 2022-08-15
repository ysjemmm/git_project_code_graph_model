package com.timevale.forward.service.excel.track;

import com.alibaba.excel.metadata.Head;
import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import com.alibaba.excel.write.metadata.style.WriteFont;
import com.alibaba.excel.write.style.AbstractVerticalCellStyleStrategy;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.VerticalAlignment;

/**
 * @author by YangXu
 * @date 2022/08/12 12:34
 */
@Slf4j
public class TrackStyleStrategy extends AbstractVerticalCellStyleStrategy  {

    @Override
    protected WriteCellStyle headCellStyle(Head head) {
        log.info("headCellStyle");
        WriteCellStyle writeCellStyle = new WriteCellStyle();
        writeCellStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
        return writeCellStyle;
    }

    @Override
    protected WriteCellStyle contentCellStyle(Head head) {
        log.info("contentCellStyle");
        WriteCellStyle writeCellStyle = new WriteCellStyle();
        WriteFont writeFont = new WriteFont();

        // 字体信息
        writeFont.setFontHeightInPoints((short)12);
        writeFont.setColor(IndexedColors.RED.index);
        // 错误信息列：红色字体
        Integer columnIndex = head.getColumnIndex();
        if (columnIndex == 0) {

        }
        writeCellStyle.setWriteFont(writeFont);

        // 自动换行
        writeCellStyle.setWrapped(true);
        //设置 垂直居中
        writeCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        //设置 水平居左边
        writeCellStyle.setHorizontalAlignment(HorizontalAlignment.LEFT);

        return writeCellStyle;
    }
}
