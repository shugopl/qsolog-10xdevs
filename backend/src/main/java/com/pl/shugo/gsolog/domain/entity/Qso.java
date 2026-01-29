package com.pl.shugo.gsolog.domain.entity;

import com.pl.shugo.gsolog.domain.enums.AdifMode;
import com.pl.shugo.gsolog.domain.enums.AdifSubmode;
import com.pl.shugo.gsolog.domain.enums.EqslStatus;
import com.pl.shugo.gsolog.domain.enums.LotwStatus;
import com.pl.shugo.gsolog.domain.enums.QslStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * QSO (ham radio contact) domain entity.
 * Stores ADIF-compliant contact information.
 */
@Table("qso")
public class Qso {

    @Id
    private UUID id;

    private UUID userId;

    // Required ADIF fields
    private String theirCallsign;
    private LocalDate qsoDate;
    private LocalTime timeOn;
    private String band;
    private BigDecimal frequencyKhz;

    // Mode fields
    private AdifMode mode;
    private AdifSubmode submode;
    private String customMode;

    // Optional QSO details
    private String rstSent;
    private String rstRecv;
    private String qth;
    private String gridSquare;
    private String notes;

    // QSL tracking
    private QslStatus qslStatus;
    private LotwStatus lotwStatus;
    private EqslStatus eqslStatus;

    // Timestamps
    private Instant createdAt;
    private Instant updatedAt;

    public Qso() {
    }

    public Qso(UUID id, UUID userId, String theirCallsign, LocalDate qsoDate, LocalTime timeOn,
               String band, BigDecimal frequencyKhz, AdifMode mode, AdifSubmode submode, String customMode,
               String rstSent, String rstRecv, String qth, String gridSquare, String notes,
               QslStatus qslStatus, LotwStatus lotwStatus, EqslStatus eqslStatus,
               Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.theirCallsign = theirCallsign;
        this.qsoDate = qsoDate;
        this.timeOn = timeOn;
        this.band = band;
        this.frequencyKhz = frequencyKhz;
        this.mode = mode;
        this.submode = submode;
        this.customMode = customMode;
        this.rstSent = rstSent;
        this.rstRecv = rstRecv;
        this.qth = qth;
        this.gridSquare = gridSquare;
        this.notes = notes;
        this.qslStatus = qslStatus;
        this.lotwStatus = lotwStatus;
        this.eqslStatus = eqslStatus;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Factory method for new QSO creation
    public static Qso create(UUID userId, String theirCallsign, LocalDate qsoDate, LocalTime timeOn,
                             String band, BigDecimal frequencyKhz, AdifMode mode, AdifSubmode submode,
                             String customMode, String rstSent, String rstRecv, String qth,
                             String gridSquare, String notes) {
        Qso qso = new Qso();
        qso.id = UUID.randomUUID();
        qso.userId = userId;
        qso.theirCallsign = theirCallsign;
        qso.qsoDate = qsoDate;
        qso.timeOn = timeOn;
        qso.band = band;
        qso.frequencyKhz = frequencyKhz;
        qso.mode = mode;
        qso.submode = submode;
        qso.customMode = customMode;
        qso.rstSent = rstSent;
        qso.rstRecv = rstRecv;
        qso.qth = qth;
        qso.gridSquare = gridSquare;
        qso.notes = notes;
        qso.qslStatus = QslStatus.NONE;
        qso.lotwStatus = LotwStatus.UNKNOWN;
        qso.eqslStatus = EqslStatus.UNKNOWN;
        qso.createdAt = Instant.now();
        qso.updatedAt = Instant.now();
        return qso;
    }

    // Getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getTheirCallsign() {
        return theirCallsign;
    }

    public void setTheirCallsign(String theirCallsign) {
        this.theirCallsign = theirCallsign;
    }

    public LocalDate getQsoDate() {
        return qsoDate;
    }

    public void setQsoDate(LocalDate qsoDate) {
        this.qsoDate = qsoDate;
    }

    public LocalTime getTimeOn() {
        return timeOn;
    }

    public void setTimeOn(LocalTime timeOn) {
        this.timeOn = timeOn;
    }

    public String getBand() {
        return band;
    }

    public void setBand(String band) {
        this.band = band;
    }

    public BigDecimal getFrequencyKhz() {
        return frequencyKhz;
    }

    public void setFrequencyKhz(BigDecimal frequencyKhz) {
        this.frequencyKhz = frequencyKhz;
    }

    public AdifMode getMode() {
        return mode;
    }

    public void setMode(AdifMode mode) {
        this.mode = mode;
    }

    public AdifSubmode getSubmode() {
        return submode;
    }

    public void setSubmode(AdifSubmode submode) {
        this.submode = submode;
    }

    public String getCustomMode() {
        return customMode;
    }

    public void setCustomMode(String customMode) {
        this.customMode = customMode;
    }

    public String getRstSent() {
        return rstSent;
    }

    public void setRstSent(String rstSent) {
        this.rstSent = rstSent;
    }

    public String getRstRecv() {
        return rstRecv;
    }

    public void setRstRecv(String rstRecv) {
        this.rstRecv = rstRecv;
    }

    public String getQth() {
        return qth;
    }

    public void setQth(String qth) {
        this.qth = qth;
    }

    public String getGridSquare() {
        return gridSquare;
    }

    public void setGridSquare(String gridSquare) {
        this.gridSquare = gridSquare;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public QslStatus getQslStatus() {
        return qslStatus;
    }

    public void setQslStatus(QslStatus qslStatus) {
        this.qslStatus = qslStatus;
    }

    public LotwStatus getLotwStatus() {
        return lotwStatus;
    }

    public void setLotwStatus(LotwStatus lotwStatus) {
        this.lotwStatus = lotwStatus;
    }

    public EqslStatus getEqslStatus() {
        return eqslStatus;
    }

    public void setEqslStatus(EqslStatus eqslStatus) {
        this.eqslStatus = eqslStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
