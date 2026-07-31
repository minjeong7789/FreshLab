package com.freshlab.freshdoctor.service;

import com.freshlab.freshdoctor.domain.Alert;
import com.freshlab.freshdoctor.domain.AlertType;
import com.freshlab.freshdoctor.domain.Item;
import com.freshlab.freshdoctor.domain.RiskScore;
import com.freshlab.freshdoctor.domain.User;
import com.freshlab.freshdoctor.domain.UserItem;
import com.freshlab.freshdoctor.repository.AlertRepository;
import com.freshlab.freshdoctor.repository.UserItemRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RiskAlertServiceTest {

    private final AlertRepository alertRepository = mock(AlertRepository.class);
    private final UserItemRepository userItemRepository = mock(UserItemRepository.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final RiskAlertService riskAlertService =
            new RiskAlertService(alertRepository, userItemRepository, eventPublisher);

    @Test
    void createsGradeEntryThresholdAndIssueAlertsForInterestedUsers() {
        UserItem target = target();
        RiskSnapshot previous = snapshot(20, "SAFE", "8.00", "9.00", 0, "NONE", 0, "NONE");
        RiskScore current = current(65, "ALERT", "13.00", "12.00", 20, "HEAVY_RAIN", 10,
                "TYPHOON_OR_LARGE_DAMAGE");
        when(userItemRepository.findByItemItemCode("1001"))
                .thenReturn(List.of(target));

        int count = riskAlertService.createAlerts(previous, current);

        ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository, org.mockito.Mockito.times(6)).save(captor.capture());
        assertThat(count).isEqualTo(6);
        assertThat(captor.getAllValues()).extracting(Alert::getAlertType).containsExactlyInAnyOrder(
                AlertType.GRADE_INCREASE,
                AlertType.RISK_LEVEL_ENTRY,
                AlertType.PRICE_VOLATILITY_THRESHOLD,
                AlertType.PRICE_INCREASE_THRESHOLD,
                AlertType.SEVERE_WEATHER_ISSUE,
                AlertType.SEVERE_NEWS_ISSUE
        );
        assertThat(captor.getAllValues()).allSatisfy(alert -> {
            assertThat(alert.getUser().getUserId()).isEqualTo(7L);
            assertThat(alert.getItemCode()).isEqualTo("1001");
            assertThat(alert.getPreviousScore()).isEqualTo(20);
            assertThat(alert.getCurrentScore()).isEqualTo(65);
            assertThat(alert.getRiskScoreDate()).isEqualTo(LocalDate.of(2026, 7, 27));
            assertThat(alert.getIsRead()).isFalse();
            assertThat(alert.getTitle()).containsPattern("[가-힣]");
            assertThat(alert.getDescription()).containsPattern("[가-힣]");
            assertThat(alert.getEvidence()).isNotBlank();
        });
    }

    @Test
    void createsDecreaseAlertWithoutLevelEntryWhenReturningToSafe() {
        when(userItemRepository.findByItemItemCode("1001"))
                .thenReturn(List.of(target()));
        RiskSnapshot previous = snapshot(45, "CAUTION", "5.00", "5.00", 0, "NONE", 0, "NONE");
        RiskScore current = current(15, "SAFE", "5.00", "5.00", 0, "NONE", 0, "NONE");

        int count = riskAlertService.createAlerts(previous, current);

        ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository).save(captor.capture());
        assertThat(count).isEqualTo(1);
        assertThat(captor.getValue().getAlertType()).isEqualTo(AlertType.GRADE_DECREASE);
    }

    @Test
    void skipsDuplicateAlertForSameUserItemTypeAndDate() {
        when(userItemRepository.findByItemItemCode("1001"))
                .thenReturn(List.of(target()));
        when(alertRepository.existsByUserUserIdAndItemCodeAndAlertTypeAndRiskScoreDate(
                7L, "1001", AlertType.GRADE_INCREASE, LocalDate.of(2026, 7, 27)))
                .thenReturn(true);
        RiskSnapshot previous = snapshot(10, "SAFE", "15.00", "15.00", 20, "HEAVY_RAIN", 10,
                "TYPHOON_OR_LARGE_DAMAGE");
        RiskScore current = current(30, "INTEREST", "15.00", "15.00", 20, "HEAVY_RAIN", 10,
                "TYPHOON_OR_LARGE_DAMAGE");

        int count = riskAlertService.createAlerts(previous, current);

        ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository).save(captor.capture());
        assertThat(count).isEqualTo(1);
        assertThat(captor.getValue().getAlertType()).isEqualTo(AlertType.RISK_LEVEL_ENTRY);
    }

    @Test
    void doesNothingWhenNoUserWatchesItem() {
        when(userItemRepository.findByItemItemCode("1001"))
                .thenReturn(List.of());

        int count = riskAlertService.createAlerts(null,
                current(30, "INTEREST", "20.00", "20.00", 20, "HEAVY_RAIN", 10,
                        "TYPHOON_OR_LARGE_DAMAGE"));

        assertThat(count).isZero();
        verify(alertRepository, never()).save(any());
    }

    private UserItem target() {
        User user = new User();
        user.setUserId(7L);
        Item item = new Item();
        item.setItemCode("1001");
        item.setItemName("배추");
        item.setItemType(Item.ItemType.DOMESTIC);
        UserItem target = new UserItem();
        target.setUser(user);
        target.setItem(item);
        target.setNotificationEnabled(false);
        target.setPriceVolatilityThreshold(new BigDecimal("10.00"));
        target.setPriceIncreaseThreshold(new BigDecimal("10.00"));
        return target;
    }

    private RiskSnapshot snapshot(int score, String grade, String increase, String volatility,
                                  int weatherScore, String weatherType, int newsScore, String newsType) {
        return new RiskSnapshot(score, grade, new BigDecimal(increase), new BigDecimal(volatility),
                weatherType, weatherScore, "weather", newsType, newsScore, "news");
    }

    private RiskScore current(int score, String grade, String increase, String volatility,
                              int weatherScore, String weatherType, int newsScore, String newsType) {
        RiskScore current = new RiskScore();
        current.setItemCode("1001");
        current.setScoreDate(LocalDate.of(2026, 7, 27));
        current.setFinalScore(score);
        current.setRiskGrade(grade);
        current.setPriceIncreaseRate(new BigDecimal(increase));
        current.setPriceVolatilityRate(new BigDecimal(volatility));
        current.setWeatherScore(weatherScore);
        current.setWeatherRiskType(weatherType);
        current.setWeatherReason("집중호우로 인한 출하 피해 위험");
        current.setNewsScore(newsScore);
        current.setNewsRiskType(newsType);
        current.setNewsReason("태풍으로 인한 생산지 피해");
        return current;
    }
}
