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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import com.argosnotary.argos4j.cli.ArgosNotaryCli;
import com.argosnotary.argos.argos4j.Argos4j;
import com.argosnotary.argos.argos4j.Argos4jSettings;
import com.argosnotary.argos.argos4j.ReleaseBuilder;
import com.argosnotary.argos.domain.release.ReleaseResult;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

@Command(
        name = "release",
        description = "create a release for the end products of a supply chain"
)
public class ReleaseCommand implements Callable<Integer> {
    private Argos4jSettings settings;
    
    @ParentCommand
    private ArgosNotaryCli cli;
    
    @Option(names = {"-c", "--config-string"}, 
            parameterConsumer = ReleaseConfigMapConsumer.class, 
            description = "a semicolon and comma delimeted string of config map name=name,key=value,key2=value2;name=name2...")
    private Map<String, Map<String,String>> configMap = new HashMap<>();

    @Override
    public Integer call() throws Exception {
        settings = cli.createSettings();
        settings.enrichReleaseCollectors(configMap);
        checkInput();
        Argos4j argos4j = new Argos4j(settings);
        ReleaseBuilder releaseBuilder = argos4j.getReleaseBuilder();
        settings.getReleaseCollectors().forEach(r -> releaseBuilder.addFileCollector(r.getCollector()));
        ReleaseResult result = releaseBuilder.release(settings.getKeyPassphrase().toCharArray());
        if (!result.isReleaseIsValid()) {
            System.out.printf("ERROR: Release is not valid");
            return 1;
        } else {
            System.out.printf("Created release with data: \n"
                    + "Document id: [%s]\nSupply Chain path: [%s]\n"
                    + "Release date: [%s]\n"
                    + "Artifacts: [%s]", 
                    result.getReleaseDossierMetaData().getDocumentId(), 
                    result.getReleaseDossierMetaData().getSupplyChainPath(), 
                    result.getReleaseDossierMetaData().getReleaseDate(),
                    result.getReleaseDossierMetaData().getReleaseArtifacts());
            return 0;
        }
    }
    
    private void checkInput() {
        if (settings.getReleaseCollectors() == null || settings.getReleaseCollectors().isEmpty()) {
            ArgosNotaryCli.illegalArgumentException("Release Collectors");
        }
    }

}
