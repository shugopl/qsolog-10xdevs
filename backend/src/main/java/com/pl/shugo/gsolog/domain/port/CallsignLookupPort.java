package com.pl.shugo.gsolog.domain.port;

import com.pl.shugo.gsolog.api.dto.CallsignLookupResponse;
import reactor.core.publisher.Mono;

/**
 * Port for callsign lookup from external services.
 * Implementations can be HamQTH, QRZ, or mock adapters.
 */
public interface CallsignLookupPort {

    /**
     * Look up callsign information from external service.
     *
     * @param callsign Callsign to look up
     * @return Mono of lookup response, or empty if not found
     */
    Mono<CallsignLookupResponse> lookup(String callsign);
}
