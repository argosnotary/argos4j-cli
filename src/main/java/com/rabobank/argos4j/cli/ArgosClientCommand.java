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

import com.rabobank.argos.argos4j.Argos4jSettings;
import com.rabobank.argos.argos4j.FileCollector;
import com.rabobank.argos.argos4j.LocalFileCollector;
import com.rabobank.argos.argos4j.internal.Argos4JSigner;
import com.rabobank.argos.argos4j.internal.ArgosServiceClient;
import com.rabobank.argos.argos4j.internal.ArtifactCollectorFactory;
import com.rabobank.argos.domain.Signature;
import com.rabobank.argos.domain.link.Artifact;
import com.rabobank.argos.domain.link.Link;
import com.rabobank.argos.domain.link.LinkMetaBlock;
import com.rabobank.argos.domain.signing.JsonSigningSerializer;
import lombok.extern.slf4j.Slf4j;
import org.apache.log4j.BasicConfigurator;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

@Command(
        name = "argos-collector",
        description = "collect artifacts from local workspace and send to argos service"
)
@Slf4j
public class ArgosClientCommand implements Callable<Boolean> {

    private static final String PRE = "pre";
    private static final String POST = "post";
    private Properties properties = Properties.getInstance();

    @Option(names = {"-r", "--runId"}, description = "unique runid of the pipeline run", required = true)
    private String runId;
    @Option(names = {"-st", "--step"}, description = "the stepname of the wrapped pipeline step", required = true)
    private String stepName;
    @Option(names = {"-ls", "--segment"}, description = "the segmentname of the wrapped pipeline step", required = true)
    private String layoutSegmentName;
    @Option(names = {"-p", "--phase"}, description = PRE + "," + POST, required = true)
    private String phase = PRE;

    private Argos4jSettings argos4jSettings;

    public static void main(String[] args) {
        BasicConfigurator.configure();
        new CommandLine(new ArgosClientCommand()).execute(args);
    }

    public Boolean call() throws Exception {
        argos4jSettings = Argos4jSettings.builder()
                .argosServerBaseUrl(properties.getArgosServiceBaseUrl())
                .path(properties.getPath())
                .signingKeyId(properties.getKeyId())
                .supplyChainName(properties.getSupplyChainName())
                .build();
        if (PRE.equals(phase)) {
            createMaterials();
        } else {
            List<Artifact> products = createProducts();
            List<Artifact> materials = LinkRegistry.getLink(runId, layoutSegmentName, stepName)
                    .map(Link::getMaterials)
                    .orElse(Collections.emptyList());
            sendLinkToArgosService(materials, products);
            LinkRegistry.removeLink(runId, layoutSegmentName, stepName);
        }
        return true;
    }

    private void sendLinkToArgosService(List<Artifact> materials, List<Artifact> products) {
        log.info("posting link to argos service ");
        log.debug("used materials " + materials);
        log.debug("used products " + products);
        Link link = Link.builder().runId(runId)
                .materials(materials)
                .products(products)
                .layoutSegmentName(layoutSegmentName)
                .stepName(stepName).build();
        ArgosServiceClient argosServiceClient = new ArgosServiceClient(argos4jSettings, properties.getPassPhrase().toCharArray());
        Signature signature = new Argos4JSigner().sign(argosServiceClient.getKeyPair(), properties.getPassPhrase().toCharArray(), new JsonSigningSerializer().serialize(link));
        argosServiceClient.uploadLinkMetaBlockToService(LinkMetaBlock.builder().link(link).signature(signature).build());
    }

    private List<Artifact> createProducts() {
        FileCollector collector = createFileCollector();
        List<Artifact> artifacts = ArtifactCollectorFactory.build(collector).collect();
        log.info("created products ");
        return artifacts;
    }


    private void createMaterials() throws URISyntaxException, IOException {
        FileCollector collector = createFileCollector();
        List<Artifact> artifacts = ArtifactCollectorFactory.build(collector).collect();
        log.info("created materials ");
        Link link = Link.builder()
                .runId(runId)
                .materials(artifacts)
                .layoutSegmentName(layoutSegmentName)
                .stepName(stepName).build();
        LinkRegistry.storeLink(runId, layoutSegmentName, stepName, link);
    }

    private FileCollector createFileCollector() {
        Path path = Path.of(properties.getWorkspace());
        return LocalFileCollector.builder()
                .basePath(path)
                .path(path)
                .build();
    }
}