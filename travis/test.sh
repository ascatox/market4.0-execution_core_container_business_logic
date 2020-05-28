#!/bin/bash
newman run ./travis/tests/tests-${NETWORK}.json --insecure

docker logs market40-execution_core_container_business_logic_data-app_1
docker logs market40-execution_core_container_business_logic_ecc-consumer_1
docker logs market40-execution_core_container_business_logic_ecc-provider_1