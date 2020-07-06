ARGOS argos4j-cli [![Build Status](https://cloud.drone.io/api/badges/argosnotary/argos4j-cli/status.svg)](https://cloud.drone.io/argosnotary/argos4j-cli) [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=argosnotary_argos4j-cli&metric=alert_status)](https://sonarcloud.io/dashboard?id=argosnotary_argos4j-cli)
============
cli based argos4j and docker cli container
## Useage
```
services:
  argos4j-cli:
    container_name:  argos4j-cli

    build:
      context: .
    volumes:
      - ./workspace:/workspace/
    environment:
      - WORKSPACE=/workspace
      - ARGOS_SERVICE_BASE_URL=http://192.168.2.2:8080/api
      - CREDENTIALS_PASSPHRASE=VCkJUYVyUimQzO
      - CREDENTIALS_KEY_ID=f39a69bc7b4b8eaf9b2f080f3df873b5f2505320280209e05d04dade6cb2dc3f
      - SUPPLY_CHAIN_NAME=argos-test-app
      - SUPPLY_CHAIN_PATH=com.rabo
    command: sleep infinity
    working_dir: /usr/local/lib/bin
    network_mode: "host"
    ```


