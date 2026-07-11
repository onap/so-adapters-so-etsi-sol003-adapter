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

package org.onap.so.adapters.etsisol003adapter.lcm.extclients.vnfm;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.net.ssl.SSLContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.onap.aai.domain.yang.EsrSystemInfo;
import org.onap.aai.domain.yang.EsrVnfm;
import org.onap.so.adapters.etsi.sol003.adapter.common.GsonProvider;
import org.onap.so.adapters.etsi.sol003.adapter.common.configuration.AbstractServiceProviderConfiguration;
import org.onap.so.adapters.etsi.sol003.adapter.common.utils.LocalDateTimeTypeAdapter;
import org.onap.so.configuration.BasicHttpHeadersProvider;
import org.onap.so.rest.service.HttpRestServiceProvider;
import org.onap.so.rest.service.HttpRestServiceProviderImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.web.client.RestTemplate;
import com.google.gson.Gson;

/**
 * Configures the HttpRestServiceProvider for REST call to a VNFM.
 */
@Configuration
public class VnfmServiceProviderConfiguration extends AbstractServiceProviderConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(VnfmServiceProviderConfiguration.class);
    private final Map<String, HttpRestServiceProvider> mapOfVnfmIdToHttpRestServiceProvider = new ConcurrentHashMap<>();

    @Value("${http.client.ssl.trust-store:#{null}}")
    private Resource trustStore;
    @Value("${http.client.ssl.trust-store-password:#{null}}")
    private String trustStorePassword;

    @Value("${server.ssl.key-store:#{null}}")
    private Resource keyStoreResource;
    @Value("${server.ssl.key--store-password:#{null}}")
    private String keyStorePassword;

    /**
     * This property is only intended to be temporary until the AAI schema is updated to support setting the endpoint
     */
    @Value("${vnfmadapter.temp.vnfm.oauth.endpoint:#{null}}")
    private String oauthEndpoint;

    @Qualifier(VnfmRestTemplateConfiguration.SOL003_LCM_REST_TEMPLATE)
    @Autowired
    private RestTemplate defaultRestTemplate;

    @Autowired
    private GsonProvider gsonProvider;
    
    private final LocalDateTimeTypeAdapter localDateTimeTypeAdapter =  new LocalDateTimeTypeAdapter(DateTimeFormatter.ISO_LOCAL_DATE);


    public HttpRestServiceProvider getHttpRestServiceProvider(final EsrVnfm vnfm) {
        if (!mapOfVnfmIdToHttpRestServiceProvider.containsKey(vnfm.getVnfmId())) {
            mapOfVnfmIdToHttpRestServiceProvider.put(vnfm.getVnfmId(), createHttpRestServiceProvider(vnfm));
        }
        return mapOfVnfmIdToHttpRestServiceProvider.get(vnfm.getVnfmId());
    }

    private HttpRestServiceProvider createHttpRestServiceProvider(final EsrVnfm vnfm) {
        final RestTemplate restTemplate = createRestTemplate(vnfm);
        setGsonMessageConverter(restTemplate);
        if (trustStore != null) {
            setTrustStore(restTemplate);
        }
        return new HttpRestServiceProviderImpl(restTemplate, new BasicHttpHeadersProvider().getHttpHeaders());
    }

    private RestTemplate createRestTemplate(final EsrVnfm vnfm) {
        if (vnfm != null) {
            for (final EsrSystemInfo esrSystemInfo : vnfm.getEsrSystemInfoList().getEsrSystemInfo()) {
                if (!StringUtils.isEmpty(esrSystemInfo.getUserName())
                        && !StringUtils.isEmpty(esrSystemInfo.getPassword())) {
                    return createOAuth2RestTemplate(esrSystemInfo);
                }
            }
        }
        return defaultRestTemplate;
    }

    /**
     * Builds a {@link RestTemplate} that attaches a {@code client_credentials} bearer token (obtained from the VNFM's
     * token endpoint) to every outbound request. Reimplemented for Spring Security 6 using an
     * {@link OAuth2AuthorizedClientManager} in place of the removed {@code OAuth2RestTemplate} /
     * {@code ClientCredentialsResourceDetails}. The token URI, client id and client secret are derived from the AAI
     * {@link EsrSystemInfo} exactly as before.
     */
    private RestTemplate createOAuth2RestTemplate(final EsrSystemInfo esrSystemInfo) {
        logger.debug("Getting OAuth2 client_credentials RestTemplate ...");
        final String tokenUri =
                oauthEndpoint == null ? esrSystemInfo.getServiceUrl().replace("vnflcm/v1", "oauth/token")
                        : oauthEndpoint;
        final ClientRegistration clientRegistration = ClientRegistration
                .withRegistrationId(UUID.randomUUID().toString())
                .tokenUri(tokenUri).clientId(esrSystemInfo.getUserName()).clientSecret(esrSystemInfo.getPassword())
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC).build();

        final OAuth2AuthorizedClientManager authorizedClientManager = createAuthorizedClientManager(clientRegistration);

        final RestTemplate restTemplate = new RestTemplate();
        restTemplate.getInterceptors()
                .add(new OAuth2ClientCredentialsInterceptor(authorizedClientManager, clientRegistration));
        return restTemplate;
    }

    private OAuth2AuthorizedClientManager createAuthorizedClientManager(final ClientRegistration clientRegistration) {
        final ClientRegistrationRepository clientRegistrationRepository =
                new InMemoryClientRegistrationRepository(clientRegistration);
        final AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientManager =
                new AuthorizedClientServiceOAuth2AuthorizedClientManager(clientRegistrationRepository,
                        new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository));
        authorizedClientManager.setAuthorizedClientProvider(
                OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build());
        return authorizedClientManager;
    }

    /**
     * Fetches (and caches, within the {@link OAuth2AuthorizedClientManager}) a {@code client_credentials} access token
     * and adds it as a {@code Bearer} authorization header on each outbound request.
     */
    private static class OAuth2ClientCredentialsInterceptor implements ClientHttpRequestInterceptor {

        private final OAuth2AuthorizedClientManager authorizedClientManager;
        private final ClientRegistration clientRegistration;

        OAuth2ClientCredentialsInterceptor(final OAuth2AuthorizedClientManager authorizedClientManager,
                final ClientRegistration clientRegistration) {
            this.authorizedClientManager = authorizedClientManager;
            this.clientRegistration = clientRegistration;
        }

        @Override
        public ClientHttpResponse intercept(final org.springframework.http.HttpRequest request, final byte[] body,
                final ClientHttpRequestExecution execution) throws IOException {
            final OAuth2AuthorizeRequest authorizeRequest =
                    OAuth2AuthorizeRequest.withClientRegistrationId(clientRegistration.getRegistrationId())
                            .principal(clientRegistration.getClientId()).build();
            final OAuth2AuthorizedClient authorizedClient = authorizedClientManager.authorize(authorizeRequest);
            if (authorizedClient != null && authorizedClient.getAccessToken() != null) {
                request.getHeaders().setBearerAuth(authorizedClient.getAccessToken().getTokenValue());
            } else {
                logger.error("Unable to obtain OAuth2 client_credentials access token from token endpoint {}",
                        clientRegistration.getProviderDetails().getTokenUri());
            }
            return execution.execute(request, body);
        }
    }

    private void setTrustStore(final RestTemplate restTemplate) {
        SSLContext sslContext;
        try {
            if (keyStoreResource != null) {
                final KeyStore keystore = KeyStore.getInstance("pkcs12");
                keystore.load(keyStoreResource.getInputStream(), keyStorePassword.toCharArray());
                sslContext =
                        new SSLContextBuilder().loadTrustMaterial(trustStore.getURL(), trustStorePassword.toCharArray())
                                .loadKeyMaterial(keystore, keyStorePassword.toCharArray()).build();
            } else {
                sslContext = new SSLContextBuilder()
                        .loadTrustMaterial(trustStore.getURL(), trustStorePassword.toCharArray()).build();
            }
            logger.info("Setting truststore: {}", trustStore.getURL());
            final SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(sslContext);
            final HttpClientConnectionManager connectionManager =
                    PoolingHttpClientConnectionManagerBuilder.create().setSSLSocketFactory(socketFactory).build();
            final HttpClient httpClient = HttpClients.custom().setConnectionManager(connectionManager).build();
            final HttpComponentsClientHttpRequestFactory factory =
                    new HttpComponentsClientHttpRequestFactory(httpClient);
            restTemplate.setRequestFactory(new BufferingClientHttpRequestFactory(factory));
        } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException | CertificateException
                | IOException | UnrecoverableKeyException exception) {
            logger.error("Error reading truststore, TLS connection to VNFM will fail.", exception);
        }
    }

    @Override
    protected Gson getGson() {
        return gsonProvider.getGson(localDateTimeTypeAdapter);
    }

}