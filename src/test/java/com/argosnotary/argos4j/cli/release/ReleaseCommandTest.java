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
package com.argosnotary.argos4j.cli.release;


import com.argosnotary.argos4j.cli.ArgosNotaryCli;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.argosnotary.argos.argos4j.rest.api.model.RestKeyPair;

import lombok.SneakyThrows;
import picocli.CommandLine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

class ReleaseCommandTest {
    private WireMockServer wireMockServer;
    private ArgosNotaryCli argosNotaryCli;
    private CommandLine cli;
    private String restKeyPairRest;
    String currentDir;

    @SneakyThrows
    @BeforeEach
    void setUp() {
        RestKeyPair restKeyPair = new ObjectMapper().readValue(this.getClass().getResourceAsStream("/keypair.json"), RestKeyPair.class);
        restKeyPairRest = new ObjectMapper().writeValueAsString(restKeyPair);
        argosNotaryCli =  new ArgosNotaryCli();
        cli = new CommandLine(argosNotaryCli);
        wireMockServer = new WireMockServer(options().port(2510).notifier(new ConsoleNotifier(false)));
        wireMockServer.start();
        wireMockServer.stubFor(get(urlEqualTo("/api/supplychain?name=supplyChainName&path=root&path=child"))
                .willReturn(ok().withBody("{\"name\":\"supplyChainName\",\"id\":\"supplyChainId\",\"parentLabelId\":\"parentLabelId\"}")));
        wireMockServer.stubFor(post(urlEqualTo("/api/supplychain/supplyChainId/release"))
                .willReturn(ok().withBody("{\n" + 
                        "    \"releaseIsValid\": true,\n" + 
                        "    \"releaseDossierMetaData\": {\n" + 
                        "        \"documentId\": \"documentId\",\n" + 
                        "        \"releaseDate\": \"2020-07-30T18:35:24.00Z\",\n" + 
                        "        \"supplyChainPath\": \"com.argosnotary.argos\",\n" + 
                        "        \"releaseArtifacts\": [\n" + 
                        "            [\n" + 
                        "                \"hash1\",\n" + 
                        "                \"hash2\"\n" + 
                        "            ],\n" + 
                        "            [\n" + 
                        "                \"hash3\",\n" + 
                        "                \"hash4\"\n" + 
                        "            ]\n" + 
                        "        ]\n" + 
                        "    }\n" + 
                        "}")));
        wireMockServer.stubFor(get(urlEqualTo("/api/serviceaccount/me/activekey")).willReturn(ok().withBody(restKeyPairRest)));
        currentDir = new java.io.File( "." ).getCanonicalPath();
    }
    
    @Test
    void callWithFileAndRelease() {
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream oldErr = System.err;
        PrintStream oldOut = System.out;
        System.setErr(new PrintStream(err));           // setup
        System.setOut(new PrintStream(out));
        int exitCode;
        try {
            System.setErr(new PrintStream(err));           // setup
            System.setOut(new PrintStream(out));
            exitCode = cli.execute(
                "-f", "./src/test/resources/release-argos-settings.json", 
                "release", 
                "-c", "name=local-collector,path="+currentDir+"/src/main/resources/log4j.properties,basePath="+currentDir);
        
        } finally {
            System.setErr(oldErr);                         // teardown
            System.setOut(oldOut);
        }
        assertThat(out.toString(), is("Created release with data: \nDocument id: [documentId]\nSupply Chain path: [com.argosnotary.argos]\nRelease date: [2020-07-30T18:35:24Z]\nArtifacts: [[[hash1, hash2], [hash3, hash4]]]")); // verify
        assertThat(err.toString(), is(""));
        assertThat(exitCode, is(0));
    }
}