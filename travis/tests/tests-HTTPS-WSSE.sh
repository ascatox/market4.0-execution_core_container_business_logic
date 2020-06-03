echo "Stress TEST DATA over HTTPS (WSS internal comm) - SMALL size"
./naive-stress-test.sh -c ${CONCURR} -r ${REPS}} -a 'https://localhost:9000/sendFile' \
-X POST \
-H 'Forward-To-Internal: wss://ecc-provider:8887' \
-H 'Forward-To: https://ecc-consumer:8890/incoming-data-channel/receivedMessage' \
-H 'Content-Type: text/plain' \
-d 'MultipartEngineeringDEMO.txt'