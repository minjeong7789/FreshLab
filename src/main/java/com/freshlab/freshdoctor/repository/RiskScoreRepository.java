package com.freshlab.freshdoctor.repository;

import com.freshlab.freshdoctor.domain.RiskScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RiskScoreRepository extends JpaRepository<RiskScore, Long> {

    Optional<RiskScore> findByItemCodeAndScoreDate(String itemCode, LocalDate scoreDate);

    Optional<RiskScore> findTopByItemCodeOrderByScoreDateDescIdDesc(String itemCode);

    Optional<RiskScore> findTopByItemCodeAndScoreDateLessThanOrderByScoreDateDescIdDesc(
            String itemCode,
            LocalDate scoreDate
    );

    Optional<RiskScore> findTopByItemCodeAndScoreDateLessThanEqualOrderByScoreDateDescIdDesc(
            String itemCode,
            LocalDate scoreDate
    );

    List<RiskScore> findByItemCodeAndScoreDateBetweenOrderByScoreDateAsc(
            String itemCode,
            LocalDate startDate,
            LocalDate endDate
    );
}
