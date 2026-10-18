package com.scaffold.mes.core.domain.service.print;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.core.domain.MesErrorCodes;
import com.scaffold.mes.core.domain.dto.print.PrintTemplateCreateRequest;
import com.scaffold.mes.core.domain.dto.print.PrintTemplateResponse;
import com.scaffold.mes.core.domain.entity.print.PrintTemplate;
import com.scaffold.mes.core.domain.repository.print.PrintTemplateRepository;
import com.scaffold.share.exception.BusinessException;

/**
 * 新建打印模板。可以按类型查出默认模板。停用后按类型查不到它。
 */
class PrintTemplateApplicationServiceTest {

    private PrintTemplateRepository repository;
    private PrintTemplateApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(PrintTemplateRepository.class);
        when(repository.findByCode("LOT-1")).thenReturn(Optional.empty());
        service = new PrintTemplateApplicationService(repository);
    }

    @Test
    void createsEnabledTemplate() {
        service.create(request("LOT-1", "LOT_TRAVELER", "<p>{{lot.lotNo}}</p>"));

        verify(repository).save(org.mockito.ArgumentMatchers.<PrintTemplate>argThat(row ->
                "流程单".equals(row.getTplName())
                        && "<p>{{lot.lotNo}}</p>".equals(row.getContent())
                        && Integer.valueOf(1).equals(row.getEnabled())));
    }

    @Test
    void duplicateCodeRejected() {
        when(repository.findByCode("LOT-1")).thenReturn(Optional.of(new PrintTemplate()));

        assertThatThrownBy(() -> service.create(request("LOT-1", "LOT_TRAVELER", "<p></p>")))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesErrorCodes.PRINT_TEMPLATE_INVALID);
    }

    @Test
    void badTypeRejected() {
        assertThatThrownBy(() -> service.create(request("LOT-1", "BOX", "<p></p>")))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesErrorCodes.PRINT_TEMPLATE_INVALID);
    }

    @Test
    void setDefaultClearsPrevious() {
        PrintTemplate chosen = new PrintTemplate();
        chosen.setId(3L);
        chosen.setTplType("LOT_TRAVELER");
        chosen.setEnabled(1);
        PrintTemplate previous = new PrintTemplate();
        previous.setId(4L);
        previous.setIsDefault(1);
        when(repository.findById(3L)).thenReturn(Optional.of(chosen));
        when(repository.listByType("LOT_TRAVELER")).thenReturn(List.of(previous, chosen));

        service.setDefault(3L);

        verify(repository).update(org.mockito.ArgumentMatchers.<PrintTemplate>argThat(row ->
                Long.valueOf(4L).equals(row.getId()) && Integer.valueOf(0).equals(row.getIsDefault())));
        verify(repository).update(org.mockito.ArgumentMatchers.<PrintTemplate>argThat(row ->
                Long.valueOf(3L).equals(row.getId()) && Integer.valueOf(1).equals(row.getIsDefault())));
    }

    @Test
    void setDefaultRejectsDisabled() {
        PrintTemplate chosen = new PrintTemplate();
        chosen.setEnabled(0);
        when(repository.findById(3L)).thenReturn(Optional.of(chosen));

        assertThatThrownBy(() -> service.setDefault(3L))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesErrorCodes.PRINT_TEMPLATE_INVALID);
    }

    @Test
    void showsDefaultByType() {
        PrintTemplate template = new PrintTemplate();
        template.setTplCode("LOT-1");
        template.setTplName("流程单");
        template.setContent("<p>{{lot.lotNo}}</p>");
        when(repository.findDefault("LOT_TRAVELER")).thenReturn(Optional.of(template));

        PrintTemplateResponse response = service.showDefault("LOT_TRAVELER");

        assertThat(response.tplCode()).isEqualTo("LOT-1");
        assertThat(response.content()).isEqualTo("<p>{{lot.lotNo}}</p>");
    }

    @Test
    void showDefaultRejectsMissing() {
        when(repository.findDefault("LOT_TRAVELER")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.showDefault("LOT_TRAVELER"))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesErrorCodes.PRINT_TEMPLATE_INVALID);
    }

    @Test
    void disableHidesDefault() {
        PrintTemplate template = new PrintTemplate();
        template.setTplCode("LOT-1");
        template.setTplName("流程单");
        template.setContent("<p></p>");
        template.setEnabled(1);
        template.setIsDefault(1);
        when(repository.findById(3L)).thenReturn(Optional.of(template));
        when(repository.findDefault("LOT_TRAVELER")).thenAnswer(invocation ->
                Integer.valueOf(1).equals(template.getEnabled()) && Integer.valueOf(1).equals(template.getIsDefault())
                        ? Optional.of(template) : Optional.empty());

        service.disable(3L);

        assertThatThrownBy(() -> service.showDefault("LOT_TRAVELER"))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesErrorCodes.PRINT_TEMPLATE_INVALID);
    }

    private static PrintTemplateCreateRequest request(String code, String type, String content) {
        PrintTemplateCreateRequest request = new PrintTemplateCreateRequest();
        request.setTplCode(code);
        request.setTplName("流程单");
        request.setTplType(type);
        request.setContent(content);
        return request;
    }
}
