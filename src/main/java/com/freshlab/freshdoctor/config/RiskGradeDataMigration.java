package com.freshlab.freshdoctor.config;

import com.freshlab.freshdoctor.domain.RiskScore;
import com.freshlab.freshdoctor.dto.RiskGrade;
import com.freshlab.freshdoctor.repository.RiskScoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RiskGradeDataMigration implements ApplicationRunner {

    private final RiskScoreRepository riskScoreRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<RiskScore> changed = riskScoreRepository.findAll().stream()
                .filter(score -> score.getFinalScore() != null)
                .filter(this::applyMvpGrade)
                .toList();
        if (!changed.isEmpty()) {
            riskScoreRepository.saveAll(changed);
        }
    }

    private boolean applyMvpGrade(RiskScore score) {
        String grade = RiskGrade.fromScore(score.getFinalScore()).name();
        boolean changed = !grade.equals(score.getRiskGrade()) || !grade.equals(score.getGrade());
        if (changed) {
            score.setRiskGrade(grade);
            score.setGrade(grade);
        }
        return changed;
    }
}
