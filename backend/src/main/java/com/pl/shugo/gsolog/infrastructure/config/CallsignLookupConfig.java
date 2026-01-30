package com.pl.shugo.gsolog.infrastructure.config;

import com.pl.shugo.gsolog.domain.port.CallsignLookupPort;
import com.pl.shugo.gsolog.infrastructure.adapter.HamQthCallsignLookupAdapter;
import com.pl.shugo.gsolog.infrastructure.adapter.MockCallsignLookupAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for callsign lookup adapters.
 * Uses HamQTH if credentials are configured, otherwise falls back to mock.
 */
@Configuration
public class CallsignLookupConfig {

    private static final Logger logger = LoggerFactory.getLogger(CallsignLookupConfig.class);

    @Value("${hamqth.base-url:https://www.hamqth.com}")
    private String hamqthBaseUrl;

    @Value("${hamqth.username:}")
    private String hamqthUsername;

    @Value("${hamqth.password:}")
    private String hamqthPassword;

    @Bean
    public CallsignLookupPort callsignLookupPort() {
        if (hamqthUsername != null && !hamqthUsername.isEmpty() &&
            hamqthPassword != null && !hamqthPassword.isEmpty()) {
            logger.info("Using HamQTH callsign lookup adapter");
            return new HamQthCallsignLookupAdapter(hamqthBaseUrl, hamqthUsername, hamqthPassword);
        } else {
            logger.info("Using mock callsign lookup adapter (HamQTH credentials not configured)");
            return new MockCallsignLookupAdapter();
        }
    }
}
