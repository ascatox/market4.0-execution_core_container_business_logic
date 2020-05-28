#!/bin/bash
curl --location --request POST 'https://localhost:9000/sendFile' \
--header 'Forward-To-Internal: wss://producer:8887' \
--header 'Forward-To: https://consumer:8890/incoming-data-channel/receivedMessage' \
--header 'Content-Type: text/plain' \
--data-raw 'MultipartEngineeringDEMO.txt'

newman run ./travis/tests/tests.json --insecure