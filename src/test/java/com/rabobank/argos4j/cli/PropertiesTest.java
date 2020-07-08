package com.rabobank.argos4j.cli;

import org.junit.jupiter.api.Test;

import static com.rabobank.argos4j.cli.EnvHelper.updateEnv;
import static com.rabobank.argos4j.cli.Properties.ARGOS_SERVICE_BASE_URL;
import static com.rabobank.argos4j.cli.Properties.CREDENTIALS_KEY_ID;
import static com.rabobank.argos4j.cli.Properties.CREDENTIALS_PASSPHRASE;
import static com.rabobank.argos4j.cli.Properties.SUPPLY_CHAIN_NAME;
import static com.rabobank.argos4j.cli.Properties.SUPPLY_CHAIN_PATH;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PropertiesTest {

    @Test
    void getInstanceShouldThrowIllegalArgumentException() {
        IllegalArgumentException exception = null;
        exception = assertThrows(IllegalArgumentException.class, () -> Properties.getInstance());
        assertThat(exception.getMessage(), is("environment variable: ARGOS_SERVICE_BASE_URL is required"));
        updateEnv(ARGOS_SERVICE_BASE_URL, "http://localhost:2500/api");
        exception = assertThrows(IllegalArgumentException.class, () -> Properties.getInstance());
        assertThat(exception.getMessage(), is("environment variable: CREDENTIALS_PASSPHRASE is required"));
        updateEnv(CREDENTIALS_PASSPHRASE, "gBM1Q4sc3kh05E");
        exception = assertThrows(IllegalArgumentException.class, () -> Properties.getInstance());
        assertThat(exception.getMessage(), is("environment variable: CREDENTIALS_KEY_ID is required"));
        updateEnv(CREDENTIALS_KEY_ID, "c76bad3017abf6049a82d89eb2b5cac1ebdc1b772c26775d5032520427b8a7b3");
        exception = assertThrows(IllegalArgumentException.class, () -> Properties.getInstance());
        assertThat(exception.getMessage(), is("environment variable: SUPPLY_CHAIN_PATH is required"));
        updateEnv(SUPPLY_CHAIN_PATH, "root.child");
        exception = assertThrows(IllegalArgumentException.class, () -> Properties.getInstance());
        assertThat(exception.getMessage(), is("environment variable: SUPPLY_CHAIN_NAME is required"));
        updateEnv(SUPPLY_CHAIN_NAME, "name");
        exception = assertThrows(IllegalArgumentException.class, () -> Properties.getInstance());
        assertThat(exception.getMessage(), is("environment variable: WORKSPACE is required"));

    }
}