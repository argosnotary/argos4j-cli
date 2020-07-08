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

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static java.util.Arrays.asList;

@Getter
public class Properties {
    private static final String ARGOS_SERVICE_BASE_URL = "ARGOS_SERVICE_BASE_URL";
    private static final String CREDENTIALS_PASSPHRASE ="CREDENTIALS_PASSPHRASE" ;
    private static final String CREDENTIALS_KEY_ID = "CREDENTIALS_KEY_ID";
    private static final String SUPPLY_CHAIN_PATH = "SUPPLY_CHAIN_PATH";
    private static final String SUPPLY_CHAIN_NAME = "SUPPLY_CHAIN_NAME";
    private static final String WORKSPACE = "WORKSPACE";
    private static Properties INSTANCE;
    private  String argosServiceBaseUrl;
    private  String passPhrase;
    private  String keyId;
    private  String supplyChainName;
    private  List<String> path;
    private  String workspace;

    public static Properties getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new Properties();
        }
        return INSTANCE;
    }

    private Properties() {
        Optional.ofNullable(System.getenv(ARGOS_SERVICE_BASE_URL))
                .map(property-> argosServiceBaseUrl = property)
                .orElseThrow(illegalArgumentException(ARGOS_SERVICE_BASE_URL));

        Optional.ofNullable(System.getenv(CREDENTIALS_PASSPHRASE))
                .map(property-> passPhrase = property)
                .orElseThrow(illegalArgumentException(CREDENTIALS_PASSPHRASE));

        Optional.ofNullable(System.getenv(CREDENTIALS_KEY_ID))
                .map(property-> keyId = property)
                .orElseThrow(illegalArgumentException(CREDENTIALS_KEY_ID));

        Optional.ofNullable(System.getenv(SUPPLY_CHAIN_PATH))
                .map(property-> path = asList(property.split("\\.")))
                .orElseThrow(illegalArgumentException(SUPPLY_CHAIN_PATH));

        Optional.ofNullable(System.getenv(SUPPLY_CHAIN_NAME))
                .map(property-> supplyChainName = property)
                .orElseThrow(illegalArgumentException(SUPPLY_CHAIN_NAME));

        Optional.ofNullable(System.getenv(WORKSPACE))
                .map(property-> workspace = property)
                .orElseThrow(illegalArgumentException(WORKSPACE));
    }

    private Supplier<IllegalArgumentException> illegalArgumentException(String environmentValue) {
        return () -> new IllegalArgumentException("environment variable: " +  environmentValue + " is required"  );
    }
}
