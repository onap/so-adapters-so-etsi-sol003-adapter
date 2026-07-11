/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019 Nordix Foundation.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.so.adapters.etsi.sol003.adapter.oauth.configuration;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.time.Duration;
import java.util.UUID;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configures the (embedded) authorization server for oauth token based authentication using the
 * <a href="https://spring.io/projects/spring-authorization-server">Spring Authorization Server</a>.
 * <p>
 * The externally observable contract is preserved from the previous {@code spring-security-oauth2}
 * ({@code @EnableAuthorizationServer}) implementation:
 * <ul>
 * <li>token endpoint remains at {@code /oauth/token} (Spring Authorization Server would default to
 * {@code /oauth2/token}, so it is overridden here),</li>
 * <li>a single in-memory client whose {@code clientId:clientSecret} is read from the
 * {@code vnfmadapter.auth} property (defaulting to {@code vnfm:vnfm}), the secret being BCrypt encoded,</li>
 * <li>{@code client_credentials} grant only,</li>
 * <li>scope {@code write},</li>
 * <li>access token validity of one day.</li>
 * </ul>
 * The issued token format necessarily changes from the previous implementation to a Spring
 * Authorization Server signed JWT; the endpoint path, grant type and scope are unchanged.
 */
@Configuration
public class AuthorizationServerConfig {

    private static final Duration ONE_DAY = Duration.ofDays(1);
    private static final String TOKEN_ENDPOINT = "/oauth/token";

    @Value("${vnfmadapter.auth:vnfm:vnfm}")
    private String vnfmAdapterAuth;

    /**
     * Security filter chain for the authorization server endpoints (e.g. the {@code /oauth/token} token endpoint). It is
     * given the highest precedence so it is evaluated ahead of the resource-server and basic-auth chains, but its
     * request matcher only matches the authorization-server endpoints.
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain authorizationServerSecurityFilterChain(final HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
        http.csrf(csrf -> csrf.disable());
        return http.build();
    }

    @Bean
    public RegisteredClientRepository registeredClientRepository(final PasswordEncoder passwordEncoder) {
        final String[] decryptedAuth = vnfmAdapterAuth.split(":");
        final RegisteredClient registeredClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(decryptedAuth[0]).clientSecret(passwordEncoder.encode(decryptedAuth[1]))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS).scope("write")
                .clientSettings(ClientSettings.builder().requireProofKey(false).build())
                .tokenSettings(TokenSettings.builder().accessTokenTimeToLive(ONE_DAY).build()).build();
        return new InMemoryRegisteredClientRepository(registeredClient);
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder().tokenEndpoint(TOKEN_ENDPOINT).build();
    }

    /**
     * The RSA key used both to sign the JWTs issued by this authorization server and (via the {@link #jwtDecoder}) to
     * validate them within the co-located resource server.
     */
    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        final KeyPair keyPair = generateRsaKey();
        final RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        final RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        final RSAKey rsaKey =
                new RSAKey.Builder(publicKey).privateKey(privateKey).keyID(UUID.randomUUID().toString()).build();
        final JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }

    @Bean
    public JwtDecoder jwtDecoder(final JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    private static KeyPair generateRsaKey() {
        try {
            final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            return keyPairGenerator.generateKeyPair();
        } catch (final Exception ex) {
            throw new IllegalStateException("Unable to generate RSA key for the authorization server", ex);
        }
    }

}
