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

import com.rabobank.argos.argos4j.Argos4j;
import com.rabobank.argos.argos4j.Argos4jSettings;
import com.rabobank.argos.argos4j.FileCollector;
import com.rabobank.argos.argos4j.LinkBuilder;
import com.rabobank.argos.argos4j.LinkBuilderSettings;
import com.rabobank.argos.argos4j.LocalFileCollector;
import com.rabobank.argos.domain.link.Artifact;
import com.rabobank.argos.domain.link.Link;

import lombok.extern.slf4j.Slf4j;
import org.apache.log4j.BasicConfigurator;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

@Command(
        name = "argos-collector",
        description = "collect artifacts from local workspace and send to argos service"
)
@Slf4j
public class PostLinkCommand implements Callable<Boolean> {
	private static final String EXCLUDE_PATTERN = "{**.git/**,**.git\\**,**.link}";

    private static final String PRE = "pre";
    private static final String POST = "post";
    private final Properties properties = Properties.getInstance();

    @Option(names = {"-r", "--runId"}, description = "unique runid of the pipeline run", required = true)
    private String runId;
    @Option(names = {"-st", "--step"}, description = "the stepname of the wrapped pipeline step", required = true)
    private String stepName;
    @Option(names = {"-ls", "--segment"}, description = "the segmentname of the wrapped pipeline step", required = true)
    private String layoutSegmentName;
    @Option(names = {"-p", "--phase"}, description = PRE + "," + POST)
    private String phase = PRE;

    private LinkBuilder linkBuilder;
    private LinkFileHandler linkFileHandler;

    public static void main(String[] args) {
        BasicConfigurator.configure();
        new CommandLine(new PostLinkCommand()).execute(args);
    }

    public Boolean call() throws Exception {
    	Argos4jSettings argos4jSettings = Argos4jSettings.builder()
                .argosServerBaseUrl(properties.getArgosServiceBaseUrl())
                .path(properties.getPath())
                .signingKeyId(properties.getKeyId())
                .supplyChainName(properties.getSupplyChainName())
                .build();
    	LinkBuilderSettings linkBuilderSettings = LinkBuilderSettings.builder()
        		.layoutSegmentName(layoutSegmentName)
        		.stepName(stepName)
        		.runId(runId)
        		.build();
    	Argos4j argos4j = new Argos4j(argos4jSettings);
        linkBuilder = argos4j.getLinkBuilder(linkBuilderSettings);
        linkFileHandler = new LinkFileHandler(argos4jSettings, linkBuilderSettings);
        if (PRE.equals(phase)) {
            createMaterials();
        } else {
        	collectProductsAndSendLinkToArgosService();
        	linkFileHandler.removeLink();
        }
        return true;
    }

    private void collectProductsAndSendLinkToArgosService() throws IOException, GeneralSecurityException {
        Optional<Link> optionalLink = linkFileHandler.getLink();
    	if (optionalLink.isPresent()) {
    	    List<Artifact> materials = optionalLink.get().getMaterials();
    	    log.info("posting link to argos service ");
    	    linkBuilder.addMaterials(materials);
    	}
        FileCollector collector = createFileCollector();
        linkBuilder.collectMaterials(collector);
        linkBuilder.store(properties.getPassPhrase().toCharArray());
    }

    private void createMaterials() throws IOException {
        FileCollector collector = createFileCollector();
        linkBuilder.collectMaterials(collector);
        linkFileHandler.storeLink(linkBuilder.create(properties.getPassPhrase().toCharArray()));
    }

    private FileCollector createFileCollector() {
        Path path = Paths.get(properties.getWorkspace());
        return LocalFileCollector.builder()
                .basePath(path)
                .path(path)
                .excludePatterns(EXCLUDE_PATTERN)
                .build();
    }
}