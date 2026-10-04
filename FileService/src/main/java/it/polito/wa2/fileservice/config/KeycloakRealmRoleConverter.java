package it.polito.wa2.fileservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class KeycloakRealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        logger.info("=== Converting JWT to authorities ===");
        logger.info("JWT Subject: {}", jwt.getSubject());
        logger.info("JWT Claims: {}", jwt.getClaims().keySet());

        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        logger.info("realm_access claim: {}", realmAccess);

        if (realmAccess == null || realmAccess.isEmpty()) {
            logger.warn("No realm_access found in JWT");
            return Collections.emptyList();
        }

        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) realmAccess.get("roles");
        if (roles == null) {
            return Collections.emptyList();
        }

        logger.info("Roles from JWT: {}", roles);

        List<GrantedAuthority> authorities = roles.stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
            .collect(Collectors.toList());
            
        logger.info("Granted authorities: {}", authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList()));

        return authorities;
    }
}
