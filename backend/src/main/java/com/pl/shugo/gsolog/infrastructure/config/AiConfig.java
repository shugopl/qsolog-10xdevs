package com.pl.shugo.gsolog.infrastructure.config;

import com.pl.shugo.gsolog.domain.port.AiHelperPort;
import com.pl.shugo.gsolog.infrastructure.adapter.MockAiHelperAdapter;
import com.pl.shugo.gsolog.infrastructure.adapter.OpenAiHelperAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for AI helper adapters.
 * Uses OpenAI if API key is configured, otherwise falls back to mock.
 */
@Configuration
public class AiConfig {

    private static final Logger logger = LoggerFactory.getLogger(AiConfig.class);

    @Value("${openai.api-key:}")
    private String openaiApiKey;

    @Bean
    public AiHelperPort aiHelperPort() {
        if (openaiApiKey != null && !openaiApiKey.isEmpty()) {
            logger.info("Using OpenAI AI helper adapter");
            return new OpenAiHelperAdapter(openaiApiKey);
        } else {
            logger.info("Using mock AI helper adapter (OpenAI API key not configured)");
            return new MockAiHelperAdapter();
        }
    }
}
