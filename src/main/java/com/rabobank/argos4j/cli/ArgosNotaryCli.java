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

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;

import org.apache.log4j.BasicConfigurator;

import com.rabobank.argos.argos4j.Argos4jSettings;
import com.rabobank.argos4j.cli.link.PostLinkCommand;
import com.rabobank.argos4j.cli.release.ReleaseCommand;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "argos-cli", synopsisSubcommandLabel = "(postLink | release)",
    subcommands = {PostLinkCommand.class, ReleaseCommand.class}, mixinStandardHelpOptions = true)
public class ArgosNotaryCli {
    public static final String ARGOS_SERVICE_BASE_URL = "ARGOS_SERVICE_BASE_URL";
    public static final String CREDENTIALS_PASSPHRASE = "CREDENTIALS_PASSPHRASE";
    public static final String CREDENTIALS_KEY_ID = "CREDENTIALS_KEY_ID";
    public static final String SUPPLY_CHAIN_PATH = "SUPPLY_CHAIN_PATH";
    public static final String SUPPLY_CHAIN_NAME = "SUPPLY_CHAIN_NAME";

    @Option(names = {"-f", "--config-file"}, description = "json file with Argos Notary configuration")
    private String configFile;
    
    public static void main(String[] args) {
        BasicConfigurator.configure();
        int exitCode = new CommandLine(new ArgosNotaryCli()).execute(args);
        System.exit(exitCode);
    }
    
    public Argos4jSettings createSettings() {
        Argos4jSettings settings;
        if (configFile != null) {
            settings = Argos4jSettings.readSettings(Paths.get(configFile));
        } else {
            settings = Argos4jSettings.builder().build();
        }
        
        Optional.ofNullable(System.getenv(ARGOS_SERVICE_BASE_URL))
            .ifPresent(settings::setArgosServerBaseUrl);

        Optional.ofNullable(System.getenv(CREDENTIALS_PASSPHRASE))
            .ifPresent(settings::setKeyPassphrase);

        Optional.ofNullable(System.getenv(CREDENTIALS_KEY_ID))
            .ifPresent(settings::setKeyId);

        Optional.ofNullable(System.getenv(SUPPLY_CHAIN_PATH))
            .ifPresent(e -> settings.setPath(Arrays.asList(e.split("\\."))));

        Optional.ofNullable(System.getenv(SUPPLY_CHAIN_NAME))
            .ifPresent(settings::setSupplyChainName);
        checkInput(settings);
        return settings;
        
    }
    
    private static void checkInput(Argos4jSettings argos4jSettings) {
        if (argos4jSettings.getArgosServerBaseUrl() == null) {
            illegalArgumentException(ARGOS_SERVICE_BASE_URL);
        }

        if (argos4jSettings.getKeyPassphrase() == null) {
            illegalArgumentException(CREDENTIALS_PASSPHRASE);
        }

        if (argos4jSettings.getKeyId() == null) {
            illegalArgumentException(CREDENTIALS_KEY_ID);
        }

        if (argos4jSettings.getPath() == null) {
            illegalArgumentException(SUPPLY_CHAIN_PATH);
        }

        if (argos4jSettings.getSupplyChainName() == null) {
            illegalArgumentException(SUPPLY_CHAIN_NAME);
        }        
    }

    public static IllegalArgumentException illegalArgumentException(String value) {
        throw new IllegalArgumentException("variable: " + value + " is required");
    }

}
