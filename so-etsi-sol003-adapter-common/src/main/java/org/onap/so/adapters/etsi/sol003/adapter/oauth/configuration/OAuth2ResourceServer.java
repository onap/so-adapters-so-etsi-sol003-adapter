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

import jakarta.servlet.http.HttpServletRequest;
import org.onap.so.adapters.etsi.sol003.adapter.common.CommonConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Enforces oauth (bearer) token based authentication when a token is provided in the request.
 * <p>
 * Reimplemented for Spring Security 6 as a {@link SecurityFilterChain} bean. It only applies to requests that carry an
 * {@code Authorization: Bearer} header AND target the {@code /grants/**} or {@code /lcn/**} endpoints (see
 * {@link OAuth2ResourceServerRequestMatcher}). Any other request — including {@code /grants} or {@code /lcn} requests
 * without a bearer token — does not match this chain and therefore falls through to the basic-auth chain
 * ({@code EtsiSol003AdapterBasicHttpSecurityConfigurer}, ordered after this one).
 * <p>
 * The JWTs are validated using the {@code JwtDecoder} bean exposed by {@link AuthorizationServerConfig}, which is backed
 * by the same JWK source used to sign the tokens issued by the co-located authorization server.
 */
@Configuration
public class OAuth2ResourceServer {

    @Bean
    @Order(0)
    public SecurityFilterChain resourceServerSecurityFilterChain(final HttpSecurity http) throws Exception {
        http.securityMatcher(new OAuth2ResourceServerRequestMatcher())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(CommonConstants.BASE_URL + "/grants/**", CommonConstants.BASE_URL + "/lcn/**")
                        .authenticated().anyRequest().authenticated())
                .csrf(csrf -> csrf.disable())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        return http.build();
    }

    private static class OAuth2ResourceServerRequestMatcher implements RequestMatcher {
        @Override
        public boolean matches(final HttpServletRequest request) {
            final String auth = request.getHeader("Authorization");
            final String uri = request.getRequestURI();
            return (auth != null && auth.startsWith("Bearer") && (uri.contains("/grants") || uri.contains("/lcn/")));
        }
    }
}
