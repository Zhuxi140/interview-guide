package interview.common.util;

import interview.common.exception.UnauthorizedException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

/**
 * @author zhuxi
 * @apiNote JWT 工具类
 */


@Slf4j
public class JwttUtil {

    private final Long expiration;
    private final SecretKey key;
    private final JwtParser parser;

    public JwttUtil(Long expiration, String secretString) {
        this.expiration = expiration;
        byte[] bytes = secretString.getBytes(StandardCharsets.UTF_8);
        this.key = Keys.hmacShaKeyFor(bytes);
        this.parser = Jwts.parser()
                .verifyWith(key)
                .build();
    }

    /**
     * 生成 JWT Token
     * @param data 存储的数据
     * @return 生成的 Token
     */
    public String generatorToken(Map<String, Object> data, Long userId) {
        String userIdString = Optional.ofNullable(userId)
                .map(String::valueOf)
                .orElseThrow(() -> new IllegalArgumentException("User ID cannot be null"));

        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);
        Date expireDate = new Date(nowMillis + (expiration * 60 * 1000));
        return Jwts.builder()
                .claims(data != null ? data : Map.of())
                .subject(userIdString)
                .issuedAt(now)
                .expiration(expireDate)
                .signWith(key)
                .compact();
    }

    /**
     * 解析 JWT Token
     * @param token 待解析的 Token
     * @return 解析后的数据
     */
    public Claims parseToken(String token){
        try {
            return  parser
                    .parseSignedClaims(token)
                    .getPayload();
            } catch (JwtException | IllegalArgumentException e){
            log.error("JWT 解析失败: {}",e.getMessage());
            throw new UnauthorizedException();
            }
    }

}
