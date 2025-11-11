package com.ethereal.auth.entity;

import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import java.time.Instant; import java.util.UUID;

@Getter @Setter
@Entity @Table(name = "auth_challenges")
public class AuthChallenge {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 32)
    private String purpose;

    @Column(name="code_hash", nullable = false, length = 128)
    private String codeHash;

    @Column(name="expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name="attempts_left", nullable = false)
    private Integer attemptsLeft = 5;

    @Column(name="created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name="consumed_at")
    private Instant consumedAt;
}