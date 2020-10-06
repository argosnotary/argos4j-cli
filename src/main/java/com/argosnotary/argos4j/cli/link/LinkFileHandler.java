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
package com.argosnotary.argos4j.cli.link;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.argosnotary.argos.argos4j.Argos4jError;
import com.argosnotary.argos.argos4j.Argos4jSettings;
import com.argosnotary.argos.argos4j.LinkBuilderSettings;
import com.argosnotary.argos.argos4j.internal.ArgosServiceClient;
import com.argosnotary.argos.argos4j.internal.mapper.RestMapper;
import com.argosnotary.argos.argos4j.rest.api.model.RestLinkMetaBlock;
import com.argosnotary.argos.domain.crypto.signing.SignatureValidator;
import com.argosnotary.argos.domain.link.Link;
import com.argosnotary.argos.domain.link.LinkMetaBlock;

import lombok.RequiredArgsConstructor;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.mapstruct.factory.Mappers;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;

@RequiredArgsConstructor
class LinkFileHandler {

    private final Argos4jSettings settings;
    private final LinkBuilderSettings linkBuilderSettings;

    public void storeLink(LinkMetaBlock linkMetaBlock, String workspace) throws  IOException {
        RestLinkMetaBlock restLinkMetaBlock = Mappers.getMapper(RestMapper.class).convertToRestLinkMetaBlock(linkMetaBlock);
        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(restLinkMetaBlock);
        IOUtils.write(json, new FileOutputStream(createPath(workspace).toString()), UTF_8);
    }
    
    private Path createPath(String workspace)  {
        List<String> fileNameList = new ArrayList<>();
        fileNameList.add(settings.getKeyId());
        fileNameList.addAll(settings.getPath());
        fileNameList.add(settings.getSupplyChainName());
        fileNameList.add(linkBuilderSettings.getRunId());
        fileNameList.add(linkBuilderSettings.getLayoutSegmentName());
        fileNameList.add(linkBuilderSettings.getStepName());
        String fileName = String.join("-", fileNameList) + ".link";
        return Paths.get(workspace, fileName);
    }

    Optional<Link> getLink(String workspace) {
        Path filePath = createPath(workspace);
        if (filePath.toFile().exists()) {
            RestLinkMetaBlock restLinkMetaBlock;
            try {
                String json = IOUtils.toString(filePath.toUri(), UTF_8);
                ObjectMapper objectMapper = new ObjectMapper();
                restLinkMetaBlock = objectMapper.readValue(json, RestLinkMetaBlock.class);
            } catch (IOException e) {
                throw new Argos4jError("Error on readding link: "+e.getMessage());
            }
            LinkMetaBlock linkMetaBlock = Mappers.getMapper(RestMapper.class).convertFromRestLinkMetaBlock(restLinkMetaBlock);
            checkSignature(linkMetaBlock);
            return Optional.of(linkMetaBlock.getLink());
        }
        return Optional.empty();
    }

    private void checkSignature(LinkMetaBlock linkMetaBlock) {
        ArgosServiceClient argosServiceClient = new ArgosServiceClient(settings, settings.getKeyPassphrase().toCharArray());
        PublicKey publicKey;
        try {
            publicKey = Mappers.getMapper(RestMapper.class).convertFromRestServiceAccountKeyPair(argosServiceClient.getKeyPair()).getJavaPublicKey();
        } catch (GeneralSecurityException | IOException e) {
            throw new Argos4jError("Invalid Public Key: "+e.getMessage());
        }
        if (!SignatureValidator.isValid(linkMetaBlock.getLink(), linkMetaBlock.getSignature(), publicKey)) {
            throw new Argos4jError("invalid signature");
        }
    }

    void removeLink(String workspace)  {
        FileUtils.deleteQuietly(createPath(workspace).toFile());
    }

}
