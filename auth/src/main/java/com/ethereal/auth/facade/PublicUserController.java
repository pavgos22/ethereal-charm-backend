package com.ethereal.auth.facade;

import com.ethereal.auth.dto.CheckEmailRequest;
import com.ethereal.auth.dto.CheckEmailResponse;
import com.ethereal.auth.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class PublicUserController {

    private final UserService userService;

    @PostMapping("/check-email")
    public CheckEmailResponse checkEmail(@RequestBody CheckEmailRequest request) {
        return userService.checkEmailExistsAndEnabled(request.getEmail());
    }
}