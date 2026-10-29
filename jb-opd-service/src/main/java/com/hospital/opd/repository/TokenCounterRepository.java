package com.hospital.opd.repository;

import com.hospital.opd.model.TokenCounter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;

public interface TokenCounterRepository extends JpaRepository<TokenCounter, LocalDate> {

    /**
     * Thread-Safe Atomic Upsert Token Generator Routine.
     * Increments the token counter for a specific date, inserting a new row starting at 1 if the day just changed.
     */
    @Modifying
    @Query(value = "INSERT INTO daily_token_counters (counter_date, last_value) VALUES (:date, 1) " +
                   "ON CONFLICT (counter_date) DO UPDATE SET last_value = daily_token_counters.last_value + 1", 
           nativeQuery = true)
    void incrementAtomicToken(@Param("date") LocalDate date);

    @Query("SELECT t.lastValue FROM TokenCounter t WHERE t.counterDate = :date")
    Integer getCurrentTokenValue(@Param("date") LocalDate date);
}
