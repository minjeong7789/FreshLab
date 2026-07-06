package com.freshlab.freshdoctor.repository;

import com.freshlab.freshdoctor.domain.Alert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findTop20ByUserUserIdOrderByCreatedAtDesc(Long userId);

    List<Alert> findByUserUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);
}
