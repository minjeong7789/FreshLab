package com.freshlab.freshdoctor.controller;

import com.freshlab.freshdoctor.dto.CurrentUserResponse;
import com.freshlab.freshdoctor.security.CurrentUserId;
import com.freshlab.freshdoctor.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public CurrentUserResponse getCurrentUser(@CurrentUserId Long userId) {
        return userService.getCurrentUser(userId);
    }
}
