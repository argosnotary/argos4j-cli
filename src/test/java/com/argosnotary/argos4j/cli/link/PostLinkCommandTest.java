/*
 * Copyright (C) 2020 Argos Notary Coöperatie UA
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
package com.argosnotary.argos4j.cli.link;


import com.argosnotary.argos4j.cli.ArgosNotaryCli;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.argosnotary.argos.argos4j.rest.api.model.RestArtifact;
import com.argosnotary.argos.argos4j.rest.api.model.RestKeyPair;
import com.argosnotary.argos.argos4j.rest.api.model.RestLinkMetaBlock;

import lombok.SneakyThrows;
import picocli.CommandLine;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.argosnotary.argos4j.cli.ArgosNotaryCli.ARGOS_SERVICE_BASE_URL;
import static com.argosnotary.argos4j.cli.ArgosNotaryCli.CREDENTIALS_KEY_ID;
import static com.argosnotary.argos4j.cli.ArgosNotaryCli.CREDENTIALS_PASSPHRASE;
import static com.argosnotary.argos4j.cli.ArgosNotaryCli.SUPPLY_CHAIN_NAME;
import static com.argosnotary.argos4j.cli.ArgosNotaryCli.SUPPLY_CHAIN_PATH;
import static com.argosnotary.argos4j.cli.EnvHelper.removeEntry;
import static com.argosnotary.argos4j.cli.EnvHelper.updateEnv;
import static com.argosnotary.argos4j.cli.link.PostLinkCommand.ENV_WORKSPACE;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.noContent;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.core.Is.is;

class PostLinkCommandTest {
    private WireMockServer wireMockServer;
    private ArgosNotaryCli argosNotaryCli;
    private CommandLine cli;
    private String restKeyPairRest;
    
    private RestArtifact artifact1;
    private RestArtifact artifact2;
    
    @TempDir
    File workspace;

    @BeforeEach
    void setUp() throws JsonParseException, JsonMappingException, IOException {
        RestKeyPair restKeyPair = new ObjectMapper().readValue(this.getClass().getResourceAsStream("/keypair.json"), RestKeyPair.class);
        restKeyPairRest = new ObjectMapper().writeValueAsString(restKeyPair);
        argosNotaryCli =  new ArgosNotaryCli();
        cli = new CommandLine(argosNotaryCli);
        wireMockServer = new WireMockServer(2500);
        wireMockServer.start();
        wireMockServer.stubFor(get(urlEqualTo("/api/supplychain?name=supplyChainName&path=root&path=child"))
                .willReturn(ok().withBody("{\"name\":\"supplyChainName\",\"id\":\"supplyChainId\",\"parentLabelId\":\"parentLabelId\"}")));
        wireMockServer.stubFor(post(urlEqualTo("/api/supplychain/supplyChainId/link")).willReturn(noContent()));
        wireMockServer.stubFor(get(urlEqualTo("/api/serviceaccount/me/activekey")).willReturn(ok().withBody(restKeyPairRest)));
        
        artifact1 = new RestArtifact();
        artifact1.setHash("ed886d735b3ed7db324ddfcfa30187c3fac4182f623a79e2b8c54ebf92a38a3a");
        artifact1.setUri("dir1/dir2/source2");
        artifact2 = new RestArtifact();
        artifact2.setHash("b94f6f125c79e3a5ffaa826f584c10d52ada669e6762051b826b55776d05aed2");
        artifact2.setUri("dir1/source");

    }
    @AfterEach
    public void teardown() {
        wireMockServer.stop();
        removeEntry(ARGOS_SERVICE_BASE_URL);
        removeEntry(CREDENTIALS_PASSPHRASE);
        removeEntry(CREDENTIALS_KEY_ID);
        removeEntry(SUPPLY_CHAIN_PATH);
        removeEntry(SUPPLY_CHAIN_NAME);
        removeEntry(ENV_WORKSPACE);
    }
    
    @Test
    void callWithPhasePreShouldStoreSingedLinkOnFileSystem() throws IOException {
        setEnv();
        updateEnv(ENV_WORKSPACE, workspace.getAbsolutePath());
        int exitCode = cli.execute(
                "postLink",
                "-a", "PRE",
                "-b", "src/test/resources/pre",
                "-p", "src/test/resources/pre",
                "-g", "layoutSegmentName", 
                "-s", "stepName", 
                "-r", "runId");
        assertThat(exitCode, is(0));
        String file = workspace.getAbsolutePath() + "/c76bad3017abf6049a82d89eb2b5cac1ebdc1b772c26775d5032520427b8a7b3-root-child-supplyChainName-runId-layoutSegmentName-stepName.link";
        assertThat(new File(file).exists(),is(true));
        String json = IOUtils.toString(Paths.get(file).toUri(), UTF_8);
        RestLinkMetaBlock restLinkMetaBlock  = new ObjectMapper().readValue(json, RestLinkMetaBlock.class);
        assertThat(restLinkMetaBlock.getLink().getMaterials(),hasSize(2));
        assertThat(restLinkMetaBlock.getLink().getMaterials(), contains(artifact1, artifact2));
    }

    @SneakyThrows
    @Test
    void callWithPhasePostShouldSendLinkAndRemoveFile() {
        setEnv();
        int exitCode = cli.execute(
                "postLink",
                "-a", "PRE",
                "-b", "src/test/resources/pre",
                "-p", "src/test/resources/pre", 
                "-g", "layoutSegmentName", 
                "-s", "stepName", 
                "-r", "runId",
                "-w", workspace.getAbsolutePath());
        assertThat(exitCode, is(0));
        String file = workspace.getAbsolutePath() + "/c76bad3017abf6049a82d89eb2b5cac1ebdc1b772c26775d5032520427b8a7b3-root-child-supplyChainName-runId-layoutSegmentName-stepName.link";
        assertThat(new File(file).exists(),is(true));
        String json = IOUtils.toString(Paths.get(file).toUri(), UTF_8);
        RestLinkMetaBlock restLinkMetaBlock  = new ObjectMapper().readValue(json, RestLinkMetaBlock.class);
        assertThat(restLinkMetaBlock.getLink().getMaterials(),hasSize(2));
        assertThat(restLinkMetaBlock.getLink().getMaterials(), contains(artifact1, artifact2));
        exitCode = cli.execute(
                "postLink",
                "-a", "POST",
                "-b", "src/test/resources/post",
                "-p", "src/test/resources/post",
                "-g", "layoutSegmentName", 
                "-s", "stepName", 
                "-r", "runId",
                "-w", workspace.getAbsolutePath());
        assertThat(exitCode, is(0));
        assertThat(new File(file).exists(),is(false));
    }
    
    @SneakyThrows
    @Test
    void callWithFileAndPhasePostShouldSendLinkAndRemoveFile() {
        int exitCode = cli.execute(
                "-f", "./src/test/resources/link-argos-settings.json", 
                "postLink",
                "-a", "PRE",
                "-b", "src/test/resources/pre",
                "-p", "src/test/resources/pre", 
                "-g", "layoutSegmentName", 
                "-s", "stepName", 
                "-r", "runId",
                "-q", "runId",
                "-w", workspace.getAbsolutePath());
        assertThat(exitCode, is(0));
        String file = workspace.getAbsolutePath() + "/runId-root-child-supplyChainName-runId-layoutSegmentName-stepName.link";
        assertThat(new File(file).exists(),is(true));
        String json = IOUtils.toString(Paths.get(file).toUri(), UTF_8);
        RestLinkMetaBlock restLinkMetaBlock  = new ObjectMapper().readValue(json, RestLinkMetaBlock.class);
        assertThat(restLinkMetaBlock.getLink().getMaterials(),hasSize(2));
        assertThat(restLinkMetaBlock.getLink().getMaterials(), contains(artifact1, artifact2));
        exitCode = cli.execute(
                "-f", "./src/test/resources/link-argos-settings.json", 
                "postLink",
                "-a", "POST",
                "-b", "src/test/resources/post",
                "-p", "src/test/resources/post", 
                "-g", "layoutSegmentName", 
                "-s", "stepName", 
                "-r", "runId",
                "-q", "runId",
                "-w", workspace.getAbsolutePath());
        assertThat(exitCode, is(0));
        assertThat(new File(file).exists(),is(false));
    }
    
    @Test
    void throwNoWorkspace() {
        setEnv();
        StringWriter sw = new StringWriter();
        cli.setErr(new PrintWriter(sw));
        
        cli.execute(
                    "postLink",
                    "-a", "PRE",
                    "-g", "layoutSegmentName", 
                    "-s", "stepName", 
                    "-r", "runId");
        assertThat(sw.toString(), startsWith("java.lang.IllegalArgumentException: variable: WORKSPACE is required"));
    }
    
    private void setEnv() {
        updateEnv(ARGOS_SERVICE_BASE_URL, "http://localhost:2500/api");
        updateEnv(CREDENTIALS_PASSPHRASE, "test");
        updateEnv(CREDENTIALS_KEY_ID, "c76bad3017abf6049a82d89eb2b5cac1ebdc1b772c26775d5032520427b8a7b3");
        updateEnv(SUPPLY_CHAIN_PATH, "root.child");
        updateEnv(SUPPLY_CHAIN_NAME, "supplyChainName");
        
    }
}