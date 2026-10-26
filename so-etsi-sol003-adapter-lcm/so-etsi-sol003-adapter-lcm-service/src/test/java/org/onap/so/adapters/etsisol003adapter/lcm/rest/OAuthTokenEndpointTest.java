/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2026 Deutsche Telekom AG.
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

package org.onap.so.adapters.etsisol003adapter.lcm.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import java.net.URI;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class OAuthTokenEndpointTest {

    @LocalServerPort
    private int port;

    @Test
    public void clientCredentialsGrantIssuesAccessToken() throws Exception {
        final TestRestTemplate restTemplate = new TestRestTemplate("vnfm", "password1$");
        final RequestEntity<String> request = RequestEntity.post(new URI("http://localhost:" + port + "/oauth/token"))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED).body("grant_type=client_credentials&scope=write");

        final ResponseEntity<String> response = restTemplate.exchange(request, String.class);

        assertEquals(200, response.getStatusCode().value());
        final JsonObject body = JsonParser.parseString(response.getBody()).getAsJsonObject();
        assertFalse(body.get("access_token").getAsString().isEmpty());
        assertEquals("Bearer", body.get("token_type").getAsString());
        assertEquals("write", body.get("scope").getAsString());
    }

    @Test
    public void wrongClientSecretIsRejected() throws Exception {
        final TestRestTemplate restTemplate = new TestRestTemplate("vnfm", "wrong");
        final RequestEntity<String> request = RequestEntity.post(new URI("http://localhost:" + port + "/oauth/token"))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED).body("grant_type=client_credentials&scope=write");

        final ResponseEntity<String> response = restTemplate.exchange(request, String.class);

        assertEquals(401, response.getStatusCode().value());
    }
}
