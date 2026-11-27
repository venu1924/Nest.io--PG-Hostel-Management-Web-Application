package com.happynest.pg.service;

import com.happynest.pg.domain.entity.PgOwner;
import com.happynest.pg.dto.AdminDto;
import com.happynest.pg.dto.LoginRequest;
import com.happynest.pg.dto.LoginResponse;
import com.happynest.pg.dto.SetupRequest;
import com.happynest.pg.exception.ApiException;
import com.happynest.pg.repository.PgOwnerRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
public class AuthService {

    private static final long SESSION_DAYS = 7;

    private final PgOwnerRepository ownerRepository;
    private final SetupService setupService;
    private final BCryptPasswordEncoder passwords = new BCryptPasswordEncoder();
    private final String tokenSecret;

    public AuthService(
            PgOwnerRepository ownerRepository,
            SetupService setupService,
            @Value("${app.auth.token-secret}") String tokenSecret) {
        this.ownerRepository = ownerRepository;
        this.setupService = setupService;
        this.tokenSecret = tokenSecret;
    }

    @Transactional
    public LoginResponse register(SetupRequest request) {
        String username = request.getUsername().trim();
        if (ownerRepository.existsByUsernameIgnoreCase(username)) {
            throw new ApiException("That username is already used. Choose another.", HttpStatus.CONFLICT);
        }
        PgOwner owner = new PgOwner();
        owner.setId(UUID.randomUUID().toString());
        List<Integer> roomsOnFloors = request.getRoomsOnFloors();
        int widest = roomsOnFloors.stream().filter(java.util.Objects::nonNull).mapToInt(Integer::intValue).max().orElse(1);
        owner.setPgName(request.getPgName().trim());
        owner.setUsername(username);
        owner.setPassword(passwords.encode(request.getPassword()));
        owner.setFloors(roomsOnFloors.size());
        owner.setRoomsPerFloor(widest);
        owner.setFloorRoomCounts(roomsOnFloors.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse(""));
        ownerRepository.save(owner);
        setupService.createBuilding(owner, roomsOnFloors);
        return session(owner);
    }

    public LoginResponse login(LoginRequest request) {
        String username = request.getUsername() != null ? request.getUsername().trim() : "";
        PgOwner owner = ownerRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ApiException("Invalid username or password.", HttpStatus.UNAUTHORIZED));
        String password = request.getPassword() != null ? request.getPassword() : "";
        if (!passwords.matches(password, owner.getPassword())) {
            throw new ApiException("Invalid username or password.", HttpStatus.UNAUTHORIZED);
        }
        return session(owner);
    }

    public String ownerIdFrom(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authorizationHeader.substring(7).trim();
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return null;
        }
        try {
            String ownerId = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            String expiry = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            String expected = sign(ownerId + "." + expiry);
            if (!expected.equals(parts[2])) {
                return null;
            }
            if (Instant.now().toEpochMilli() > Long.parseLong(expiry)) {
                return null;
            }
            return ownerRepository.existsById(ownerId) ? ownerId : null;
        } catch (RuntimeException ex) {
            return null;
        }
    }

    public AdminDto currentAdmin() {
        return ownerRepository.findById(com.happynest.pg.security.OwnerContext.require())
                .map(this::admin)
                .orElse(null);
    }

    private LoginResponse session(PgOwner owner) {
        long expiry = Instant.now().plus(SESSION_DAYS, ChronoUnit.DAYS).toEpochMilli();
        String payload = owner.getId() + "." + expiry;
        String token = b64(owner.getId()) + "." + b64(String.valueOf(expiry)) + "." + sign(payload);
        return LoginResponse.builder().token(token).admin(admin(owner)).build();
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(tokenSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Could not sign the login token.", ex);
        }
    }

    private static String b64(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private AdminDto admin(PgOwner owner) {
        return AdminDto.builder()
                .id(owner.getId())
                .name(owner.getPgName())
                .role("Owner")
                .build();
    }
}
