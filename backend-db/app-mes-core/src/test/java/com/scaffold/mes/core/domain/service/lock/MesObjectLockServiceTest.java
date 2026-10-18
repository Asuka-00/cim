package com.scaffold.mes.core.domain.service.lock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

import com.scaffold.mes.core.domain.MesErrorCodes;
import com.scaffold.mes.core.domain.entity.lock.MesObjectLock;
import com.scaffold.mes.core.domain.repository.lock.MesObjectLockRepository;
import com.scaffold.share.exception.BusinessException;

/**
 * 对象锁的占用、续期、过期接管和释放。
 */
class MesObjectLockServiceTest {

    private MesObjectLockRepository repository;
    private MesObjectLockService service;

    @BeforeEach
    void setUp() {
        repository = org.mockito.Mockito.mock(MesObjectLockRepository.class);
        service = new MesObjectLockService(repository);
    }

    @Test
    void acquireInsertsWhenFree() {
        when(repository.find("BATCH", 9L)).thenReturn(Optional.empty());

        service.acquire("BATCH", 9L, "alice", "组批", Duration.ofMinutes(10));

        ArgumentCaptor<MesObjectLock> captor = ArgumentCaptor.forClass(MesObjectLock.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getLockOwner()).isEqualTo("alice");
        assertThat(captor.getValue().getExpireAt()).isAfter(LocalDateTime.now());
    }

    @Test
    void sameOwnerExtendsExpiry() {
        MesObjectLock held = heldBy("alice", LocalDateTime.now().plusMinutes(1));
        when(repository.find("BATCH", 9L)).thenReturn(Optional.of(held));

        service.acquire("BATCH", 9L, "alice", "继续组批", Duration.ofMinutes(10));

        verify(repository).update(held);
        verify(repository, never()).save(any());
        assertThat(held.getPurpose()).isEqualTo("继续组批");
    }

    @Test
    void otherOwnerIsRejectedWhileActive() {
        when(repository.find("BATCH", 9L))
                .thenReturn(Optional.of(heldBy("alice", LocalDateTime.now().plusMinutes(5))));

        assertThatThrownBy(() -> service.acquire("BATCH", 9L, "bob", "组批", Duration.ofMinutes(10)))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesErrorCodes.OBJECT_BUSY);
    }

    @Test
    void expiredLockCanBeTakenOver() {
        MesObjectLock expired = heldBy("alice", LocalDateTime.now().minusSeconds(1));
        expired.setId(3L);
        when(repository.find("LOCATION", 2L)).thenReturn(Optional.of(expired));

        service.acquire("LOCATION", 2L, "bob", "盘点", Duration.ofMinutes(5));

        verify(repository).deleteById(3L);
        verify(repository).save(any());
    }

    @Test
    void duplicateInsertBecomesBusy() {
        when(repository.find("BATCH", 9L)).thenReturn(Optional.empty());
        org.mockito.Mockito.doThrow(new DuplicateKeyException("uk")).when(repository).save(any());

        assertThatThrownBy(() -> service.acquire("BATCH", 9L, "alice", null, Duration.ofMinutes(1)))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesErrorCodes.OBJECT_BUSY);
    }

    @Test
    void releaseByOwnerDeletesAndMissingLockIsIgnored() {
        MesObjectLock held = heldBy("alice", LocalDateTime.now().plusMinutes(1));
        held.setId(8L);
        when(repository.find("BATCH", 9L)).thenReturn(Optional.of(held), Optional.empty());

        service.release("BATCH", 9L, "alice");
        service.release("BATCH", 9L, "alice");

        verify(repository).deleteById(8L);
    }

    @Test
    void releaseByOtherOwnerIsRejected() {
        when(repository.find("BATCH", 9L))
                .thenReturn(Optional.of(heldBy("alice", LocalDateTime.now().plusMinutes(1))));

        assertThatThrownBy(() -> service.release("BATCH", 9L, "bob"))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesErrorCodes.OBJECT_BUSY);
    }

    private static MesObjectLock heldBy(String owner, LocalDateTime expireAt) {
        MesObjectLock lock = new MesObjectLock();
        lock.setObjectType("BATCH");
        lock.setObjectId(9L);
        lock.setLockOwner(owner);
        lock.setExpireAt(expireAt);
        return lock;
    }
}
