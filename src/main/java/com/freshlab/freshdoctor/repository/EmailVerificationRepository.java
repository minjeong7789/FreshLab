package com.freshlab.freshdoctor.repository;

import com.freshlab.freshdoctor.domain.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, String> {
}
