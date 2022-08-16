package com.timevale.forward.service.excel.track;

import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.metadata.CellData;
import com.alibaba.excel.metadata.Head;
import com.alibaba.excel.write.handler.CellWriteHandler;
import com.alibaba.excel.write.metadata.holder.WriteSheetHolder;
import com.alibaba.excel.write.metadata.holder.WriteTableHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;

import java.util.List;

/**
 * @author by YangXu
 * @date 2022/08/12 12:34
 */
@Slf4j
public class TrackStyleStrategy implements CellWriteHandler {

    @Override
    public void beforeCellCreate(WriteSheetHolder writeSheetHolder, WriteTableHolder writeTableHolder, Row row, Head head, Integer columnIndex, Integer relativeRowIndex, Boolean isHead) {

    }

    @Override
    public void afterCellCreate(WriteSheetHolder writeSheetHolder, WriteTableHolder writeTableHolder, Cell cell, Head head, Integer relativeRowIndex, Boolean isHead) {

    }

    @Override
    public void afterCellDataConverted(WriteSheetHolder writeSheetHolder, WriteTableHolder writeTableHolder, CellData cellData, Cell cell, Head head, Integer relativeRowIndex, Boolean isHead) {

    }

    @Override
    public void afterCellDispose(WriteSheetHolder writeSheetHolder, WriteTableHolder writeTableHolder, List<CellData> cellDataList, Cell cell, Head head, Integer relativeRowIndex, Boolean isHead) {
        int columnIndex = cell.getColumnIndex();

        Sheet sheet = cell.getSheet();
        Workbook workbook = sheet.getWorkbook();
        CellStyle cellStyle = workbook.createCellStyle();
        cellStyle.cloneStyleFrom(cellStyle);

        // 字体
        Font font = workbook.createFont();
        font.setFontName("宋体");
        if (columnIndex == 0) {
            font.setFontHeightInPoints((short) 10);
            font.setColor(IndexedColors.RED.index);
            cellStyle.setVerticalAlignment(VerticalAlignment.TOP);
        } else {
            font.setFontHeightInPoints((short) 12);
        }
        cellStyle.setFont(font);
        // 自动换行
        cellStyle.setWrapText(true);

        cell.setCellStyle(cellStyle);
    }
}
