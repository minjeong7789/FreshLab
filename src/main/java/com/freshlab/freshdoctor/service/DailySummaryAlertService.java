package com.freshlab.freshdoctor.service;

import com.freshlab.freshdoctor.domain.*;
import com.freshlab.freshdoctor.repository.AlertRepository;
import com.freshlab.freshdoctor.repository.RiskScoreRepository;
import com.freshlab.freshdoctor.repository.UserItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DailySummaryAlertService {
    private static final String SUMMARY_ITEM_CODE = "DAILY_SUMMARY";

    private final UserItemRepository userItemRepository;
    private final RiskScoreRepository riskScoreRepository;
    private final AlertRepository alertRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public int create(LocalDate date) {
        Map<Long, List<UserItem>> itemsByUser =
                userItemRepository.findAllByOrderByUserUserIdAscItemItemCodeAsc().stream()
                .collect(Collectors.groupingBy(userItem -> userItem.getUser().getUserId()));
        int created = 0;
        for (Map.Entry<Long, List<UserItem>> entry : itemsByUser.entrySet()) {
            User user = entry.getValue().get(0).getUser();
            if (alertRepository.existsByUserUserIdAndItemCodeAndAlertTypeAndRiskScoreDate(
                    user.getUserId(), SUMMARY_ITEM_CODE, AlertType.DAILY_SUMMARY, date)) {
                continue;
            }
            List<RiskScore> scores = entry.getValue().stream()
                    .map(userItem -> riskScoreRepository
                            .findTopByItemCodeAndScoreDateLessThanEqualOrderByScoreDateDescIdDesc(
                                    userItem.getItem().getItemCode(), date)
                            .orElse(null))
                    .filter(java.util.Objects::nonNull)
                    .toList();
            if (scores.isEmpty()) {
                continue;
            }
            RiskScore highest = scores.stream()
                    .max(Comparator.comparingInt(score -> score.getFinalScore() == null ? 0 : score.getFinalScore()))
                    .orElseThrow();
            Alert alert = new Alert();
            alert.setUser(user);
            alert.setItemCode(SUMMARY_ITEM_CODE);
            alert.setItemName("일일 요약");
            alert.setAlertType(AlertType.DAILY_SUMMARY);
            alert.setCurrentScore(highest.getFinalScore());
            alert.setCurrentGrade(highest.getRiskGrade());
            alert.setTitle("오늘의 관심 품목 위험 요약");
            alert.setDescription("관심 품목 " + scores.size() + "개의 위험 현황을 확인해 보세요.");
            alert.setEvidence(scores.stream()
                    .map(score -> score.getItemCode() + "=" + score.getRiskGrade() + "(" + score.getFinalScore() + ")")
                    .collect(Collectors.joining(", ")));
            alert.setRiskScoreDate(date);
            alert.setOccurredAt(LocalDateTime.now());
            alert.setIsRead(false);
            Alert savedAlert = alertRepository.save(alert);
            if (savedAlert != null && savedAlert.getId() != null) {
                eventPublisher.publishEvent(new com.freshlab.freshdoctor.event.AlertCreatedEvent(savedAlert.getId()));
            }
            created++;
        }
        return created;
    }
}
