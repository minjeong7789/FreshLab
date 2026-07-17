package com.freshlab.freshdoctor.service;

import com.freshlab.freshdoctor.domain.Item;
import com.freshlab.freshdoctor.domain.RiskScore;
import com.freshlab.freshdoctor.dto.RiskScoreResponse;
import com.freshlab.freshdoctor.dto.RiskScoreUpsertRequest;
import com.freshlab.freshdoctor.repository.RiskScoreRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RiskScoreServiceTest {

    private final RiskScoreRepository riskScoreRepository = mock(RiskScoreRepository.class);
    private final ItemService itemService = mock(ItemService.class);
    private final RiskAlertService riskAlertService = mock(RiskAlertService.class);
    private final RiskScoreService riskScoreService = new RiskScoreService(
            riskScoreRepository,
            itemService,
            riskAlertService
    );

    @Test
    void createsRiskScoreWhenItemAndDateDoNotExist() {
        RiskScoreUpsertRequest request = request("1001", LocalDate.of(2026, 7, 20), 10);
        when(itemService.getItem("1001")).thenReturn(item());
        when(riskScoreRepository.findByItemCodeAndScoreDate("1001", LocalDate.of(2026, 7, 20)))
                .thenReturn(Optional.empty());
        when(riskScoreRepository.save(any(RiskScore.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RiskScoreResponse response = riskScoreService.upsert(request);

        assertThat(response.itemCode()).isEqualTo("1001");
        assertThat(response.scoreDate()).isEqualTo(LocalDate.of(2026, 7, 20));
        assertThat(response.rawScore()).isEqualTo(60);
        assertThat(response.finalScore()).isEqualTo(71);
        assertThat(response.riskGrade()).isEqualTo("ALERT");
        verify(riskAlertService).createAlerts(isNull(), any(RiskScore.class));
    }

    @Test
    void updatesExistingRiskScoreWhenItemAndDateAlreadyExist() {
        LocalDate scoreDate = LocalDate.of(2026, 7, 20);
        RiskScore existing = new RiskScore();
        existing.setId(1L);
        existing.setItemCode("1001");
        existing.setScoreDate(scoreDate);
        existing.setTotalScore(10);

        RiskScoreUpsertRequest request = request("1001", scoreDate, 20);
        when(itemService.getItem("1001")).thenReturn(item());
        when(riskScoreRepository.findByItemCodeAndScoreDate("1001", scoreDate))
                .thenReturn(Optional.of(existing));
        when(riskScoreRepository.save(same(existing))).thenReturn(existing);

        RiskScoreResponse response = riskScoreService.upsert(request);

        verify(riskScoreRepository).save(same(existing));
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.priceIncreaseScore()).isEqualTo(20);
        assertThat(response.finalScore()).isEqualTo(82);
        assertThat(response.riskGrade()).isEqualTo("CRITICAL");
    }

    @Test
    void capsFinalScoreAtOneHundred() {
        RiskScoreUpsertRequest request = new RiskScoreUpsertRequest(
                "1001",
                LocalDate.of(2026, 7, 20),
                new BigDecimal("30.00"),
                40,
                new BigDecimal("25.00"),
                30,
                new BigDecimal("20.00"),
                30,
                "HEAVY_RAIN",
                30,
                "호우 위험",
                LocalDate.of(2026, 7, 20),
                "1400",
                "TYPHOON_OR_LARGE_DAMAGE",
                30,
                "태풍 위험",
                10L,
                null,
                null
        );
        when(itemService.getItem("1001")).thenReturn(item());
        when(riskScoreRepository.findByItemCodeAndScoreDate("1001", LocalDate.of(2026, 7, 20)))
                .thenReturn(Optional.empty());
        when(riskScoreRepository.save(any(RiskScore.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RiskScoreResponse response = riskScoreService.upsert(request);

        assertThat(response.rawScore()).isEqualTo(160);
        assertThat(response.finalScore()).isEqualTo(100);
        assertThat(response.riskGrade()).isEqualTo("CRITICAL");
    }

    @Test
    void rejectsInvalidRequest() {
        assertThatThrownBy(() -> riskScoreService.upsert(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("request must not be null.");

        assertThatThrownBy(() -> riskScoreService.upsert(request("", LocalDate.of(2026, 7, 20), 10)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("itemCode must not be blank.");

        assertThatThrownBy(() -> riskScoreService.upsert(request("1001", null, 10)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("scoreDate must not be null.");
    }

    private RiskScoreUpsertRequest request(String itemCode, LocalDate scoreDate, Integer priceIncreaseScore) {
        return new RiskScoreUpsertRequest(
                itemCode,
                scoreDate,
                new BigDecimal("12.50"),
                priceIncreaseScore,
                new BigDecimal("15.00"),
                15,
                new BigDecimal("8.00"),
                10,
                "HEAT_WAVE",
                10,
                "폭염 위험",
                scoreDate,
                "1400",
                "SHIPMENT_DECREASE",
                15,
                "출하량 감소",
                5L,
                "normalYear",
                "평년 가격 없음"
        );
    }

    private Item item() {
        Item item = new Item();
        item.setItemCode("1001");
        item.setItemName("배추");
        item.setItemType(Item.ItemType.DOMESTIC);
        return item;
    }
}
