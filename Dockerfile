#
# Copyright (C) 2020 Argos Notary Coöperatie UA
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#         http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

FROM openjdk:11-jre-slim
COPY target/appassembler /usr/local/argos
RUN chmod 777 /usr/local/argos/bin/* \
    && cd /usr/bin && ln -s /usr/local/argos/bin/argos-cli
    
RUN adduser --system --home /home/argos --uid 1000 argos

USER argos

WORKDIR /home/argos
