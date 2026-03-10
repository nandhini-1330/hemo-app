package com.anemia.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.util.*;
import java.util.function.Function;

@Service
public class JwtService {
    @Value("${app.jwt.secret}") private String jwtSecret;
    @Value("${app.jwt.expiration-ms}") private long jwtExpirationMs;

    public String generateToken(UserDetails userDetails) {
        return Jwts.builder().claims(new HashMap<>()).subject(userDetails.getUsername())
                .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSignKey()).compact();
    }
    public boolean isTokenValid(String token, UserDetails userDetails) {
        return extractUsername(token).equals(userDetails.getUsername()) && !isTokenExpired(token);
    }
    public String extractUsername(String token) { return extractClaim(token, Claims::getSubject); }
    public <T> T extractClaim(String token, Function<Claims, T> resolver) { return resolver.apply(extractAllClaims(token)); }
    private boolean isTokenExpired(String token) { return extractClaim(token, Claims::getExpiration).before(new Date()); }
    private Claims extractAllClaims(String token) { return Jwts.parser().verifyWith(getSignKey()).build().parseSignedClaims(token).getPayload(); }
    private SecretKey getSignKey() {
        byte[] k = jwtSecret.getBytes();
        if (k.length < 32) { byte[] p = new byte[32]; System.arraycopy(k,0,p,0,k.length); k=p; }
        return Keys.hmacShaKeyFor(k);
    }
}