package com.pl.shugo.gsolog.api.controller;

import com.pl.shugo.gsolog.api.dto.CallsignLookupResponse;
import com.pl.shugo.gsolog.domain.port.CallsignLookupPort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Callsign lookup REST controller.
 * Provides external callsign lookup via HamQTH or mock adapter.
 */
@RestController
@RequestMapping("/api/v1/lookup")
public class LookupController {

    private final CallsignLookupPort callsignLookupPort;

    public LookupController(CallsignLookupPort callsignLookupPort) {
        this.callsignLookupPort = callsignLookupPort;
    }

    /**
     * Look up callsign information from external service.
     * Uses HamQTH if credentials are configured, otherwise returns empty.
     *
     * @param callsign Callsign to look up
     * @return Lookup response with name, QTH, grid, country if found
     */
    @GetMapping("/{callsign}")
    public Mono<CallsignLookupResponse> lookupCallsign(@PathVariable String callsign) {
        return callsignLookupPort.lookup(callsign);
    }
}
