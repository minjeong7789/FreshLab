package com.freshlab.freshdoctor.service;

import com.freshlab.freshdoctor.dto.CurrentUserResponse;
import com.freshlab.freshdoctor.repository.UserRepository;
import com.freshlab.freshdoctor.security.InvalidTokenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public CurrentUserResponse getCurrentUser(Long userId) {
        return userRepository.findById(userId)
                .map(CurrentUserResponse::from)
                .orElseThrow(InvalidTokenException::new);
    }
}
