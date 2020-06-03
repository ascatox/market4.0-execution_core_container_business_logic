echo "Stress TEST DATA over WSS (WSS internal comm) - SMALL size"
./naive-stress-test.sh -c ${CONCURR} -r ${REPS}} -a 'https://localhost:9000/sendFile' \
-X POST \
-H 'Forward-To-Internal: wss://ecc-provider:8887' \
-H 'Forward-To: wss://ecc-consumer:8086/incoming-data-channel/receivedMessage' \
-H 'Content-Type: text/plain' \
-d 'MultipartEngineeringDEMO.txt'