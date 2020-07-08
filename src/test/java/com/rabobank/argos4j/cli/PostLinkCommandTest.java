/*
 * Copyright (C) 2019 - 2020 Rabobank Nederland
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.rabobank.argos4j.cli;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.rabobank.argos.argos4j.rest.api.model.RestKeyPair;
import com.rabobank.argos.argos4j.rest.api.model.RestLinkMetaBlock;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.noContent;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.apache.commons.lang3.reflect.FieldUtils.writeField;

class PostLinkCommandTest {
    private WireMockServer wireMockServer;
    private PostLinkCommand postLinkCommand;
    private String restKeyPairRest;
    private Properties properties = Properties.getInstance();
    private static final String ARGOS_SERVICE_BASE_URL = "ARGOS_SERVICE_BASE_URL";
    private static final String CREDENTIALS_PASSPHRASE ="CREDENTIALS_PASSPHRASE" ;
    private static final String CREDENTIALS_KEY_ID = "CREDENTIALS_KEY_ID";
    private static final String SUPPLY_CHAIN_PATH = "SUPPLY_CHAIN_PATH";
    private static final String SUPPLY_CHAIN_NAME = "SUPPLY_CHAIN_NAME";
    private static final String WORKSPACE = "WORKSPACE";

    @BeforeAll
    static void initialize() throws ReflectiveOperationException {
        updateEnv(ARGOS_SERVICE_BASE_URL,"http://localhost:2500/api");
        updateEnv(CREDENTIALS_PASSPHRASE,"gBM1Q4sc3kh05E");
        updateEnv(CREDENTIALS_KEY_ID,"c76bad3017abf6049a82d89eb2b5cac1ebdc1b772c26775d5032520427b8a7b3");
        updateEnv(SUPPLY_CHAIN_PATH,"root.child");
        updateEnv(SUPPLY_CHAIN_NAME,"supplyChainName");
        updateEnv(WORKSPACE,"./workspace");
    }

    @SuppressWarnings({ "unchecked" })
    static void updateEnv(String name, String val) throws ReflectiveOperationException {
        Map<String, String> env = System.getenv();
        Field field = env.getClass().getDeclaredField("m");
        field.setAccessible(true);
        ((Map<String, String>) field.get(env)).put(name, val);
    }

    @SneakyThrows
    @BeforeEach
    void setUp() {

        RestKeyPair restKeyPair = new ObjectMapper().readValue(this.getClass().getResourceAsStream("/keypair.json"), RestKeyPair.class);
        restKeyPairRest = new ObjectMapper().writeValueAsString(restKeyPair);
        postLinkCommand = new PostLinkCommand();
        writeField(postLinkCommand, "layoutSegmentName", "layoutSegmentName", true);
        writeField(postLinkCommand, "stepName", "stepName", true);
        writeField(postLinkCommand, "runId", "runId", true);
        wireMockServer = new WireMockServer(2500);
        wireMockServer.start();
        wireMockServer.stubFor(get(urlEqualTo("/api/supplychain?name=supplyChainName&path=root&path=child"))
                .willReturn(ok().withBody("{\"name\":\"supplyChainName\",\"id\":\"supplyChainId\",\"parentLabelId\":\"parentLabelId\"}")));
        wireMockServer.stubFor(post(urlEqualTo("/api/supplychain/supplyChainId/link")).willReturn(noContent()));
        wireMockServer.stubFor(get(urlEqualTo("/api/serviceaccount/me/activekey")).willReturn(ok().withBody(restKeyPairRest)));

    }
    @AfterEach
    public void teardown() {
        wireMockServer.stop();
    }
    @SneakyThrows
    @Test
    void callWithPhasePreShouldStoreSingedLinkOnFileSystem() {
        postLinkCommand.call();
        Path basePath = Path.of(properties.getWorkspace());
        String file = basePath.toString()+"/c76bad3017abf6049a82d89eb2b5cac1ebdc1b772c26775d5032520427b8a7b3-root-child-supplyChainName-runId-layoutSegmentName-stepName.link";
        assertThat(new File(file).exists(),is(true));
        String json = IOUtils.toString(Paths.get(file).toUri(), UTF_8);
        RestLinkMetaBlock restLinkMetaBlock  = new ObjectMapper().readValue(json, RestLinkMetaBlock.class);
        assertThat(restLinkMetaBlock.getLink().getMaterials(),hasSize(1));
    }

    @SneakyThrows
    @Test
    void callWithPhasePostShouldSendLinkAndRemoveFile() {
        postLinkCommand.call();
        postLinkCommand = new PostLinkCommand();
        writeField(postLinkCommand, "layoutSegmentName", "layoutSegmentName", true);
        writeField(postLinkCommand, "stepName", "stepName", true);
        writeField(postLinkCommand, "runId", "runId", true);
        writeField(postLinkCommand, "phase", "post", true);
        postLinkCommand.call();
        Path basePath = Path.of(properties.getWorkspace());
        String file = basePath.toString()+"/c76bad3017abf6049a82d89eb2b5cac1ebdc1b772c26775d5032520427b8a7b3-root-child-supplyChainName-runId-layoutSegmentName-stepName.link";
        assertThat(new File(file).exists(),is(false));
    }
}