package com.scaffold.share.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaffold.share.context.CurrentSessionProvider;

class LoggingAutoConfigurationTest {

    @Test
    void objectMapperSerializesAndDeserializesDateTimesInStandardFormat() throws Exception {
        ObjectMapper objectMapper = new LoggingAutoConfiguration(new LoggingProperties(), emptySessionProvider())
                .objectMapper();

        SampleTimePayload payload = new SampleTimePayload();
        payload.setLocalDateTime(LocalDateTime.of(2026, 6, 8, 17, 28, 10));
        payload.setInstant(Instant.from(payload.getLocalDateTime().atZone(ZoneId.systemDefault())));
        payload.setDate(Date.from(payload.getInstant()));

        String json = objectMapper.writeValueAsString(payload);

        assertThat(json).contains("\"localDateTime\":\"2026-06-08 17:28:10\"");
        assertThat(json).contains("\"instant\":\"2026-06-08 17:28:10\"");
        assertThat(json).contains("\"date\":\"2026-06-08 17:28:10\"");

        SampleTimePayload restored = objectMapper.readValue(json, SampleTimePayload.class);
        assertThat(restored.getLocalDateTime()).isEqualTo(payload.getLocalDateTime());
        assertThat(restored.getInstant()).isEqualTo(payload.getInstant());
        assertThat(restored.getDate()).isEqualTo(payload.getDate());
    }

    private CurrentSessionProvider emptySessionProvider() {
        return new CurrentSessionProvider() {
            @Override
            public String getCurrentUserName() {
                return null;
            }

            @Override
            public String getCurrentTenantId() {
                return null;
            }
        };
    }

    static class SampleTimePayload {
        private LocalDateTime localDateTime;
        private Instant instant;
        private Date date;

        public LocalDateTime getLocalDateTime() {
            return localDateTime;
        }

        public void setLocalDateTime(LocalDateTime localDateTime) {
            this.localDateTime = localDateTime;
        }

        public Instant getInstant() {
            return instant;
        }

        public void setInstant(Instant instant) {
            this.instant = instant;
        }

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }
    }
}
