package com.ctbe.eventflow.model;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
@Entity @Table(name="token_blacklist")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TokenBlacklist {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,unique=true,columnDefinition="TEXT") private String token;
    @Column(name="expires_at",nullable=false) private LocalDateTime expiresAt;
    @CreationTimestamp @Column(name="created_at",nullable=false,updatable=false) private LocalDateTime createdAt;
}
