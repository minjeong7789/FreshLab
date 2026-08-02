package com.freshlab.freshdoctor.repository;

import com.freshlab.freshdoctor.domain.Alert;
import com.freshlab.freshdoctor.domain.AlertType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findTop20ByUserUserIdOrderByCreatedAtDesc(Long userId);

    List<Alert> findByUserUserIdOrderByCreatedAtDescIdDesc(Long userId);

    Optional<Alert> findByIdAndUserUserId(Long alertId, Long userId);

    long countByUserUserIdAndIsReadFalse(Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Alert alert
               set alert.isRead = true
             where alert.user.userId = :userId
               and (alert.isRead = false or alert.isRead is null)
            """)
    int markAllAsReadByUserId(@Param("userId") Long userId);

    List<Alert> findByUserUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);

    boolean existsByUserUserIdAndItemCodeAndAlertTypeAndRiskScoreDate(
            Long userId,
            String itemCode,
            AlertType alertType,
            java.time.LocalDate riskScoreDate
    );
}
