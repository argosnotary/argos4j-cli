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
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.Getter;

import java.util.List;

import static java.util.Arrays.asList;

@Getter
public class Properties {
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
        Config conf = ConfigFactory.load();
        if (System.getenv("ARGOS_SERVICE_BASE_URL") == null) {
            argosServiceBaseUrl = conf.getString("argos-service.rest-api.base-url");
        } else {
            argosServiceBaseUrl = System.getenv("ARGOS_SERVICE_BASE_URL");
        }
        if (System.getenv("CREDENTIALS_PASSPHRASE") == null) {
            passPhrase = conf.getString("supplychain.credentials.passphrase");
        } else {
            passPhrase = System.getenv("CREDENTIALS_PASSPHRASE");
        }
        if (System.getenv("CREDENTIALS_KEY_ID") == null) {
            keyId = conf.getString("supplychain.credentials.keyid");
        } else {
            keyId = System.getenv("CREDENTIALS_KEY_ID");
        }
        if (System.getenv("SUPPLY_CHAIN_PATH") == null) {
            path = asList(conf.getString("supplychain.path").split("\\."));
        } else {
            path = asList(System.getenv("SUPPLY_CHAIN_PATH").split("\\."));
        }
        if (System.getenv("SUPPLY_CHAIN_NAME") == null) {
            supplyChainName = conf.getString("supplychain.name");
        } else {
            supplyChainName = System.getenv("SUPPLY_CHAIN_NAME");
        }
        if (System.getenv("WORKSPACE") == null) {
            workspace = conf.getString("workspace");
        } else {
            workspace = System.getenv("WORKSPACE");
        }
    }
}
