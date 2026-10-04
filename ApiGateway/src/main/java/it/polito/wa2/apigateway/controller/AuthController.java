package it.polito.wa2.apigateway.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @GetMapping("/userinfo")
    public Map<String, Object> getUserInfo(@AuthenticationPrincipal OidcUser principal) {
        if (principal == null) {
            throw new RuntimeException("User not authenticated");
        }

        logger.info("=== getUserInfo called for: {} ===", principal.getPreferredUsername());
        logger.info("ID Token claims available: {}", principal.getClaims().keySet());

        principal.getClaims().forEach((key, value) -> logger.info("  Claim '{}': {}", key, value));

        List<String> roles = Collections.emptyList();

        try {
            Map<String, Object> realmAccess = principal.getClaim("realm_access");
            logger.info("realm_access claim: {}", realmAccess);
            if (realmAccess != null) {
                Object rolesObj = realmAccess.get("roles");
                if (rolesObj instanceof List<?> list) {
                    roles = list.stream().map(Object::toString).toList();
                }
                logger.info("Roles extracted: {}", roles);
            } else {
                logger.warn("✗ realm_access is NULL in ID token!");
            }
        } catch (Exception e) {
            logger.error("Error extracting roles", e);
        }

        Set<String> ignoredRoles = Set.of("offline_access", "uma_authorization");

        List<String> cleanRoles = roles.stream()
                .filter(role -> !ignoredRoles.contains(role) && !role.startsWith("default-roles"))
                .collect(Collectors.toList());

        logger.info("Clean roles to return: {}", cleanRoles);

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("username", principal.getPreferredUsername() != null ? principal.getPreferredUsername() : (principal.getEmail() != null ? principal.getEmail() : "unknown"));
        userInfo.put("email", principal.getEmail() != null ? principal.getEmail() : "");
        userInfo.put("name", principal.getFullName() != null ? principal.getFullName() : (principal.getGivenName() != null ? principal.getGivenName() : ""));
        userInfo.put("roles", cleanRoles);
        userInfo.put("sub", principal.getSubject());

        return userInfo;
    }
}
