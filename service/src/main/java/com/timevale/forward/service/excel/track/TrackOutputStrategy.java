package com.timevale.forward.service.excel.track;

import com.alibaba.excel.metadata.CellData;
import com.alibaba.excel.metadata.Head;
import com.alibaba.excel.write.handler.CellWriteHandler;
import com.alibaba.excel.write.metadata.holder.WriteSheetHolder;
import com.alibaba.excel.write.metadata.holder.WriteTableHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;

import java.util.List;
import java.util.Map;

/**
 * @author by YangXu
 * @date 2022/08/12 12:34
 */
@Slf4j
public class TrackOutputStrategy implements CellWriteHandler {

    private final Map<Integer, Integer> mergeInfo;
    private final int[] mergeColumns = new int[]{0,1,2,3,4,5,6,7,8,12,13,14,15,16};

    public TrackOutputStrategy(Map<Integer, Integer> mergeInfo) {
        this.mergeInfo = mergeInfo;
    }

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
        int rowIndex = cell.getRowIndex();
        int columnIndex = cell.getColumnIndex();

        Integer size = mergeInfo.get(rowIndex);
        if (columnIndex != 0 || size == null || size == 1) {
            return;
        }

        Sheet sheet = writeSheetHolder.getSheet();
        for (int column : mergeColumns) {
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex + size - 1, column, column));
        }
    }
}
