package com.scaffold.mes.exec.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.LotChildResponse;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.entity.LotRelation;
import com.scaffold.mes.exec.domain.repository.LotRelationRepository;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.share.exception.BusinessException;

/**
 * 只列出 SPLIT 出去的子批。父 Lot 不存在时拒绝。
 */
class LotGenealogyApplicationServiceTest {

    private LotRepository lots;
    private LotRelationRepository relations;
    private LotGenealogyApplicationService service;

    @BeforeEach
    void setUp() {
        lots = mock(LotRepository.class);
        relations = mock(LotRelationRepository.class);
        when(lots.findById(8L)).thenReturn(Optional.of(lot(8L, "L-1")));
        service = new LotGenealogyApplicationService(lots, relations);
    }

    @Test
    void listsSplitChildren() {
        when(relations.listSplits(8L)).thenReturn(List.of(split(9L, 2L), split(10L, 1L)));
        when(lots.findById(9L)).thenReturn(Optional.of(lot(9L, "L-1.1")));
        when(lots.findById(10L)).thenReturn(Optional.of(lot(10L, "L-1.2")));

        List<LotChildResponse> children = service.children(8L);

        assertThat(children).containsExactly(new LotChildResponse(9L, "L-1.1", 2L), new LotChildResponse(10L, "L-1.2", 1L));
    }

    @Test
    void missingParentRejected() {
        when(lots.findById(8L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.children(8L))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.LOT_NOT_FOUND);
    }

    @Test
    void findsSplitParent() {
        when(lots.findById(9L)).thenReturn(Optional.of(lot(9L, "L-1.1")));
        LotRelation relation = split(9L, 2L);
        relation.setParentLotId(8L);
        when(relations.findSplitByChild(9L)).thenReturn(Optional.of(relation));

        assertThat(service.parent(9L)).contains(new LotChildResponse(8L, "L-1", 2L));
    }

    @Test
    void lotWithoutSplitHasNoParent() {
        when(lots.findById(9L)).thenReturn(Optional.of(lot(9L, "L-2")));
        when(relations.findSplitByChild(9L)).thenReturn(Optional.empty());

        assertThat(service.parent(9L)).isEmpty();
    }

    @Test
    void listsMergeSources() {
        LotRelation relation = split(8L, 3L);
        relation.setParentLotId(9L);
        relation.setRelationType("MERGE");
        when(relations.listMergesInto(8L)).thenReturn(List.of(relation));
        when(lots.findById(9L)).thenReturn(Optional.of(lot(9L, "L-2")));

        assertThat(service.mergeSources(8L)).containsExactly(new LotChildResponse(9L, "L-2", 3L));
    }

    private static Lot lot(long id, String lotNo) {
        Lot lot = new Lot();
        lot.setId(id);
        lot.setLotNo(lotNo);
        return lot;
    }

    private static LotRelation split(long childId, long qty) {
        LotRelation relation = new LotRelation();
        relation.setChildLotId(childId);
        relation.setQty(qty);
        relation.setRelationType("SPLIT");
        return relation;
    }
}
