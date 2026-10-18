package com.scaffold.mes.exec.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.scaffold.mes.exec.domain.dto.ImportWipRowRequest;
import com.scaffold.share.exception.BusinessException;

class WipImportCsvTest {

    @Test
    void parsesOneDataRow() {
        List<ImportWipRowRequest> rows = WipImportCsv.parse("""
                woId,lotNo,qty,seqNo,stepId
                4,L-1,1,1,7
                """);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getRowNo()).isEqualTo(1);
        assertThat(rows.get(0).getWoId()).isEqualTo(4L);
        assertThat(rows.get(0).getRow().getLotNo()).isEqualTo("L-1");
        assertThat(rows.get(0).getRow().getQty()).isEqualTo(1L);
        assertThat(rows.get(0).getRow().getSeqNo()).isEqualTo(1);
        assertThat(rows.get(0).getRow().getStepId()).isEqualTo(7L);
    }

    @Test
    void numbersDataRowsAfterSkippingBlankLines() {
        List<ImportWipRowRequest> rows = WipImportCsv.parse("""
                woId,lotNo,qty,seqNo,stepId

                4,L-1,1,1,7
                4,L-2,2,2,7
                """);

        assertThat(rows).extracting(ImportWipRowRequest::getRowNo).containsExactly(1, 2);
        assertThat(rows.get(1).getRow().getLotNo()).isEqualTo("L-2");
    }

    @Test
    void rejectsUnknownHeader() {
        assertThatThrownBy(() -> WipImportCsv.parse("woId,lotNo,qty,seqNo,stepId,nope\n"))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode(), Throwable::getMessage)
                .containsExactly(400190, "导入文件表头无法识别");
    }

    @Test
    void rejectsUnparsedCell() {
        assertThatThrownBy(() -> WipImportCsv.parse("""
                woId,lotNo,qty,seqNo,stepId
                4,L-1,x,1,7
                """))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode(), Throwable::getMessage)
                .containsExactly(400190, "第1行无法解析");
    }
}
