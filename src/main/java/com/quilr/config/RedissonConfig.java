package com.quilr.config;

import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.redisson.client.codec.BaseCodec;
import org.redisson.client.protocol.Decoder;
import org.redisson.client.protocol.Encoder;
import org.redisson.config.Config;
import org.redisson.spring.starter.RedissonAutoConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.nio.charset.StandardCharsets;

@Configuration
@Slf4j
public class RedissonConfig {

    private static final String EMPTY = "";
    private static final String NON_PRINTABLE_CHARS = "\\P{Print}";
    private ObjectMapper jsonObjectMapper = null;

    @PostConstruct
    public void init() {
        jsonObjectMapper = JsonMapper.builder().enable(JsonWriteFeature.ESCAPE_NON_ASCII).addModule(new JavaTimeModule()).build();
    }

    @Bean
    RedissonAutoConfigurationCustomizer redisConfigCustomizer() {
        return new RedissonAutoConfigurationCustomizer() {

            @Override
            public void customize(Config configuration) {
                configuration.setCodec(new BaseCodec() {

                    @Override
                    public Decoder<Object> getValueDecoder() {
                        return (buf, state) -> {
                            byte[] bytes = new byte[buf.readableBytes()];
                            buf.readBytes(bytes);
                            String cleanedstring = new String(bytes, StandardCharsets.UTF_8).replaceAll(NON_PRINTABLE_CHARS, EMPTY);
                            return jsonObjectMapper.readValue(cleanedstring, Object.class);

                        };

                    }

                    @Override
                    public Encoder getValueEncoder() {
                        return (in) -> {
                            //log.info("> using redisson config to encode value: {}", in);
                            String objectval = jsonObjectMapper.writeValueAsString(in);
                            ByteBuf buf = Unpooled.copiedBuffer(objectval, StandardCharsets.UTF_8);
                            return buf;
                        };
                    }
                });
            }
        };

    }
}
