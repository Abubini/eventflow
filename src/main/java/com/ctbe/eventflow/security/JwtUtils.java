package com.ctbe.eventflow.security;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
@Component
public class JwtUtils {
    private static final Logger log = LoggerFactory.getLogger(JwtUtils.class);
    @Value("${app.jwt.secret}") private String jwtSecret;
    @Value("${app.jwt.expiration-ms}") private long jwtExpirationMs;
    private SecretKey key() { return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)); }
    public String generateToken(String email) {
        Date now=new Date(); Date expiry=new Date(now.getTime()+jwtExpirationMs);
        return Jwts.builder().subject(email).issuedAt(now).expiration(expiry).signWith(key()).compact();
    }
    public String getEmailFromToken(String token) {
        return Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload().getSubject();
    }
    public LocalDateTime getExpiryFromToken(String token) {
        Date expiry=Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload().getExpiration();
        return expiry.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
    public boolean validateToken(String token) {
        try { Jwts.parser().verifyWith(key()).build().parseSignedClaims(token); return true; }
        catch (JwtException|IllegalArgumentException e) { log.warn("Invalid JWT: {}",e.getMessage()); return false; }
    }
    public long getExpirationMs() { return jwtExpirationMs; }
}
