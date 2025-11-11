package com.ethereal.auth.facade;

import com.ethereal.auth.entity.*;
import com.ethereal.auth.exceptions.UserDoesntExistException;
import com.ethereal.auth.exceptions.UserExistingWithMail;
import com.ethereal.auth.exceptions.UserExistingWithName;
import com.ethereal.auth.services.TwoFaMediator;
import com.ethereal.auth.services.UserService;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;
    private final TwoFaMediator twoFaMediator;

    @RequestMapping(path = "/register", method = RequestMethod.POST)
    public ResponseEntity<AuthResponse> addNewUser(@Valid @RequestBody UserRegisterDTO user) {
        try {
            log.info("--START REGISTER USER");
            userService.register(user);
            log.info("--STOP REGISTER USER");
            return ResponseEntity.ok(new AuthResponse(Code.SUCCESS));
        } catch (UserExistingWithName e) {
            log.info("User doesn't exist in database");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new AuthResponse(Code.A4));
        } catch (UserExistingWithMail existing) {
            log.info("User doesn't exist in database with this mail");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new AuthResponse(Code.A5));
        }
    }

    @RequestMapping(path = "/login", method = RequestMethod.POST)
    public ResponseEntity<?> login(@RequestBody User user, HttpServletResponse response) {
        log.info("--TRY LOGIN USER");
        return userService.login(response, user);
    }

    @RequestMapping(path = "/auto-login", method = RequestMethod.GET)
    public ResponseEntity<?> autoLogin(HttpServletResponse response, HttpServletRequest request) {
        log.info("--TRY AUTO-LOGIN USER");
        return userService.loginByToken(request, response);
    }

    @RequestMapping(path = "/logged-in", method = RequestMethod.GET)
    public ResponseEntity<?> loggedIn(HttpServletResponse response, HttpServletRequest request) {
        log.info("--CHECK USER LOGGED-IN");
        return userService.loggedIn(request, response);
    }

    @RequestMapping(path = "/logout", method = RequestMethod.GET)
    public ResponseEntity<?> logout(HttpServletResponse response, HttpServletRequest request) {
        log.info("--TRY LOGOUT USER");
        return userService.logout(request, response);
    }

    @RequestMapping(path = "/validate", method = RequestMethod.GET)
    public ResponseEntity<AuthResponse> validateToken(HttpServletRequest request, HttpServletResponse response) {
        try {
            log.info("--START validateToken");
            userService.validateToken(request, response);
            log.info("--STOP validateToken");
            return ResponseEntity.ok(new AuthResponse(Code.PERMIT));
        } catch (IllegalArgumentException | ExpiredJwtException e) {
            log.info("Token is not correct");
            return ResponseEntity.status(401).body(new AuthResponse(Code.A3));
        }
    }

    @RequestMapping(path = "/authorize", method = RequestMethod.GET)
    public ResponseEntity<AuthResponse> authorize(HttpServletRequest request, HttpServletResponse response) {
        try {
            log.info("--START authorize");
            userService.validateToken(request, response);
            userService.authorize(request);
            log.info("--STOP authorize");
            return ResponseEntity.ok(new AuthResponse(Code.PERMIT));
        } catch (IllegalArgumentException | ExpiredJwtException e) {
            log.info("Token is not correct");
            return ResponseEntity.status(401).body(new AuthResponse(Code.A3));
        } catch (UserDoesntExistException e1) {
            log.info("User doesn't exist");
            return ResponseEntity.status(401).body(new AuthResponse(Code.A1));
        }
    }

    @RequestMapping(path = "/activate", method = RequestMethod.GET)
    public ResponseEntity<AuthResponse> activateUser(@RequestParam String uid) {
        try {
            log.info("--START activateUser");
            userService.activateUser(uid);
            log.info("--STOP activateUser");
            return ResponseEntity.ok(new AuthResponse(Code.SUCCESS));
        } catch (UserDoesntExistException e) {
            log.info("User doesn't exist in database");
            return ResponseEntity.status(400).body(new AuthResponse(Code.A6));
        }
    }

    @RequestMapping(path = "/deactivate", method = RequestMethod.PATCH)
    public ResponseEntity<AuthResponse> deactivateUser(HttpServletRequest request) {
        try {
            log.info("--START deactivateUser");
            userService.deactivateUser(request);
            log.info("--STOP deactivateUser");
            log.info("Finally, the harvest can commence...");
            return ResponseEntity.ok(new AuthResponse(Code.SUCCESS));
        } catch (UserDoesntExistException e) {
            log.info("User doesn't exist in database");
            return ResponseEntity.status(400).body(new AuthResponse(Code.A6));
        }
    }

    @RequestMapping(path = "/reset-password", method = RequestMethod.POST)
    public ResponseEntity<AuthResponse> sendMailRecovery(@RequestBody ResetPasswordData resetPasswordData) {
        try {
            log.info("--START sendMailRecovery");
            userService.recoverPassword(resetPasswordData.getEmail());
            log.info("--STOP sendMailRecovery");
            return ResponseEntity.ok(new AuthResponse(Code.SUCCESS));
        } catch (UserDoesntExistException e) {
            log.info("User doesn't exist in database");
            return ResponseEntity.status(400).body(new AuthResponse(Code.A6));
        }
    }

    @RequestMapping(path = "/reset-password", method = RequestMethod.PATCH)
    public ResponseEntity<AuthResponse> recoveryMail(@RequestBody ChangePasswordData changePasswordData) {
        try {
            log.info("--START recoveryMail");
            userService.resetPassword(changePasswordData);
            log.info("--STOP recoveryMail");
            return ResponseEntity.ok(new AuthResponse(Code.SUCCESS));
        } catch (UserDoesntExistException e) {
            log.info("User doesn't exist in database");
            return ResponseEntity.status(400).body(new AuthResponse(Code.A6));
        }
    }

    @RequestMapping(path = "/2fa/verify", method = RequestMethod.POST)
    public ResponseEntity<?> verify2fa(@RequestParam("challengeId") String challengeId, @RequestParam("code") String code, HttpServletResponse response) {
        log.info("--START 2FA VERIFY");
        try {
            var dto = twoFaMediator.verifyAndIssueTokens(challengeId, code, response);
            log.info("--STOP 2FA VERIFY");
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            log.info("2FA invalid code/challenge");
            return ResponseEntity.status(401).body(new AuthResponse(Code.A3));
        } catch (IllegalStateException e) {
            log.info("2FA expired/too many attempts");
            return ResponseEntity.status(410).body(new AuthResponse(Code.A3));
        }
    }

    @RequestMapping(path = "/settings/2fa", method = RequestMethod.PATCH)
    public ResponseEntity<AuthResponse> toggle2fa(HttpServletRequest request, @RequestParam("enabled") boolean enabled) {
        log.info("--START 2FA TOGGLE: {}", enabled);
        try {
            twoFaMediator.toggleTwoFactor(request, enabled);
            log.info("--STOP 2FA TOGGLE");
            return ResponseEntity.ok(new AuthResponse(Code.SUCCESS));
        } catch (UserDoesntExistException e) {
            return ResponseEntity.status(401).body(new AuthResponse(Code.A1));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(new AuthResponse(Code.A3));
        }
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ValidationMessage handleValidationExceptions(MethodArgumentNotValidException ex) {
        return new ValidationMessage(ex.getBindingResult().getAllErrors().get(0).getDefaultMessage());
    }
}