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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabobank.argos.argos4j.Argos4jError;
import com.rabobank.argos.argos4j.Argos4jSettings;
import com.rabobank.argos.argos4j.internal.Argos4JSigner;
import com.rabobank.argos.argos4j.internal.ArgosServiceClient;
import com.rabobank.argos.argos4j.internal.mapper.RestMapper;
import com.rabobank.argos.argos4j.rest.api.model.RestLinkMetaBlock;
import com.rabobank.argos.argos4j.rest.api.model.RestServiceAccountKeyPair;
import com.rabobank.argos.domain.Signature;
import com.rabobank.argos.domain.key.RSAPublicKeyFactory;
import com.rabobank.argos.domain.link.Link;
import com.rabobank.argos.domain.link.LinkMetaBlock;
import com.rabobank.argos.domain.signing.JsonSigningSerializer;
import com.rabobank.argos.domain.signing.SignatureValidator;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.mapstruct.factory.Mappers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;

class LinkRegistry {
    private LinkRegistry() {
    }

    private static Properties properties = Properties.getInstance();

    static void storeLink(String runId, String segmentName, String stepName, Link link) throws URISyntaxException, IOException {
        Argos4jSettings argos4jSettings = createArgosSettings();
        ArgosServiceClient argosServiceClient = new ArgosServiceClient(argos4jSettings, properties.getPassPhrase().toCharArray());
        Signature signature = new Argos4JSigner().sign(argosServiceClient.getKeyPair(), properties.getPassPhrase().toCharArray(), new JsonSigningSerializer().serialize(link));
        LinkMetaBlock linkMetaBlock = LinkMetaBlock.builder().link(link).signature(signature).build();
        RestLinkMetaBlock restLinkMetaBlock = Mappers.getMapper(RestMapper.class).convertToRestLinkMetaBlock(linkMetaBlock);
        ObjectMapper objectMapper = new ObjectMapper();
        String fileName = createFileName(runId, segmentName, stepName);
        String json = objectMapper.writeValueAsString(restLinkMetaBlock);
        IOUtils.write(json, new FileOutputStream(new File(fileName)), "UTF-8");
    }

    private static String createFileName(String runId, String segmentName, String stepName) throws URISyntaxException {
        Path basePath = Paths.get(
                LinkRegistry.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                .getParent().toAbsolutePath();
        return basePath.toString() +
                "/" +
                properties.getKeyId() + "-" +
                String.join("-", properties.getPath()) + "-" +
                properties.getSupplyChainName() + "-" +
                runId + "-" +
                segmentName + "-" +
                stepName +
                "-link.json";
    }

    private static Argos4jSettings createArgosSettings() {
        return Argos4jSettings.builder()
                .argosServerBaseUrl(properties.getArgosServiceBaseUrl())
                .path(properties.getPath())
                .signingKeyId(properties.getKeyId())
                .supplyChainName(properties.getSupplyChainName())
                .build();
    }

    static Optional<Link> getLink(String runId, String segmentName, String stepName) throws URISyntaxException, IOException, GeneralSecurityException {

        String fileName = createFileName(runId, segmentName, stepName);
        boolean exists = new File(fileName).exists();
        if (exists) {
            Argos4jSettings argos4jSettings = createArgosSettings();
            String json = IOUtils.toString(Paths.get(fileName).toUri(), UTF_8);
            ObjectMapper objectMapper = new ObjectMapper();
            RestLinkMetaBlock restLinkMetaBlock = objectMapper.readValue(json, RestLinkMetaBlock.class);
            LinkMetaBlock linkMetaBlock = Mappers.getMapper(RestMapper.class).convertFromRestLinkMetaBlock(restLinkMetaBlock);
            checkSignature(argos4jSettings, linkMetaBlock);
            return Optional.of(linkMetaBlock.getLink());
        }
        return Optional.empty();
    }

    private static void checkSignature(Argos4jSettings argos4jSettings, LinkMetaBlock linkMetaBlock) throws GeneralSecurityException {
        ArgosServiceClient argosServiceClient = new ArgosServiceClient(argos4jSettings, properties.getPassPhrase().toCharArray());
        RestServiceAccountKeyPair restServiceAccountKeyPair = argosServiceClient.getKeyPair();
        SignatureValidator signatureValidator = new SignatureValidator();
        boolean signatureValid = signatureValidator
                .isValid(linkMetaBlock.getLink(),
                        linkMetaBlock.getSignature().getSignature(),
                        RSAPublicKeyFactory.instance(restServiceAccountKeyPair.getPublicKey()));
        if (!signatureValid) {
            throw new Argos4jError("invalid signature");
        }
    }

    static void removeLink(String runId, String segmentName, String stepName) throws URISyntaxException {
        String file = createFileName(runId, segmentName, stepName);
        File fileToDelete = FileUtils.getFile(file);
        FileUtils.deleteQuietly(fileToDelete);
    }

}
