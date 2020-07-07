ARGOS argos4j-cli [![Build Status](https://cloud.drone.io/api/badges/argosnotary/argos4j-cli/status.svg)](https://cloud.drone.io/argosnotary/argos4j-cli)[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=argosnotary_argos4j-cli&metric=alert_status)](https://sonarcloud.io/dashboard?id=argosnotary_argos4j-cli)
============
cli based argos4j docker container
## Useage
You can use this docker base image in any docker multistage based build image of choice this way you can "wrap" your build commands with argos4j cli.
this is illustrated in the project example ArgosBuildWithArgosWrapper docker file ArgosBuildWithArgosWrapper.
```shell FROM argosnotary/argos4j-cli-snapshot:latest as argosWrapper
         FROM argosnotary/argos-build:3.6.3
         COPY --from=argosWrapper /usr/local/lib /usr/local/lib```

In the example above the




