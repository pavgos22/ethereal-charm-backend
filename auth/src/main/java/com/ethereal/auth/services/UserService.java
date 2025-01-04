package com.ethereal.auth.services;

import com.ethereal.auth.entity.*;
import com.ethereal.auth.exceptions.*;
import com.ethereal.auth.repository.ResetOperationsRepository;
import com.ethereal.auth.repository.UserRepository;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final EntityManager entityManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ResetOperationService resetOperationService;
    private final ResetOperationsRepository resetOperationsRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final CookieService cookieService;
    @Value("${jwt.exp}")
    private int exp;
    @Value("${jwt.refresh.exp}")
    private int refreshExp;


    private User saveUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.saveAndFlush(user);
    }

    private String generateToken(String username, int exp) {
        return jwtService.generateToken(username, exp);
    }

    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        log.info("Delete all cookies");
        Cookie cookie = cookieService.removeCookie(request.getCookies(), "Authorization");
        if (cookie != null)
            response.addCookie(cookie);

        cookie = cookieService.removeCookie(request.getCookies(), "refresh");
        if (cookie != null)
            response.addCookie(cookie);

        return ResponseEntity.ok(new AuthResponse(Code.SUCCESS));
    }

    public void validateToken(HttpServletRequest request, HttpServletResponse response) throws ExpiredJwtException, IllegalArgumentException {
        String token = null;
        String refresh = null;
        if (request.getCookies() != null) {
            for (Cookie value : Arrays.stream(request.getCookies()).toList()) {
                if (value.getName().equals("Authorization")) {
                    token = value.getValue();
                } else if (value.getName().equals("refresh")) {
                    refresh = value.getValue();
                }
            }
        } else {
            log.info("Can't login because in token is empty");
            throw new IllegalArgumentException("Token cannot be null");
        }
        try {
            jwtService.validateToken(token);
        } catch (IllegalArgumentException | ExpiredJwtException e) {
            jwtService.validateToken(refresh);
            Cookie refreshCookie = cookieService.generateCookie("refresh", jwtService.refreshToken(refresh, refreshExp), refreshExp);
            Cookie cookie = cookieService.generateCookie("Authorization", jwtService.refreshToken(refresh, exp), exp);
            response.addCookie(cookie);
            response.addCookie(refreshCookie);
        }
    }

    public ResponseEntity<LoginResponse> loggedIn(HttpServletRequest request, HttpServletResponse response) {
        try {
            validateToken(request, response);
            return ResponseEntity.ok(new LoginResponse(true));
        } catch (ExpiredJwtException | IllegalArgumentException e) {
            return ResponseEntity.ok(new LoginResponse(false));
        }
    }

    public ResponseEntity<?> loginByToken(HttpServletRequest request, HttpServletResponse response) {
        try {
            validateToken(request, response);

            String refresh = null;
            for (Cookie value : Arrays.stream(request.getCookies()).toList()) {
                if (value.getName().equals("refresh")) {
                    refresh = value.getValue();
                }
            }

            if (refresh == null || refresh.isEmpty()) {
                log.info("Refresh token is missing or empty");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new AuthResponse(Code.A3));
            }

            String loginOrEmail = jwtService.getSubject(refresh);

            User user = userRepository.findUserByLoginOrEmailAndLockAndEnabled(loginOrEmail).orElse(null);
            if (user != null) {
                return ResponseEntity.ok(
                        UserRegisterDTO
                                .builder()
                                .login(user.getUsername())
                                .email(user.getEmail())
                                .role(user.getRole())
                                .build()
                );
            }

            log.info("User not found for the given token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new AuthResponse(Code.A1));
        } catch (ExpiredJwtException | IllegalArgumentException e) {
            log.info("Token is expired or invalid: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new AuthResponse(Code.A3));
        }
    }


    public void register(UserRegisterDTO userRegisterDTO) throws UserExistingWithName, UserExistingWithMail {
        userRepository.findUserByLogin(userRegisterDTO.getLogin()).ifPresent(value -> {
            log.info("Users alredy exist with this name");
            throw new UserExistingWithName("Users alredy exist with this name");
        });
        userRepository.findUserByEmail(userRegisterDTO.getEmail()).ifPresent(value -> {
            log.info("Users alredy exist with this mail");
            throw new UserExistingWithMail("Users alredy exist with this mail");
        });
        User user = new User();
        user.setLock(true);
        user.setEnabled(false);
        user.setLogin(userRegisterDTO.getLogin());
        user.setPassword(userRegisterDTO.getPassword());
        user.setEmail(userRegisterDTO.getEmail());
        user.setRole(Role.USER);

        saveUser(user);
        emailService.sendActivation(user);
    }

    public ResponseEntity<?> login(HttpServletResponse response, User authRequest) {
        log.info("--START LoginService");

        log.info("Authenticating user: " + authRequest.getUsername());

        User user = userRepository.findUserByLoginOrEmailAndLockAndEnabled(authRequest.getUsername()).orElse(null);
        // log.info("User found: " + user);

        if (user != null) {
            Authentication authenticate = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword())
            );

            log.info("Authentication result: " + authenticate.isAuthenticated());

            if (authenticate.isAuthenticated()) {
                Cookie refresh = cookieService.generateCookie("refresh", generateToken(authRequest.getUsername(), refreshExp), refreshExp);
                Cookie cookie = cookieService.generateCookie("Authorization", generateToken(authRequest.getUsername(), exp), exp);
                response.addCookie(cookie);
                response.addCookie(refresh);

                log.info("--STOP LoginService");
                return ResponseEntity.ok(
                        UserRegisterDTO.builder()
                                .login(user.getUsername())
                                .email(user.getEmail())
                                .role(user.getRole())
                                .build()
                );
            } else {
                log.info("--STOP LoginService: Authentication failed");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new AuthResponse(Code.A1));
            }
        }

        log.info("--STOP LoginService: User doesn't exist");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new AuthResponse(Code.A2));
    }


    public void setAsAdmin(UserRegisterDTO user) {
        userRepository.findUserByLogin(user.getLogin()).ifPresent(value -> {
            value.setRole(Role.ADMIN);
            userRepository.save(value);
        });
    }

    public void activateUser(String uuid) throws UserDoesntExistException {
        User user = userRepository.findUserByUuid(uuid).orElse(null);

        if (user != null) {
            user.setLock(false);
            user.setEnabled(true);
            userRepository.save(user);
            return;
        }
        throw new UserDoesntExistException("User doesn't exist");
    }

    public void deactivateUser(HttpServletRequest request) throws UserDoesntExistException {
        String token = getTokenFromRequest(request);
        String usernameOrEmail = jwtService.getSubject(token);

        User user = userRepository.findUserByLoginOrEmailAndLockAndEnabled(usernameOrEmail)
                .orElseThrow(() -> new UserDoesntExistException("User with username/email " + usernameOrEmail + " does not exist"));

        if (user != null) {
            user.setLock(true);
            user.setEnabled(false);
            userRepository.save(user);
            return;
        }
        throw new UserDoesntExistException("User doesn't exist");
    }

    public void recoverPassword(String email) throws UserDoesntExistException {
        User user = userRepository.findUserByEmail(email).orElse(null);
        if (user != null) {
            ResetOperations resetOperations = resetOperationService.initResetOperation(user);
            emailService.sendPasswordRecovery(user, resetOperations.getUid());
            return;
        }
        throw new UserDoesntExistException("User doesn't exist");
    }

    public void resetPassword(ChangePasswordData changePasswordData) throws UserDoesntExistException {
        ResetOperations resetOperations = resetOperationsRepository.findByUid(changePasswordData.getUid()).orElse(null);
        if (resetOperations != null) {
            User user = userRepository.findUserByUuid(resetOperations.getUser().getUuid()).orElse(null);

            if (user != null) {
                user.setPassword(changePasswordData.getPassword());
                saveUser(user);
                resetOperationService.endOperation(resetOperations.getUid());
                return;
            }
        }
        throw new UserDoesntExistException("User doesn't exist");
    }

    public void authorize(HttpServletRequest request) throws UserDoesntExistException {
        String token = null;
        String refresh = null;

        if (request.getCookies() != null) {
            for (Cookie value : request.getCookies()) {
                if (value.getName().equals("Authorization")) {
                    token = value.getValue();
                } else if (value.getName().equals("refresh")) {
                    refresh = value.getValue();
                }
            }
        } else {
            log.info("Cannot authorize, because no cookies were found");
            throw new IllegalArgumentException("Token cannot be null");
        }

        if (token != null && !token.isEmpty()) {
            String subject = jwtService.getSubject(token);
            userRepository.findUserByLoginOrEmailAndLockAndEnabledAndIsAdmin(subject)
                    .orElseThrow(() -> new UserDoesntExistException("User not found or not authorized"));
        } else if (refresh != null && !refresh.isEmpty()) {
            String subject = jwtService.getSubject(refresh);
            userRepository.findUserByLoginOrEmailAndLockAndEnabledAndIsAdmin(subject)
                    .orElseThrow(() -> new UserDoesntExistException("User not found or not authorized"));
        } else {
            log.info("No valid token provided");
            throw new IllegalArgumentException("Token cannot be null");
        }
    }

    public void addShippingDetails(HttpServletRequest request, ShippingDetailsDTO shippingDetails) {
        String subject = jwtService.getSubject(getTokenFromRequest(request));

        User user = userRepository.findUserByLoginOrEmailAndLockAndEnabled(subject)
                .orElseThrow(() -> new UserDoesntExistException("User with username/email " + subject + " does not exist"));

        if (!user.isEnabled()) {
            throw new UserNotEnabledException("User with UUID " + user.getUuid() + " is not enabled");
        }

        if (user.isLock()) {
            throw new UserLockedException("User with UUID " + user.getUuid() + " is locked");
        }

        log.info("Shipping Details: {}", shippingDetails);

        user.setFirstName(shippingDetails.getFirstName());
        user.setLastName(shippingDetails.getLastName());
        user.setPhone(shippingDetails.getPhone());
        user.setCity(shippingDetails.getCity());
        user.setStreet(shippingDetails.getStreet());
        user.setApartmentNumber(shippingDetails.getApartmentNumber());
        user.setPostalCode(shippingDetails.getPostalCode());
        user.setCompany(shippingDetails.isCompany());
        user.setCompanyName(shippingDetails.getCompanyName());
        user.setNip(shippingDetails.getNip());

        userRepository.save(user);
    }

    public UserDTO getUserProfile(HttpServletRequest request) {
        String usernameOrEmail = jwtService.getSubject(getTokenFromRequest(request));

        User user = userRepository.findUserByLoginOrEmailAndLockAndEnabled(usernameOrEmail)
                .orElseThrow(() -> new UserDoesntExistException("User with username/email " + usernameOrEmail + " does not exist"));

        return new UserDTO(
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhone(),
                user.getCity(),
                user.getStreet(),
                user.getApartmentNumber(),
                user.getPostalCode(),
                user.isCompany(),
                user.getCompanyName(),
                user.getNip()
        );
    }

    public String toggleFavourite(HttpServletRequest request, String productUid) {
        String token = getTokenFromRequest(request);
        String usernameOrEmail = jwtService.getSubject(token);

        User user = userRepository.findUserByLoginOrEmailAndLockAndEnabled(usernameOrEmail)
                .orElseThrow(() -> new UserDoesntExistException("User with username/email " + usernameOrEmail + " does not exist"));

        List<String> favourites = user.getFavouriteProductUids();

        if (favourites.contains(productUid)) {
            favourites.remove(productUid);
            userRepository.save(user);
            return "Product removed from favourites";
        } else {
            favourites.add(productUid);
            userRepository.save(user);
            return "Product added to favourites";
        }
    }

    public List<SimpleProductDTO> getFavouriteProducts(HttpServletRequest request) {
        String token = getTokenFromRequest(request);
        String usernameOrEmail = jwtService.getSubject(token);

        User user = userRepository.findUserByLoginOrEmailAndLockAndEnabled(usernameOrEmail)
                .orElseThrow(() -> new UserDoesntExistException("User with username/email " + usernameOrEmail + " does not exist"));

        List<Object[]> results = entityManager.createNativeQuery("""
                            SELECT p.uid AS uid, p.product_name AS name, p.main_desc AS mainDesc, 
                                   p.price AS price, i.uuid AS imageUuid, 
                                   p.create_at AS createAt, p.discount AS discount, 
                                   p.discounted_price AS discountedPrice
                            FROM products p
                            LEFT JOIN image_data i ON p.image_urls[1] = i.uuid
                            WHERE p.uid = ANY(:productUids)
                        """).setParameter("productUids", user.getFavouriteProductUids().toArray(new String[0]))
                .getResultList();

        String FILE_SERVICE = "http://localhost:8088/api/v1/image?uuid=";

        return results.stream()
                .map(result -> new SimpleProductDTO(
                        (String) result[0],  // uid
                        (String) result[1],  // name
                        (String) result[2],  // mainDesc
                        ((BigDecimal) result[3]).floatValue(),  // price
                        FILE_SERVICE + result[4],  // image_url
                        ((java.sql.Date) result[5]).toLocalDate(), // createAt
                        (Boolean) result[6],  // discount
                        result[7] != null ? ((BigDecimal) result[7]).floatValue() : null // discountedPrice
                ))
                .collect(Collectors.toList());
    }


    private String getTokenFromRequest(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("Authorization")) {
                    return cookie.getValue();
                }
            }
        }
        throw new IllegalArgumentException("Token not found in request");
    }
}
