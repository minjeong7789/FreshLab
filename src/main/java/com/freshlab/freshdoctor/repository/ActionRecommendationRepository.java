package com.freshlab.freshdoctor.repository;

import com.freshlab.freshdoctor.domain.ActionRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ActionRecommendationRepository extends JpaRepository<ActionRecommendation, Long> {

    Optional<ActionRecommendation> findByInputHash(String inputHash);

    Optional<ActionRecommendation> findTopByItemCodeOrderByUpdatedAtDescIdDesc(String itemCode);
}
