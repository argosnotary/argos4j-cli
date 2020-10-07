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
import com.argosnotary.argos.argos4j.Argos4j;
import com.argosnotary.argos.argos4j.Argos4jSettings;
import com.argosnotary.argos.argos4j.FileCollector;
import com.argosnotary.argos.argos4j.LinkBuilder;
import com.argosnotary.argos.argos4j.LinkBuilderSettings;
import com.argosnotary.argos.argos4j.LocalFileCollector;
import com.argosnotary.argos.domain.link.Artifact;
import com.argosnotary.argos.domain.link.Link;

import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

import java.io.IOException;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

@Command(
        name = "postLink",
        aliases = {"link"},
        helpCommand = true,
        description = "collect artifacts from local workspace and send to argos service"
)
@Slf4j
public class PostLinkCommand implements Callable<Integer> {
    private Argos4jSettings settings;
	private static final String EXCLUDE_PATTERN = "{**.git/**,**.git\\**,**.link}";
    public static final String ENV_WORKSPACE = "WORKSPACE";

    @Option(names = {"-r", "--runId"}, description = "unique runid of the pipeline run", required = true)
    private String runId;
    @Option(names = {"-s", "--step"}, description = "the stepname of the wrapped pipeline step", required = true)
    private String stepName;
    @Option(names = {"-g", "--segment"}, description = "the segmentname of the wrapped pipeline step", required = true)
    private String layoutSegmentName;
    @Option(names = {"-w", "--workspace"}, description = "the workspace of the inner command")
    private String workspace;
    @Option(names = {"-q", "--qualifier"}, description = "first qualifier of temp link file")
    private String qualifier;
    @Option(names = {"-b", "--basepath"}, description = "base path from which to report artifact uri's")
    private String basePath;
    @Option(names = {"-p", "--path"}, description = "path to file or directory to collect")
    private String path;
    @Option(names = {"-a", "--phase"}, description = "${COMPLETION-CANDIDATES}", required = true)
    private Phase phase;
    
    private LinkBuilder linkBuilder;
    private LinkFileHandler linkFileHandler;
    
    @ParentCommand
    private ArgosNotaryCli cli;

    public Integer call() throws Exception {
        settings = cli.createSettings();
        workspace = getWorkspace();
    	LinkBuilderSettings linkBuilderSettings = LinkBuilderSettings.builder()
        		.layoutSegmentName(layoutSegmentName)
        		.stepName(stepName)
        		.runId(runId)
        		.build();
    	Argos4j argos4j = new Argos4j(settings);
        linkBuilder = argos4j.getLinkBuilder(linkBuilderSettings);
        linkFileHandler = new LinkFileHandler(settings, linkBuilderSettings);
        if (Phase.PRE == phase) {
            createMaterials();
        } else {
        	collectProductsAndSendLinkToArgosService();
        	linkFileHandler.removeLink(workspace, qualifier);
        }
        return 0;
    }
    
    private String getWorkspace() {
        Optional<String> opt = Optional.ofNullable(workspace);
        if (opt.isPresent()) {
            return opt.get();
        } else {
            return Optional.ofNullable(System.getenv(ENV_WORKSPACE))
                    .orElseThrow(() -> ArgosNotaryCli.illegalArgumentException(ENV_WORKSPACE));
        }
    }

    private void collectProductsAndSendLinkToArgosService() throws IOException, GeneralSecurityException {
        Optional<Link> optionalLink = linkFileHandler.getLink(workspace, qualifier);
    	if (optionalLink.isPresent()) {
    	    List<Artifact> materials = optionalLink.get().getMaterials();
    	    log.info("posting link to argos service ");
    	    linkBuilder.addMaterials(materials);
    	}
        FileCollector collector = createFileCollector();
        linkBuilder.collectProducts(collector);
        linkBuilder.store(settings.getKeyPassphrase().toCharArray());
    }

    private void createMaterials() throws IOException {
        FileCollector collector = createFileCollector();
        linkBuilder.collectMaterials(collector);
        linkFileHandler.storeLink(linkBuilder
                .create(settings.getKeyPassphrase().toCharArray()), workspace, qualifier);
    }

    private FileCollector createFileCollector() {
        return LocalFileCollector.builder()
                .basePath(Paths.get(basePath))
                .path(Paths.get(path))
                .excludePatterns(EXCLUDE_PATTERN)
                .build();
    }
    
    enum Phase {
        PRE, POST
        
    }
}