package com.freshlab.freshdoctor.service;

import com.freshlab.freshdoctor.domain.*;
import com.freshlab.freshdoctor.repository.AlertRepository;
import com.freshlab.freshdoctor.repository.FcmDeliveryLogRepository;
import com.freshlab.freshdoctor.repository.FcmRegistrationRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FcmPushServiceTest {

    private final AlertRepository alertRepository = mock(AlertRepository.class);
    private final FcmRegistrationRepository registrationRepository = mock(FcmRegistrationRepository.class);
    private final FcmDeliveryLogRepository deliveryLogRepository = mock(FcmDeliveryLogRepository.class);
    private final PushGateway pushGateway = mock(PushGateway.class);
    private final FcmPushService service = new FcmPushService(
            alertRepository, registrationRepository, deliveryLogRepository, pushGateway);

    @Test
    void recordsSuccessForEveryRegisteredDevice() {
        prepare(List.of(registration(1L, "a"), registration(2L, "b")));
        when(pushGateway.send(anyString(), any())).thenReturn(PushSendResult.success("message-id"));

        service.sendAlert(100L);

        verify(pushGateway, times(2)).send(anyString(), any());
        ArgumentCaptor<FcmDeliveryLog> logs = ArgumentCaptor.forClass(FcmDeliveryLog.class);
        verify(deliveryLogRepository, times(2)).save(logs.capture());
        assertThat(logs.getAllValues()).allMatch(log -> log.getStatus() == FcmDeliveryStatus.SUCCESS);
    }

    @Test
    void keepsSuccessfulDeviceAndDeactivatesOnlyInvalidDeviceOnPartialFailure() {
        FcmRegistration valid = registration(1L, "valid");
        FcmRegistration invalid = registration(2L, "invalid");
        prepare(List.of(valid, invalid));
        when(pushGateway.send(eq("valid"), any())).thenReturn(PushSendResult.success("message-id"));
        when(pushGateway.send(eq("invalid"), any()))
                .thenReturn(PushSendResult.failure("UNREGISTERED", true));

        service.sendAlert(100L);

        assertThat(valid.getActive()).isTrue();
        assertThat(invalid.getActive()).isFalse();
        verify(deliveryLogRepository, times(2)).save(any(FcmDeliveryLog.class));
    }

    @Test
    void recordsAllFailuresWithoutThrowingOrRemovingTemporaryFailures() {
        FcmRegistration first = registration(1L, "a");
        FcmRegistration second = registration(2L, "b");
        prepare(List.of(first, second));
        when(pushGateway.send(anyString(), any()))
                .thenReturn(PushSendResult.failure("UNAVAILABLE", false));

        service.sendAlert(100L);

        assertThat(first.getActive()).isTrue();
        assertThat(second.getActive()).isTrue();
        ArgumentCaptor<FcmDeliveryLog> logs = ArgumentCaptor.forClass(FcmDeliveryLog.class);
        verify(deliveryLogRepository, times(2)).save(logs.capture());
        assertThat(logs.getAllValues()).allMatch(log -> log.getStatus() == FcmDeliveryStatus.FAILED);
    }

    private void prepare(List<FcmRegistration> registrations) {
        Alert alert = alert();
        when(alertRepository.findById(100L)).thenReturn(Optional.of(alert));
        when(registrationRepository.findByUserUserIdAndActiveTrueOrderByIdAsc(7L))
                .thenReturn(registrations);
    }

    private Alert alert() {
        User user = new User();
        user.setUserId(7L);
        Alert alert = new Alert();
        alert.setId(100L);
        alert.setUser(user);
        alert.setItemCode("1001");
        alert.setAlertType(AlertType.GRADE_INCREASE);
        alert.setCurrentGrade("CRITICAL");
        alert.setTitle("Risk grade increased");
        alert.setDescription("Cabbage risk increased.");
        return alert;
    }

    private FcmRegistration registration(Long id, String key) {
        FcmRegistration registration = new FcmRegistration();
        registration.setId(id);
        registration.setRegistrationKey(key);
        registration.setActive(true);
        return registration;
    }
}
