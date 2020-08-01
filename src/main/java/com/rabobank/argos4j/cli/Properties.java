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

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Getter
public class Properties {
    static final String ARGOS_SERVICE_BASE_URL = "ARGOS_SERVICE_BASE_URL";
    static final String CREDENTIALS_PASSPHRASE = "CREDENTIALS_PASSPHRASE";
    static final String CREDENTIALS_KEY_ID = "CREDENTIALS_KEY_ID";
    static final String SUPPLY_CHAIN_PATH = "SUPPLY_CHAIN_PATH";
    static final String SUPPLY_CHAIN_NAME = "SUPPLY_CHAIN_NAME";
    static final String ENV_WORKSPACE = "WORKSPACE";
    private static Properties INSTANCE;
    private final String argosServiceBaseUrl;
    private final String passPhrase;
    private final String keyId;
    private final String supplyChainName;
    private final List<String> path;
    private final String workspace;

    public static Properties getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new Properties();
        }
        return INSTANCE;
    }

    private Properties() {

        argosServiceBaseUrl = Optional.ofNullable(System.getenv(ARGOS_SERVICE_BASE_URL))
                .orElseThrow(() -> illegalArgumentException(ARGOS_SERVICE_BASE_URL));

        passPhrase = Optional.ofNullable(System.getenv(CREDENTIALS_PASSPHRASE))
                .orElseThrow(() -> illegalArgumentException(CREDENTIALS_PASSPHRASE));

        keyId = Optional.ofNullable(System.getenv(CREDENTIALS_KEY_ID))
                .orElseThrow(() -> illegalArgumentException(CREDENTIALS_KEY_ID));

        path = Optional.ofNullable(System.getenv(SUPPLY_CHAIN_PATH)).map(prop -> Arrays.asList(prop.split("\\.")))
                .orElseThrow(() -> illegalArgumentException(SUPPLY_CHAIN_PATH));

        supplyChainName = Optional.ofNullable(System.getenv(SUPPLY_CHAIN_NAME))
                .orElseThrow(() -> illegalArgumentException(SUPPLY_CHAIN_NAME));

        workspace = Optional.ofNullable(System.getenv(ENV_WORKSPACE))
                .orElseThrow(() -> illegalArgumentException(ENV_WORKSPACE));

    }

    private IllegalArgumentException illegalArgumentException(String environmentValue) {
        return new IllegalArgumentException("environment variable: " + environmentValue + " is required");
    }
}
