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

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import picocli.CommandLine.IParameterConsumer;
import picocli.CommandLine.Model.ArgSpec;
import picocli.CommandLine.Model.CommandSpec;

public class ReleaseConfigMapConsumer implements IParameterConsumer {
    private static final String CONFIG_MAP_PATTERN = 
            "^name=[a-zA-Z0-9_\\-]+,([a-zA-Z0-9_\\-]+=[a-zA-Z0-9_\\-\\/.]+,)*"
            + "[a-zA-Z0-9_\\-]+=[a-zA-Z0-9_\\-\\/.]+"
            + "(;name=[a-zA-Z0-9_\\-]+,([a-zA-Z0-9_\\-]+=[a-zA-Z0-9_\\-\\/.]+,)*[a-zA-Z0-9_\\-]+=[a-zA-Z0-9_\\-\\/.]+)*$";

    @Override
    public void consumeParameters(Stack<String> args, ArgSpec argSpec, CommandSpec commandSpec) {
        Map<String, Map<String,String>> configMaps = argSpec.getValue();
        if (!args.isEmpty()) {
            String arg = args.pop();
            checkConfigArg(arg);
            for (String mapString : arg.split(";")) {
                Map<String,String> configMap = new HashMap<>();
                String[] mapArray = mapString.split(",");
                String name = mapArray[0].split("=")[1];
                for (int i = 1; i< mapArray.length;i++) {
                    String[] keyValue = mapArray[i].split("=");
                    configMap.put(keyValue[0], keyValue[1]);
                }
                configMaps.put(name, configMap);
            }
        }
    }
    
    private void checkConfigArg(String arg) {
        Pattern r = Pattern.compile(CONFIG_MAP_PATTERN);
        Matcher matchString = r.matcher(arg);
        if (!matchString.find()) {
            throw new IllegalArgumentException(String.format("Incorrect config map string: [%s]", arg));
        }
    }

}
