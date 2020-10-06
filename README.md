ARGOS argos4j-cli [![Build Status](https://cloud.drone.io/api/badges/argosnotary/argos4j-cli/status.svg)](https://cloud.drone.io/argosnotary/argos4j-cli)[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=argosnotary_argos4j-cli&metric=alert_status)](https://sonarcloud.io/dashboard?id=argosnotary_argos4j-cli)
============
cli based argos4j docker container
## Useage
You can use this docker base image in any docker multistage build image of choice this way you can "wrap" your build commands with argos4j cli.

This is illustrated in the project example ArgosBuildWithArgosWrapper docker file ArgosBuildWithArgosWrapper.

```
    FROM argosnotary/argos4j-cli:latest as argosWrapper
    FROM argosnotary/argos-build:latest
    COPY --from=argosWrapper /usr/local/lib/argos /usr/local/lib/argos
    RUN cd /usr/bin && ln -s /usr/local/lib/argos/bin/postLink
```

In the example above the docker argos4j-cli is fetched from the dockerhub repository and used in the build image of argos notary.
The argos4j-cli docker image contains a sh or bat script to use  located in /usr/local/lib/argos.
The executable script is called postLink the last command is used to create a soft link so it can be called without the path.

The following docker environment variables can be used in any build step to connect to an argosnotary service instance.

           WORKSPACE: the workspace of your ci cd pipeline setup eg "/drone/src"
           ARGOS_SERVICE_BASE_URL: the url of the argosnotary service eg "https://notary.argosnotary.org/api"
           CREDENTIALS_PASSPHRASE: the passphrase of the service account key configured in argosnotary.
           CREDENTIALS_KEY_ID: the keyId of the service account key configured in argosnotary.
           SUPPLY_CHAIN_NAME: your supplychain name configured in argosnotary eg "argos-test-app"
           SUPPLY_CHAIN_PATH : the label path to your supplychain configured in argosnotary eg "com.argosnotary"
           RUN_ID : bcdd4bf0245c82c060407b3b24b9b87301d15ac1

Commands can then be wrapped to collect materials and products and send sent link files to argosnotary.

       - postLink --phase pre --segment jenkins --step build --runId $RUN_ID
       - mvn -s settings.xml install
       - postLink --phase post --segment jenkins --step build --runId $RUN_ID

The first command before the build command (--phase pre)  is used to make a snapshot of the pipeline workspace before the main command is executed.
It collects the materials and stores a signed link in the workspace.

The second command (--phase post)  is executed after the main command, it makes a new snapshot of the workspace and sends the stored link file to the argos service.

Complete example for drone ci:

       - name: build
         image:argosnotary/argosbuild-argos4j-cli:latest
         volumes:
           - name: mvn_cache
             path: /root/.m2/repository
         environment:
           WORKSPACE: /drone/src
           ARGOS_SERVICE_BASE_URL: http://192.168.2.2:8080/api
           CREDENTIALS_PASSPHRASE:
             from_secret: argos-service-account-pw
           CREDENTIALS_KEY_ID:
             from_secret: argos-service-account-key-id
           SUPPLY_CHAIN_NAME: argos-test-app
           SUPPLY_CHAIN_PATH : com.argosnotary
           DRONE_COMMIT : bcdd4bf0245c82c060407b3b24b9b87301d15ac1
         commands:
           - postLink --phase pre --segment drone --step build --runId $DRONE_COMMIT
           - mvn -s settings.xml install
           - postLink --phase post --segment drone --step build --runId $DRONE_COMMIT



