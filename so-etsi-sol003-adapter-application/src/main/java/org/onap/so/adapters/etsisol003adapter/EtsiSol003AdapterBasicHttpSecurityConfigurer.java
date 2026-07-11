/*-
 * ============LICENSE_START=======================================================
 * ONAP - SO
 * ================================================================================
 * Copyright (C) 2017 - 2018 AT&T Intellectual Property. All rights reserved.
 * ================================================================================
 * Modifications Copyright (c) 2019 Samsung
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
 * ============LICENSE_END=========================================================
 */

package org.onap.so.adapters.etsisol003adapter;

import java.util.Arrays;
import org.onap.so.adapters.etsi.sol003.adapter.common.CommonConstants;
import org.onap.so.security.HttpSecurityConfigurer;
import org.onap.so.security.SoUserCredentialConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * @author Waqas Ikram (waqas.ikram@est.tech)
 * @author Gareth Roper (gareth.roper@est.tech)
 */
@Primary
@Component
public class EtsiSol003AdapterBasicHttpSecurityConfigurer implements HttpSecurityConfigurer {

    @Autowired
    private SoUserCredentialConfiguration soUserCredentialConfiguration;

    @Value("${server.ssl.client-auth:none}")
    private String clientAuth;

    private static RequestMatcher[] antMatchers(final String... patterns) {
        return Arrays.stream(patterns).map(AntPathRequestMatcher::new).toArray(RequestMatcher[]::new);
    }

    @Override
    public void configure(final HttpSecurity http) throws Exception {
        if (("need").equalsIgnoreCase(clientAuth)) {
            http.csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());
        } else {
            final String roles =
                    StringUtils.collectionToDelimitedString(soUserCredentialConfiguration.getRoles(), ",");
            http.csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(authorize -> authorize
                            .requestMatchers(antMatchers("/manage/health", "/manage/info")).permitAll()
                            .requestMatchers(new AntPathRequestMatcher(
                                    CommonConstants.ETSI_SUBSCRIPTION_NOTIFICATION_BASE_URL, HttpMethod.GET.name()))
                            .permitAll().requestMatchers(antMatchers("/**")).hasAnyRole(roles.split(",")))
                    .httpBasic(httpBasic -> {
                    });
        }
    }
}

