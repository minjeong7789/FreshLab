package com.freshlab.freshdoctor.service;

import com.freshlab.freshdoctor.domain.*;
import com.freshlab.freshdoctor.repository.AlertRepository;
import com.freshlab.freshdoctor.repository.RiskScoreRepository;
import com.freshlab.freshdoctor.repository.UserItemRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DailySummaryAlertServiceTest {
    private final UserItemRepository userItemRepository = mock(UserItemRepository.class);
    private final RiskScoreRepository riskScoreRepository = mock(RiskScoreRepository.class);
    private final AlertRepository alertRepository = mock(AlertRepository.class);
    private final DailySummaryAlertService service =
            new DailySummaryAlertService(userItemRepository, riskScoreRepository, alertRepository);

    @Test
    void createsOneSummaryPerUserFromEnabledWatchItems() {
        LocalDate date = LocalDate.of(2026, 7, 28);
        User user = new User();
        user.setUserId(1L);
        Item item = new Item();
        item.setItemCode("1001");
        item.setItemName("Cabbage");
        UserItem userItem = new UserItem();
        userItem.setUser(user);
        userItem.setItem(item);
        userItem.setNotificationEnabled(true);
        RiskScore score = new RiskScore();
        score.setItemCode("1001");
        score.setFinalScore(88);
        score.setRiskGrade("CRITICAL");
        when(userItemRepository.findByNotificationEnabledTrue()).thenReturn(List.of(userItem));
        when(riskScoreRepository.findTopByItemCodeAndScoreDateLessThanEqualOrderByScoreDateDescIdDesc("1001", date))
                .thenReturn(Optional.of(score));

        assertThat(service.create(date)).isEqualTo(1);

        ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository).save(captor.capture());
        assertThat(captor.getValue().getAlertType()).isEqualTo(AlertType.DAILY_SUMMARY);
        assertThat(captor.getValue().getCurrentScore()).isEqualTo(88);
        assertThat(captor.getValue().getEvidence()).contains("1001=CRITICAL(88)");
    }
}
