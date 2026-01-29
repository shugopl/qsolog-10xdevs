package com.pl.shugo.gsolog.domain.validation;

import com.pl.shugo.gsolog.domain.enums.AdifMode;
import com.pl.shugo.gsolog.domain.enums.AdifSubmode;
import com.pl.shugo.gsolog.domain.enums.Band;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * QSO validation service.
 * Enforces ADIF mode/submode/customMode rules.
 */
@Component
public class QsoValidator {

    private static final Set<AdifSubmode> MFSK_SUBMODES = Set.of(
            AdifSubmode.FT8,
            AdifSubmode.FT4,
            AdifSubmode.JS8
    );

    private static final Set<AdifSubmode> PSK_SUBMODES = Set.of(
            AdifSubmode.PSK31,
            AdifSubmode.PSK63,
            AdifSubmode.PSK125
    );

    /**
     * Validate QSO mode, submode, and customMode combination.
     *
     * Rules:
     * - If submode in {FT8, FT4, JS8} → mode must be MFSK
     * - If submode in {PSK31, PSK63, PSK125} → mode must be PSK
     * - If customMode not null → mode must be DATA and submode must be null
     *
     * @return List of validation error messages (empty if valid)
     */
    public List<String> validateModeConfiguration(AdifMode mode, AdifSubmode submode, String customMode) {
        List<String> errors = new ArrayList<>();

        // Rule 1: If customMode is set, mode must be DATA and submode must be null
        if (customMode != null && !customMode.isBlank()) {
            if (mode != AdifMode.DATA) {
                errors.add("When customMode is specified, mode must be DATA");
            }
            if (submode != null) {
                errors.add("When customMode is specified, submode must be null");
            }
        }

        // Rule 2: If submode is set, validate mode/submode combination
        if (submode != null) {
            if (MFSK_SUBMODES.contains(submode) && mode != AdifMode.MFSK) {
                errors.add(String.format("Submode %s requires mode MFSK, but got %s", submode, mode));
            } else if (PSK_SUBMODES.contains(submode) && mode != AdifMode.PSK) {
                errors.add(String.format("Submode %s requires mode PSK, but got %s", submode, mode));
            }
        }

        return errors;
    }

    /**
     * Validate band is a valid ADIF band.
     *
     * @return true if valid, false otherwise
     */
    public boolean isValidBand(String band) {
        if (band == null || band.isBlank()) {
            return false;
        }
        try {
            Band.fromAdifValue(band);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Get validation error message for band.
     */
    public String getBandValidationError(String band) {
        return String.format("Invalid band '%s'. Must be a valid ADIF band (e.g., 160m, 80m, 40m, 20m, etc.)", band);
    }
}
