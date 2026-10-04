package it.polito.wa2.aiprocessingservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@Profile("!test")
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> {})
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            );

        return http.build();
    }

    private Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(new KeycloakRealmRoleConverter());
        return jwtConverter;
    }
}

class KeycloakRealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
    private final Logger logger = LoggerFactory.getLogger(KeycloakRealmRoleConverter.class);

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        logger.info("=== Converting JWT to authorities ===");
        logger.info("JWT Subject: " + jwt.getSubject());
        logger.info("JWT Claims: " + jwt.getClaims().keySet());

        List<GrantedAuthority> authorities = new ArrayList<>();

        // 1. Extract roles from realm_access
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        logger.info("realm_access claim: " + realmAccess);

        if (realmAccess != null && !realmAccess.isEmpty()) {
            List<String> roles = (List<String>) realmAccess.getOrDefault("roles", new ArrayList<>());
            logger.info("Roles from realm_access: " + roles);
            for (String role : roles) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
            }
        }

        // 2. Extract roles from resource_access (client roles)
        Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
        logger.info("resource_access claim: " + resourceAccess);

        if (resourceAccess != null) {
            for (Map.Entry<String, Object> entry : resourceAccess.entrySet()) {
                String clientId = entry.getKey();
                Object clientAccessObj = entry.getValue();
                if (clientAccessObj instanceof Map) {
                    Map<String, Object> clientAccess = (Map<String, Object>) clientAccessObj;
                    List<String> clientRoles = (List<String>) clientAccess.get("roles");
                    if (clientRoles != null) {
                        logger.info("Roles from client '" + clientId + "': " + clientRoles);
                        for (String role : clientRoles) {
                            authorities.add(new SimpleGrantedAuthority("ROLE_" + clientId + "_" + role));
                        }
                    }
                }
            }
        }

        // 3. Add client_id as a role for service accounts
        String azp = jwt.getClaimAsString("azp");
        if (azp != null) {
            logger.info("Adding client_id '" + azp + "' as role");
            authorities.add(new SimpleGrantedAuthority("ROLE_" + azp));
        }

        logger.info("Final granted authorities: " + authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList()));

        return authorities;
    }
}
