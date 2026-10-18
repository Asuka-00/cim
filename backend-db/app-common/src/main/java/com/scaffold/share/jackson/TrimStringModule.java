package com.scaffold.share.jackson;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * TrimStringModule类
 * <p>
 * 负责处理TrimStringModule相关核心业务能力
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
public class TrimStringModule extends SimpleModule {

    public TrimStringModule() {
        super("TrimStringModule");
        addDeserializer(String.class, new TrimStringDeserializer());
    }

    private static class TrimStringDeserializer extends JsonDeserializer<String> {
        /**
         * 执行反序列化
         */
        @Override
        public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String value = p.getValueAsString();
            if (value == null)
                return null;
            String trimmed = value.trim();
            return trimmed.isEmpty() ? null : trimmed;
        }
    }
}
