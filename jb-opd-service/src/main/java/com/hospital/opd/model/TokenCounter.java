package com.hospital.opd.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "daily_token_counters")
public class TokenCounter {

    @Id
    @Column(name = "counter_date")
    private LocalDate counterDate;

    @Column(name = "last_value", nullable = false)
    private Integer lastValue;

    public TokenCounter() {}

    public TokenCounter(LocalDate counterDate, Integer lastValue) {
        this.counterDate = counterDate;
        this.lastValue = lastValue;
    }

    public LocalDate getCounterDate() { return counterDate; }
    public void setCounterDate(LocalDate counterDate) { this.counterDate = counterDate; }
    public Integer getLastValue() { return lastValue; }
    public void setLastValue(Integer lastValue) { this.lastValue = lastValue; }
}
