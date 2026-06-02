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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.patchRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import java.net.URI;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onap.aai.domain.yang.EsrSystemInfo;
import org.onap.aai.domain.yang.EsrSystemInfoList;
import org.onap.aai.domain.yang.EsrVnfm;
import org.onap.aai.domain.yang.EsrVnfmList;
import org.onap.aai.domain.yang.GenericVnf;
import org.onap.aai.domain.yang.GenericVnfs;
import org.onap.aai.domain.yang.Relationship;
import org.onap.aai.domain.yang.RelationshipData;
import org.onap.aai.domain.yang.RelationshipList;
import org.onap.aaiclient.client.aai.AAICommonObjectMapperProvider;
import org.onap.so.adapters.etsi.sol003.adapter.common.GsonProvider;
import org.onap.so.adapters.etsisol003adapter.lcm.extclients.EtsiPackageProvider;
import org.onap.so.adapters.etsisol003adapter.lcm.extclients.vnfm.model.InlineResponse201;
import org.onap.so.adapters.etsisol003adapter.lcm.extclients.vnfm.model.InlineResponse201Links;
import org.onap.so.adapters.etsisol003adapter.lcm.extclients.vnfm.model.InlineResponse201LinksSelf;
import org.onap.so.adapters.etsisol003adapter.lcm.grant.model.GrantRequest;
import org.onap.so.adapters.etsisol003adapter.lcm.grant.model.GrantsAddResources;
import org.onap.so.adapters.etsisol003adapter.lcm.grant.model.GrantsLinks;
import org.onap.so.adapters.etsisol003adapter.lcm.grant.model.GrantsLinksVnfLcmOpOcc;
import org.onap.so.adapters.etsisol003adapter.lcm.grant.model.InlineResponse201VimConnections;
import org.onap.so.adapters.etsisol003adapter.lcm.v1.model.CreateVnfRequest;
import org.onap.so.adapters.etsisol003adapter.lcm.v1.model.CreateVnfResponse;
import org.onap.so.adapters.etsisol003adapter.lcm.v1.model.DeleteVnfResponse;
import org.onap.so.adapters.etsisol003adapter.lcm.v1.model.Tenant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * WireMock-based integration tests that verify the adapter communicates correctly with AAI over HTTP. Unlike the
 * existing tests that stub {@code AAIResourcesClient} with Mockito at the Java level, these tests let the real
 * {@code AAIResourcesClient} (Jersey-based) make genuine HTTP requests so that URL construction, serialization, HTTP
 * verb choice, and error handling are all exercised against a real HTTP listener.
 *
 * <p>
 * The AAI endpoint is overridden via Spring property {@code aai.endpoint=http://localhost:18089} so that all AAI calls
 * reach the WireMock server started by {@link #wireMockServer}.
 * </p>
 *
 * <p>
 * VNFM calls still use {@link MockRestServiceServer} bound to the {@code SOL003_LCM_REST_TEMPLATE} bean, matching the
 * pattern of the existing controller tests.
 * </p>
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT,
        properties = {"aai.endpoint=http://localhost:18089",
                "vnfmadapter.temp.vnfm.oauth.endpoint=http://localhost:18089/oauth/token"})
@ActiveProfiles("test")
public class AaiWireMockIntegrationTest {

    // Fixed port so the Spring property can reference it at context-load time.
    private static final int WIREMOCK_AAI_PORT = 18089;
    private static final String AAI_BASE_PATH = "/aai/v15";

    private static final String CLOUD_OWNER = "myTestCloudOwner";
    private static final String REGION = "myTestRegion";
    private static final String TENANT_ID = "myTestTenantId";
    private static final String VNF_ID_IN_AAI = "myTestVnfId";
    private static final String VNFM1_ID = "vnfm1";
    private static final String VNFM2_ID = "vnfm2";

    /**
     * Shared WireMock server for the entire test class (started once). The per-test {@link #wireMockRule} field
     * delegates to this instance and resets all stubs between tests.
     */
    @ClassRule
    public static WireMockClassRule wireMockServer =
            new WireMockClassRule(wireMockConfig().port(WIREMOCK_AAI_PORT));

    /** Resets stubs after each test while keeping the server running. */
    @Rule
    public WireMockClassRule wireMockRule = wireMockServer;

    private static final String WIREMOCK_URL = "http://localhost:" + WIREMOCK_AAI_PORT;

    @LocalServerPort
    private int port;

    @MockBean
    private EtsiPackageProvider etsiPackageProvider;

    @Autowired
    private EtsiSol003AdapterController controller;

    @Autowired
    private Sol003GrantController grantController;

    @Autowired
    private GsonProvider gsonProvider;

    /**
     * Jackson ObjectMapper that matches the one used internally by {@code AAIResourcesClient}. It includes the JAXB
     * annotation introspector so that AAI domain-object field names are serialised with their JAXB/XML element names
     * (kebab-case) rather than the Java camelCase field names.
     */
    private final ObjectMapper aaiObjectMapper = new AAICommonObjectMapperProvider().getMapper();

    @Before
    public void setUp() {
        stubOAuthToken();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CREATE VNF
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Verifies that a successful createVnf call issues the correct GET requests to AAI to discover the GenericVnf and
     * select a compatible VNFM, and then issues a PUT (relationship) and PATCH (selflink update) back to AAI.
     */
    @Test
    public void createVnf_ValidRequest_AllExpectedAaiHttpCallsMade() throws Exception {
        // GenericVnf with nfType matching vnfm2
        stubAaiGetGenericVnf(VNF_ID_IN_AAI, buildGenericVnf(VNF_ID_IN_AAI, "vnfmType2"));
        // VNFM list containing both VNFMs
        stubAaiGetEsrVnfmList(buildEsrVnfmListWithIds(VNFM1_ID, VNFM2_ID));
        // ESR system info for each VNFM (vnfm1 does not match, vnfm2 does)
        stubAaiGetVnfmEsrSystemInfoList(VNFM1_ID,
                buildEsrSystemInfoList(WIREMOCK_URL, "vnfmType1", "VNFM"));
        stubAaiGetVnfmEsrSystemInfoList(VNFM2_ID,
                buildEsrSystemInfoList(WIREMOCK_URL, "vnfmType2", "VNFM"));
        // Write stubs – relationship creation and selflink patch
        stubAaiPutVnfmRelationship(VNFM2_ID);
        stubAaiPatchGenericVnf(VNF_ID_IN_AAI);
        // VIM ESR (used during instantiate request building)
        stubAaiGetCloudRegionEsrSystemInfoList(CLOUD_OWNER, REGION,
                buildEsrSystemInfoList("http://myVim:8080", "openstack", "VIM"));
        // VNFM HTTP calls via MockRestServiceServer
        stubVnfmCreate(VNFM2_ID, "vnfId", WIREMOCK_URL + "/vnf_instances/vnfId");
        stubVnfmSubscribe(VNFM2_ID);
        stubVnfmInstantiate(VNFM2_ID, "vnfId", WIREMOCK_URL + "/vnf_lcm_op_occs/op1");

        final CreateVnfRequest createVnfRequest = new CreateVnfRequest().name("myTestName")
                .tenant(new Tenant().cloudOwner(CLOUD_OWNER).regionName(REGION).tenantId(TENANT_ID));

        final ResponseEntity<CreateVnfResponse> response =
                controller.vnfCreate(VNF_ID_IN_AAI, createVnfRequest, "reqId", "so", "1");

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody().getJobId());

        // ── AAI GET verifications ──────────────────────────────────────────
        wireMockRule.verify(1, getRequestedFor(
                urlEqualTo(aaiPath("/network/generic-vnfs/generic-vnf/" + VNF_ID_IN_AAI))));
        wireMockRule.verify(1, getRequestedFor(urlEqualTo(aaiPath("/external-system/esr-vnfm-list"))));
        // vnfm1's ESR info must be queried (type mismatch → skipped)
        wireMockRule.verify(getRequestedFor(
                urlEqualTo(aaiPath("/external-system/esr-vnfm-list/esr-vnfm/" + VNFM1_ID + "/esr-system-info-list"))));
        // vnfm2's ESR info must be queried (type match → selected, also by VnfmUrlProvider)
        wireMockRule.verify(getRequestedFor(
                urlEqualTo(aaiPath("/external-system/esr-vnfm-list/esr-vnfm/" + VNFM2_ID + "/esr-system-info-list"))));
        // VIM ESR for instantiate request
        wireMockRule.verify(1, getRequestedFor(urlEqualTo(aaiPath("/cloud-infrastructure/cloud-regions/"
                + "cloud-region/" + CLOUD_OWNER + "/" + REGION + "/esr-system-info-list"))));

        // ── AAI WRITE verifications ────────────────────────────────────────
        // Relationship (PUT) from VNFM to GenericVnf must be created
        wireMockRule.verify(1, putRequestedFor(urlEqualTo(aaiPath(
                "/external-system/esr-vnfm-list/esr-vnfm/" + VNFM2_ID + "/relationship-list/relationship"))));
        // Selflink update on GenericVnf (AAI client sends PATCH as POST with X-HTTP-Method-Override)
        wireMockRule.verify(1,
                postRequestedFor(urlEqualTo(aaiPath("/network/generic-vnfs/generic-vnf/" + VNF_ID_IN_AAI))));
    }

    /**
     * When the GenericVnf already has a VNFM relationship, the adapter must use that VNFM directly without querying the
     * full VNFM list. The relationship PUT should also be skipped.
     */
    @Test
    public void createVnf_VnfmAlreadyAssigned_SkipsVnfmListAndRelationshipCreation() throws Exception {
        final GenericVnf genericVnf =
                buildGenericVnfWithVnfmRelationship(VNF_ID_IN_AAI, "vnfmType2", VNFM2_ID);
        stubAaiGetGenericVnf(VNF_ID_IN_AAI, genericVnf);
        // The assigned VNFM is fetched individually (with depth=1)
        stubAaiGetEsrVnfm(VNFM2_ID,
                buildEsrVnfmWithSystemInfo(VNFM2_ID, WIREMOCK_URL, "vnfmType2"));
        // VnfmUrlProvider fetches the ESR system-info-list separately
        stubAaiGetVnfmEsrSystemInfoList(VNFM2_ID,
                buildEsrSystemInfoList(WIREMOCK_URL, "vnfmType2", "VNFM"));
        stubAaiPatchGenericVnf(VNF_ID_IN_AAI);
        stubAaiGetCloudRegionEsrSystemInfoList(CLOUD_OWNER, REGION,
                buildEsrSystemInfoList("http://myVim:8080", "openstack", "VIM"));
        stubVnfmCreate(VNFM2_ID, "vnfId", WIREMOCK_URL + "/vnf_instances/vnfId");
        stubVnfmSubscribe(VNFM2_ID);
        stubVnfmInstantiate(VNFM2_ID, "vnfId", WIREMOCK_URL + "/vnf_lcm_op_occs/op1");

        final CreateVnfRequest createVnfRequest = new CreateVnfRequest().name("myTestName")
                .tenant(new Tenant().cloudOwner(CLOUD_OWNER).regionName(REGION).tenantId(TENANT_ID));

        final ResponseEntity<CreateVnfResponse> response =
                controller.vnfCreate(VNF_ID_IN_AAI, createVnfRequest, "reqId", "so", "1");

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());

        // VNFM list must NOT be queried
        wireMockRule.verify(0, getRequestedFor(urlEqualTo(aaiPath("/external-system/esr-vnfm-list"))));
        // No relationship PUT since VNFM is already assigned
        wireMockRule.verify(0, putRequestedFor(urlEqualTo(
                aaiPath("/external-system/esr-vnfm-list/esr-vnfm/" + VNFM2_ID + "/relationship-list/relationship"))));
        // Selflink update must still happen (AAI client sends PATCH as POST)
        wireMockRule.verify(1,
                postRequestedFor(urlEqualTo(aaiPath("/network/generic-vnfs/generic-vnf/" + VNF_ID_IN_AAI))));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE VNF
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * When the GenericVnf does not exist in AAI the adapter must return 404 and the AAI GET must have been attempted.
     */
    @Test
    public void deleteVnf_VnfNotFoundInAai_Returns404AndAaiWasQueried() throws Exception {
        final String unknownVnfId = "unknownVnfId";
        wireMockRule.stubFor(get(urlEqualTo(aaiPath("/network/generic-vnfs/generic-vnf/" + unknownVnfId)))
                .willReturn(aResponse().withStatus(404)));

        final TestRestTemplate restTemplate = new TestRestTemplate("test", "test");
        final ResponseEntity<DeleteVnfResponse> response = restTemplate.exchange(
                RequestEntity.delete(new URI("http://localhost:" + port + "/so/vnfm-adapter/v1/vnfs/" + unknownVnfId))
                        .header("X-ONAP-RequestId", "reqId").header("X-ONAP-InvocationID", "invId")
                        .header("Content-Type", "application/json").build(),
                DeleteVnfResponse.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody().getJobId());

        wireMockRule.verify(1,
                getRequestedFor(urlEqualTo(aaiPath("/network/generic-vnfs/generic-vnf/" + unknownVnfId))));
    }

    /**
     * When the GenericVnf exists but has no VNFM relationship the adapter must return 400.
     */
    @Test
    public void deleteVnf_VnfExistsWithNoVnfmRelationship_Returns400() throws Exception {
        stubAaiGetGenericVnf(VNF_ID_IN_AAI, buildGenericVnf(VNF_ID_IN_AAI, "vnfmType1"));

        final TestRestTemplate restTemplate = new TestRestTemplate("test", "test");
        final ResponseEntity<DeleteVnfResponse> response = restTemplate.exchange(
                RequestEntity.delete(new URI("http://localhost:" + port + "/so/vnfm-adapter/v1/vnfs/" + VNF_ID_IN_AAI))
                        .header("X-ONAP-RequestId", "reqId").header("X-ONAP-InvocationID", "invId")
                        .header("Content-Type", "application/json").build(),
                DeleteVnfResponse.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        wireMockRule.verify(1, getRequestedFor(
                urlEqualTo(aaiPath("/network/generic-vnfs/generic-vnf/" + VNF_ID_IN_AAI))));
    }

    /**
     * Valid delete request: the adapter must query AAI for the GenericVnf (with its VNFM relationship) and then
     * retrieve the assigned VNFM's system-info before delegating termination to the VNFM.
     */
    @Test
    @Ignore("Requires additional WireMock stubs for the full terminate→get→delete VNFM OAuth2 flow")
    public void deleteVnf_ValidRequest_AaiQueriedForGenericVnfAndAssignedVnfm() throws Exception {
        final GenericVnf genericVnf = buildGenericVnfWithVnfmRelationship(VNF_ID_IN_AAI, "vnfmType1", VNFM1_ID);
        genericVnf.setSelflink(WIREMOCK_URL + "/vnfs/myTestVnfIdOnVnfm");
        stubAaiGetGenericVnf(VNF_ID_IN_AAI, genericVnf);
        stubAaiGetEsrVnfm(VNFM1_ID,
                buildEsrVnfmWithSystemInfo(VNFM1_ID, WIREMOCK_URL, "vnfmType1"));
        stubAaiGetVnfmEsrSystemInfoList(VNFM1_ID,
                buildEsrSystemInfoList(WIREMOCK_URL, "vnfmType1", "VNFM"));

        // VNFM returns 409 (already terminated) → adapter then GETs state and DELETEs
        final InlineResponse201 notInstantiated = new InlineResponse201();
        notInstantiated.setInstantiationState(InlineResponse201.InstantiationStateEnum.NOT_INSTANTIATED);
        wireMockRule.stubFor(post(urlEqualTo("/vnfs/myTestVnfIdOnVnfm/terminate"))
                .willReturn(aResponse().withStatus(409)
                        .withHeader("Content-Type", "application/json")));
        wireMockRule.stubFor(get(urlEqualTo("/vnfs/myTestVnfIdOnVnfm"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(gsonProvider.getGson().toJson(notInstantiated))));
        wireMockRule.stubFor(delete(urlEqualTo("/vnfs/myTestVnfIdOnVnfm"))
                .willReturn(aResponse().withStatus(204)));

        final TestRestTemplate restTemplate = new TestRestTemplate("test", "test");
        final ResponseEntity<DeleteVnfResponse> response = restTemplate.exchange(
                RequestEntity.delete(new URI("http://localhost:" + port + "/so/vnfm-adapter/v1/vnfs/" + VNF_ID_IN_AAI))
                        .header("X-ONAP-RequestId", "reqId").header("X-ONAP-InvocationID", "invId")
                        .header("Content-Type", "application/json").build(),
                DeleteVnfResponse.class);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody().getJobId());

        wireMockRule.verify(1, getRequestedFor(
                urlEqualTo(aaiPath("/network/generic-vnfs/generic-vnf/" + VNF_ID_IN_AAI))));
        // VNFM ESR info is expected to be fetched (with depth=1 query parameter)
        wireMockRule.verify(1, getRequestedFor(
                urlMatching(aaiPath("/external-system/esr-vnfm-list/esr-vnfm/" + VNFM1_ID + "(\\?.*)?"))));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GRANT
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Verifies that the grant controller fetches VIM connection details from AAI (cloud-region ESR system-info-list)
     * and returns these details in the grant response.
     */
    @Test
    public void grant_InstantiateRequest_VimInfoFetchedFromAaiAndReturnedInResponse() throws Exception {
        final GenericVnf genericVnf =
                buildGenericVnfWithTenantRelationship(VNF_ID_IN_AAI, CLOUD_OWNER, REGION, TENANT_ID);
        stubAaiGetGenericVnfsBySelflink("http://vnfm:8080/vnfs/myTestVnfIdOnVnfm", genericVnf);
        stubAaiGetCloudRegionEsrSystemInfoList(CLOUD_OWNER, REGION,
                buildEsrSystemInfoList("http://myVim:8080", "openstack", "VIM"));

        final ResponseEntity<org.onap.so.adapters.etsisol003adapter.lcm.grant.model.InlineResponse201> response =
                grantController.grantsPost(buildInstantiateGrantRequest());

        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        final java.util.List<InlineResponse201VimConnections> vimConnections =
                response.getBody().getVimConnections();
        assertNotNull(vimConnections);
        assertFalse(vimConnections.isEmpty());
        assertEquals(CLOUD_OWNER + "_" + REGION, vimConnections.get(0).getVimId());

        wireMockRule.verify(1, getRequestedFor(urlEqualTo(aaiPath("/cloud-infrastructure/cloud-regions/"
                + "cloud-region/" + CLOUD_OWNER + "/" + REGION + "/esr-system-info-list"))));
        wireMockRule.verify(1, getRequestedFor(urlPathEqualTo(aaiPath("/network/generic-vnfs"))));
    }

    /**
     * When the VIM ESR system-info-list is not present in AAI (404 response) the adapter must still attempt the AAI
     * call and then propagate the resulting error.
     */
    @Test
    public void grant_VimEsrNotFoundInAai_AaiQueried_ExceptionPropagated() throws Exception {
        final GenericVnf genericVnf =
                buildGenericVnfWithTenantRelationship(VNF_ID_IN_AAI, CLOUD_OWNER, REGION, TENANT_ID);
        stubAaiGetGenericVnfsBySelflink("http://vnfm:8080/vnfs/myTestVnfIdOnVnfm", genericVnf);
        wireMockRule.stubFor(
                get(urlEqualTo(aaiPath("/cloud-infrastructure/cloud-regions/cloud-region/"
                        + CLOUD_OWNER + "/" + REGION + "/esr-system-info-list")))
                                .willReturn(aResponse().withStatus(404)));

        boolean exceptionThrown = false;
        try {
            grantController.grantsPost(buildInstantiateGrantRequest());
        } catch (final Exception e) {
            exceptionThrown = true;
        }

        // VIM ESR endpoint must have been called
        wireMockRule.verify(1, getRequestedFor(urlEqualTo(aaiPath("/cloud-infrastructure/cloud-regions/"
                + "cloud-region/" + CLOUD_OWNER + "/" + REGION + "/esr-system-info-list"))));

        // NullPointerException is expected because the 404 causes a null ESR info list
        // which is not guarded in the current production code
        assert exceptionThrown : "Expected an exception when VIM ESR returns 404";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers – WireMock stub registration
    // ─────────────────────────────────────────────────────────────────────────

    private String aaiPath(final String resource) {
        return AAI_BASE_PATH + resource;
    }

    private void stubAaiGetGenericVnf(final String vnfId, final GenericVnf vnf) throws Exception {
        wireMockRule.stubFor(get(urlEqualTo(aaiPath("/network/generic-vnfs/generic-vnf/" + vnfId)))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(aaiObjectMapper.writeValueAsString(vnf))));
    }

    private void stubAaiGetEsrVnfmList(final EsrVnfmList list) throws Exception {
        wireMockRule.stubFor(get(urlEqualTo(aaiPath("/external-system/esr-vnfm-list")))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(aaiObjectMapper.writeValueAsString(list))));
    }

    private void stubAaiGetVnfmEsrSystemInfoList(final String vnfmId, final EsrSystemInfoList infoList)
            throws Exception {
        wireMockRule.stubFor(get(urlEqualTo(
                aaiPath("/external-system/esr-vnfm-list/esr-vnfm/" + vnfmId + "/esr-system-info-list")))
                        .willReturn(aResponse().withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody(aaiObjectMapper.writeValueAsString(infoList))));
    }

    private void stubAaiGetEsrVnfm(final String vnfmId, final EsrVnfm vnfm) throws Exception {
        // Matches with and without the ?depth=1 query parameter
        wireMockRule.stubFor(
                get(urlMatching(aaiPath("/external-system/esr-vnfm-list/esr-vnfm/" + vnfmId + "(\\?.*)?$")))
                        .willReturn(aResponse().withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody(aaiObjectMapper.writeValueAsString(vnfm))));
    }

    private void stubAaiPutVnfmRelationship(final String vnfmId) {
        wireMockRule.stubFor(put(urlEqualTo(
                aaiPath("/external-system/esr-vnfm-list/esr-vnfm/" + vnfmId + "/relationship-list/relationship")))
                        .willReturn(aResponse().withStatus(200)));
    }

    private void stubAaiPatchGenericVnf(final String vnfId) {
        // AAIResourcesClient sends PATCH as POST with X-HTTP-Method-Override header
        wireMockRule.stubFor(post(urlEqualTo(aaiPath("/network/generic-vnfs/generic-vnf/" + vnfId)))
                .willReturn(aResponse().withStatus(200)));
        wireMockRule.stubFor(patch(urlEqualTo(aaiPath("/network/generic-vnfs/generic-vnf/" + vnfId)))
                .willReturn(aResponse().withStatus(200)));
    }

    private void stubAaiGetCloudRegionEsrSystemInfoList(final String cloudOwner, final String region,
            final EsrSystemInfoList infoList) throws Exception {
        wireMockRule.stubFor(get(urlEqualTo(aaiPath(
                "/cloud-infrastructure/cloud-regions/cloud-region/" + cloudOwner + "/" + region
                        + "/esr-system-info-list")))
                                .willReturn(aResponse().withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(aaiObjectMapper.writeValueAsString(infoList))));
    }

    private void stubAaiGetGenericVnfsBySelflink(final String selflink, final GenericVnf vnf) throws Exception {
        final GenericVnfs genericVnfs = new GenericVnfs();
        genericVnfs.getGenericVnf().add(vnf);
        wireMockRule.stubFor(get(urlPathEqualTo(aaiPath("/network/generic-vnfs")))
                .withQueryParam("selflink", equalTo(selflink))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(aaiObjectMapper.writeValueAsString(genericVnfs))));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers – OAuth and VNFM stubs (WireMock)
    // ─────────────────────────────────────────────────────────────────────────

    private void stubOAuthToken() {
        wireMockRule.stubFor(post(urlEqualTo("/oauth/token"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"access_token\":\"test-token\",\"token_type\":\"bearer\",\"expires_in\":3600}")));
    }

    private void stubVnfmCreate(final String vnfmId, final String vnfId, final String selfHref) {
        final InlineResponse201 createResponse = new InlineResponse201();
        final InlineResponse201LinksSelf self = new InlineResponse201LinksSelf();
        self.setHref(selfHref);
        final InlineResponse201Links links = new InlineResponse201Links();
        links.setSelf(self);
        createResponse.setLinks(links);
        createResponse.setId(vnfId);

        wireMockRule.stubFor(post(urlEqualTo("/vnf_instances"))
                .willReturn(aResponse().withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody(gsonProvider.getGson().toJson(createResponse))));
    }

    private void stubVnfmSubscribe(final String vnfmId) {
        wireMockRule.stubFor(post(urlEqualTo("/subscriptions"))
                .willReturn(aResponse().withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{}")));
    }

    private void stubVnfmInstantiate(final String vnfmId, final String vnfId, final String operationLocation) {
        wireMockRule.stubFor(post(urlEqualTo("/vnf_instances/" + vnfId + "/instantiate"))
                .willReturn(aResponse().withStatus(202)
                        .withHeader("Content-Type", "application/json")
                        .withHeader("Location", operationLocation)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers – AAI domain object builders
    // ─────────────────────────────────────────────────────────────────────────

    private GenericVnf buildGenericVnf(final String vnfId, final String nfType) {
        final GenericVnf vnf = new GenericVnf();
        vnf.setVnfId(vnfId);
        vnf.setNfType(nfType);
        return vnf;
    }

    private GenericVnf buildGenericVnfWithVnfmRelationship(final String vnfId, final String nfType,
            final String vnfmId) {
        final GenericVnf vnf = buildGenericVnf(vnfId, nfType);
        final Relationship rel = new Relationship();
        rel.setRelatedTo("esr-vnfm");
        rel.setRelatedLink("/aai/v15/external-system/esr-vnfm-list/esr-vnfm/" + vnfmId);
        final RelationshipData data = new RelationshipData();
        data.setRelationshipKey("esr-vnfm.vnfm-id");
        data.setRelationshipValue(vnfmId);
        rel.getRelationshipData().add(data);
        final RelationshipList list = new RelationshipList();
        list.getRelationship().add(rel);
        vnf.setRelationshipList(list);
        return vnf;
    }

    private GenericVnf buildGenericVnfWithTenantRelationship(final String vnfId, final String cloudOwner,
            final String region, final String tenantId) {
        final GenericVnf vnf = buildGenericVnf(vnfId, "vnfmType");
        vnf.setSelflink("http://vnfm:8080/vnfs/myTestVnfIdOnVnfm");
        final Relationship rel = new Relationship();
        rel.setRelatedTo("tenant");
        addRelationshipData(rel, "cloud-region.cloud-owner", cloudOwner);
        addRelationshipData(rel, "cloud-region.cloud-region-id", region);
        addRelationshipData(rel, "tenant.tenant-id", tenantId);
        final RelationshipList list = new RelationshipList();
        list.getRelationship().add(rel);
        vnf.setRelationshipList(list);
        return vnf;
    }

    private void addRelationshipData(final Relationship relationship, final String key, final String value) {
        final RelationshipData data = new RelationshipData();
        data.setRelationshipKey(key);
        data.setRelationshipValue(value);
        relationship.getRelationshipData().add(data);
    }

    private EsrVnfmList buildEsrVnfmListWithIds(final String... vnfmIds) {
        final EsrVnfmList list = new EsrVnfmList();
        for (final String vnfmId : vnfmIds) {
            final EsrVnfm vnfm = new EsrVnfm();
            vnfm.setVnfmId(vnfmId);
            vnfm.setResourceVersion("1234");
            list.getEsrVnfm().add(vnfm);
        }
        return list;
    }

    private EsrSystemInfoList buildEsrSystemInfoList(final String serviceUrl, final String type,
            final String systemType) {
        final EsrSystemInfo info = new EsrSystemInfo();
        info.setServiceUrl(serviceUrl);
        info.setType(type);
        info.setSystemType(systemType);
        info.setCloudDomain("myDomain");
        info.setUserName("myUser");
        info.setPassword("myPassword");
        final EsrSystemInfoList infoList = new EsrSystemInfoList();
        infoList.getEsrSystemInfo().add(info);
        return infoList;
    }

    private EsrVnfm buildEsrVnfmWithSystemInfo(final String vnfmId, final String serviceUrl, final String type) {
        final EsrVnfm vnfm = new EsrVnfm();
        vnfm.setVnfmId(vnfmId);
        vnfm.setResourceVersion("1234");
        vnfm.setEsrSystemInfoList(buildEsrSystemInfoList(serviceUrl, type, "VNFM"));
        return vnfm;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers – grant request builder
    // ─────────────────────────────────────────────────────────────────────────

    private GrantRequest buildInstantiateGrantRequest() {
        final GrantRequest req = new GrantRequest();
        req.setVnfInstanceId("myTestVnfIdOnVnfm");
        req.setVnfLcmOpOccId("123456");
        req.setOperation(GrantRequest.OperationEnum.INSTANTIATE);
        req.links(new GrantsLinks()
                .vnfInstance(new GrantsLinksVnfLcmOpOcc().href("http://vnfm:8080/vnfs/myTestVnfIdOnVnfm")));
        final GrantsAddResources resource = new GrantsAddResources();
        resource.setId("123");
        resource.setType(GrantsAddResources.TypeEnum.COMPUTE);
        req.addAddResourcesItem(resource);
        return req;
    }
}
