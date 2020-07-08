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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static com.rabobank.argos4j.cli.EnvHelper.removeEntry;
import static com.rabobank.argos4j.cli.EnvHelper.updateEnv;
import static com.rabobank.argos4j.cli.Properties.ARGOS_SERVICE_BASE_URL;
import static com.rabobank.argos4j.cli.Properties.CREDENTIALS_KEY_ID;
import static com.rabobank.argos4j.cli.Properties.CREDENTIALS_PASSPHRASE;
import static com.rabobank.argos4j.cli.Properties.ENV_WORKSPACE;
import static com.rabobank.argos4j.cli.Properties.SUPPLY_CHAIN_NAME;
import static com.rabobank.argos4j.cli.Properties.SUPPLY_CHAIN_PATH;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PropertiesTest {
    @BeforeEach
    void setup() {
        removeEntry(ARGOS_SERVICE_BASE_URL);
        removeEntry(CREDENTIALS_PASSPHRASE);
        removeEntry(CREDENTIALS_KEY_ID);
        removeEntry(SUPPLY_CHAIN_PATH);
        removeEntry(SUPPLY_CHAIN_NAME);
        removeEntry(ENV_WORKSPACE);
    }

    private static Properties createProperties() throws NoSuchMethodException, ClassNotFoundException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Class clazz = Class.forName("com.rabobank.argos4j.cli.Properties");
        Constructor<Properties> con = clazz.getDeclaredConstructor();
        con.setAccessible(true);
        return con.newInstance(null);
    }

    @Test
    void getInstanceShouldThrowIllegalArgumentException() {
        assertThrows(InvocationTargetException.class, () -> createProperties());
        updateEnv(ARGOS_SERVICE_BASE_URL, "http://localhost:2500/api");
        assertThrows(InvocationTargetException.class, () -> createProperties());
        updateEnv(CREDENTIALS_PASSPHRASE, "gBM1Q4sc3kh05E");
        assertThrows(InvocationTargetException.class, () -> createProperties());
        updateEnv(CREDENTIALS_KEY_ID, "c76bad3017abf6049a82d89eb2b5cac1ebdc1b772c26775d5032520427b8a7b3");
        assertThrows(InvocationTargetException.class, () -> createProperties());
        updateEnv(SUPPLY_CHAIN_PATH, "root.child");
        assertThrows(InvocationTargetException.class, () -> createProperties());
        updateEnv(SUPPLY_CHAIN_NAME, "name");
        assertThrows(InvocationTargetException.class, () -> createProperties());

    }
}