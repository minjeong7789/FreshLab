package com.freshlab.freshdoctor.repository;

import com.freshlab.freshdoctor.domain.FcmRegistration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FcmRegistrationRepository extends JpaRepository<FcmRegistration, Long> {
    Optional<FcmRegistration> findByRegistrationKey(String registrationKey);
    Optional<FcmRegistration> findByIdAndUserUserId(Long id, Long userId);
    List<FcmRegistration> findByUserUserIdAndActiveTrueOrderByIdAsc(Long userId);
}
