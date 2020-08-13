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
package com.rabobank.argos4j.cli.release;


import picocli.CommandLine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

class ReleaseConfigMapConsumerTest {
    private CommandLine cmd;
    StringWriter sw;

    @BeforeEach
    void setUp() {
        cmd = new CommandLine(new ReleaseCommand());
        sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));
    }
    
    @Test
    void shouldbeOk() {
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));
        String arg = "name=local-collector,path=/home/borstg/git/argos4j-cli/src/main/resources/log4j.properties,basePath=/home/borstg/git/argos4j-cli/src/main/resources";
        int exitCode = cmd.execute("-c",arg);
        assertThat(exitCode, is(1));
    }
    
    @Test
    void throwIllegalArgument() {
        String arg = "name=local-collector,path/foo=/home/borstg/git/argos4j-cli";
        int exitCode = cmd.execute("-c",arg);
        assertThat(exitCode, is(2));
        assertThat(sw.toString(), startsWith("IllegalArgumentException: Incorrect config map string: [name=local-collector"));

        sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));
        arg = "local-collector,path/foo=/home/borstg/git/argos4j-cli";
        exitCode = cmd.execute("-c",arg);
        assertThat(exitCode, is(2));
        assertThat(sw.toString(), startsWith("IllegalArgumentException: Incorrect config map string: [local-collector"));
    }
}