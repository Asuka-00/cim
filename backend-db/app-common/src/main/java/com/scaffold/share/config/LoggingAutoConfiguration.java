package com.scaffold.share.config;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.TimeZone;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.scaffold.share.aop.AuditLogAspect;
import com.scaffold.share.aop.PerformanceLogAspect;
import com.scaffold.share.audit.OperationLogService;
import com.scaffold.share.context.CurrentSessionProvider;
import com.scaffold.share.logging.AuditPayloadSerializer;
import com.scaffold.share.logging.TraceIdFilter;
import com.scaffold.share.logging.TraceIdInterceptor;

import lombok.RequiredArgsConstructor;

/**
 * LoggingAutoConfiguration配置类
 * <p>
 * 负责提供LoggingAutoConfiguration相关配置与组件装配
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@AutoConfiguration
@EnableConfigurationProperties(LoggingProperties.class)
@RequiredArgsConstructor
public class LoggingAutoConfiguration implements WebMvcConfigurer {

    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);

    /**
     * 宽松日期时间解析器:序列化仍用 {@link #DATE_TIME_FORMATTER}(yyyy-MM-dd HH:mm:ss),
     * 但反序列化时同时兼容「空格分隔」与 ISO-8601 的「T 分隔」,秒/毫秒可选,
     * 避免客户端发送 {@code 2026-06-17T09:24:48} 时报「字段类型不匹配」(code 100002)。
     */
    private static final DateTimeFormatter LENIENT_DATE_TIME_PARSER =
            new DateTimeFormatterBuilder()
                    .appendPattern("yyyy-MM-dd")
                    // 日期与时间之间的分隔符:T 或 空格,二选一
                    .optionalStart().appendLiteral('T').optionalEnd()
                    .optionalStart().appendLiteral(' ').optionalEnd()
                    .appendPattern("HH:mm")
                    .optionalStart().appendLiteral(':').appendPattern("ss").optionalEnd()
                    .optionalStart().appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true).optionalEnd()
                    .toFormatter();

    private final LoggingProperties loggingProperties;
    private final CurrentSessionProvider currentSessionProvider;

    /**
     * HTTP 入口 Filter：尽早建立 traceId，请求结束清理 MDC。
     */
    @Bean
    public FilterRegistrationBean<TraceIdFilter> traceIdFilterRegistration() {
        FilterRegistrationBean<TraceIdFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new TraceIdFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.addUrlPatterns("/*");
        return registration;
    }

    /**
     * 注册会话补全拦截器（user/tenant；trace 由 TraceIdFilter 负责）
     */
    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(new TraceIdInterceptor(currentSessionProvider))
                .addPathPatterns("/**")
                .excludePathPatterns("/swagger-ui/**", "/v3/api-docs/**", "/favicon.ico");
    }

    /**
     * ObjectMapper - 用于JSON序列化
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(javaTimeModule());
        mapper.registerModule(new com.scaffold.share.jackson.TrimStringModule());
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_TIME_PATTERN);
        dateFormat.setTimeZone(TimeZone.getDefault());
        mapper.setDateFormat(dateFormat);
        mapper.setTimeZone(TimeZone.getDefault());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    private JavaTimeModule javaTimeModule() {
        JavaTimeModule module = new JavaTimeModule();
        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DATE_TIME_FORMATTER));
        module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(LENIENT_DATE_TIME_PARSER));
        module.addSerializer(Instant.class, new InstantDateTimeSerializer());
        module.addDeserializer(Instant.class, new InstantDateTimeDeserializer());
        return module;
    }

    private static class InstantDateTimeSerializer extends com.fasterxml.jackson.databind.JsonSerializer<Instant> {

        @Override
        public void serialize(Instant value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString(DATE_TIME_FORMATTER.format(LocalDateTime.ofInstant(value, ZoneId.systemDefault())));
        }
    }

    /**
     * Instant 反序列化：优先 ISO-8601 绝对时间（{@code ...Z} / offset），再回退无时区本地时间。
     */
    private static class InstantDateTimeDeserializer extends JsonDeserializer<Instant> {

        @Override
        public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String text = p.getText();
            if (text == null || text.isBlank()) {
                return null;
            }
            text = text.trim();

            // 1) ISO-8601 UTC / offset，如 2026-07-30T08:00:00Z、...+00:00
            try {
                return Instant.parse(text);
            } catch (DateTimeParseException ignored) {
                // fall through
            }

            // 2) 带 offset 但形态略异于 Instant.parse 严格规则时
            try {
                return DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(text, Instant::from);
            } catch (DateTimeParseException ignored) {
                // fall through
            }

            // 3) 无时区本地时间（空格或 T）：按系统默认时区转 Instant
            try {
                LocalDateTime local = LocalDateTime.parse(text, LENIENT_DATE_TIME_PARSER);
                return local.atZone(ZoneId.systemDefault()).toInstant();
            } catch (DateTimeParseException ex) {
                return (Instant) ctxt.handleWeirdStringValue(Instant.class, text,
                        "期望 ISO-8601 Instant(如 2026-07-30T08:00:00Z) 或本地时间(yyyy-MM-dd[ T]HH:mm[:ss])");
            }
        }
    }

    /**
     * 性能日志切面
     */
    @Bean
    public PerformanceLogAspect performanceLogAspect() {
        return new PerformanceLogAspect(loggingProperties);
    }

    /**
     * 审计payload序列化器
     */
    @Bean
    public AuditPayloadSerializer auditPayloadSerializer(ObjectMapper objectMapper) {
        return new AuditPayloadSerializer(objectMapper);
    }

    /**
     * 审计日志切面
     */
    @Bean
    @ConditionalOnProperty(name = "logging.audit-enabled", havingValue = "true", matchIfMissing = true)
    public AuditLogAspect auditLogAspect(AuditPayloadSerializer auditPayloadSerializer,
            OperationLogService operationLogService) {
        return new AuditLogAspect(loggingProperties, auditPayloadSerializer, operationLogService);
    }
}
