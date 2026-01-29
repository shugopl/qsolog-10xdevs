package com.pl.shugo.gsolog.domain.enums;

/**
 * ADIF standard ham radio bands.
 */
public enum Band {
    BAND_160M("160m"),
    BAND_80M("80m"),
    BAND_60M("60m"),
    BAND_40M("40m"),
    BAND_30M("30m"),
    BAND_20M("20m"),
    BAND_17M("17m"),
    BAND_15M("15m"),
    BAND_12M("12m"),
    BAND_10M("10m"),
    BAND_6M("6m"),
    BAND_4M("4m"),
    BAND_2M("2m"),
    BAND_1_25M("1.25m"),
    BAND_70CM("70cm"),
    BAND_33CM("33cm"),
    BAND_23CM("23cm"),
    BAND_13CM("13cm"),
    BAND_9CM("9cm"),
    BAND_6CM("6cm"),
    BAND_3CM("3cm"),
    BAND_1_25CM("1.25cm"),
    BAND_6MM("6mm"),
    BAND_4MM("4mm"),
    BAND_2_5MM("2.5mm"),
    BAND_2MM("2mm"),
    BAND_1MM("1mm");

    private final String adifValue;

    Band(String adifValue) {
        this.adifValue = adifValue;
    }

    public String getAdifValue() {
        return adifValue;
    }

    public static Band fromAdifValue(String adifValue) {
        for (Band band : values()) {
            if (band.adifValue.equalsIgnoreCase(adifValue)) {
                return band;
            }
        }
        throw new IllegalArgumentException("Unknown ADIF band: " + adifValue);
    }
}
