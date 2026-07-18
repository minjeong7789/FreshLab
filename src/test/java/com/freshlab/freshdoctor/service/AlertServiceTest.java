package com.freshlab.freshdoctor.service;

import com.freshlab.freshdoctor.domain.Alert;
import com.freshlab.freshdoctor.domain.AlertType;
import com.freshlab.freshdoctor.domain.User;
import com.freshlab.freshdoctor.dto.AlertResponse;
import com.freshlab.freshdoctor.exception.AlertNotFoundException;
import com.freshlab.freshdoctor.repository.AlertRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AlertServiceTest {

    private final AlertRepository alertRepository = mock(AlertRepository.class);
    private final AlertService alertService = new AlertService(alertRepository);

    @Test
    void returnsOnlyCurrentUsersAlertsInRepositoryOrder() {
        Alert latest = alert(2L, 1L, false, LocalDateTime.of(2026, 7, 18, 10, 0));
        Alert older = alert(1L, 1L, true, LocalDateTime.of(2026, 7, 18, 9, 0));
        when(alertRepository.findByUserUserIdOrderByCreatedAtDescIdDesc(1L))
                .thenReturn(List.of(latest, older));

        List<AlertResponse> result = alertService.getAlerts(1L);

        assertThat(result).extracting(AlertResponse::alertId).containsExactly(2L, 1L);
        verify(alertRepository).findByUserUserIdOrderByCreatedAtDescIdDesc(1L);
    }

    @Test
    void returnsUnreadCountForCurrentUser() {
        when(alertRepository.countByUserUserIdAndIsReadFalse(1L)).thenReturn(3L);

        assertThat(alertService.getUnreadCount(1L).unreadCount()).isEqualTo(3L);
    }

    @Test
    void marksOwnedAlertAsRead() {
        Alert alert = alert(10L, 1L, false, LocalDateTime.now());
        when(alertRepository.findByIdAndUserUserId(10L, 1L)).thenReturn(Optional.of(alert));

        AlertResponse response = alertService.markAsRead(1L, 10L);

        assertThat(alert.getIsRead()).isTrue();
        assertThat(response.read()).isTrue();
    }

    @Test
    void doesNotExposeAnotherUsersAlert() {
        when(alertRepository.findByIdAndUserUserId(10L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> alertService.markAsRead(1L, 10L))
                .isInstanceOf(AlertNotFoundException.class);
    }

    @Test
    void marksAllCurrentUsersUnreadAlertsAsRead() {
        alertService.markAllAsRead(1L);

        verify(alertRepository).markAllAsReadByUserId(1L);
    }

    private Alert alert(Long id, Long userId, boolean read, LocalDateTime createdAt) {
        User user = new User();
        user.setUserId(userId);
        Alert alert = new Alert();
        alert.setId(id);
        alert.setUser(user);
        alert.setItemCode("1001");
        alert.setItemName("Cabbage");
        alert.setAlertType(AlertType.GRADE_INCREASE);
        alert.setPreviousScore(20);
        alert.setPreviousGrade("SAFE");
        alert.setCurrentScore(40);
        alert.setCurrentGrade("INTEREST");
        alert.setTitle("Risk grade increased");
        alert.setDescription("Description");
        alert.setEvidence("Evidence");
        alert.setRiskScoreDate(LocalDate.of(2026, 7, 18));
        alert.setOccurredAt(createdAt);
        alert.setCreatedAt(createdAt);
        alert.setIsRead(read);
        return alert;
    }
}
