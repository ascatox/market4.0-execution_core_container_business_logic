echo "Stress TEST DATA over IDSCP (WSS internal comm) - SMALL size"
./travis/tests/naive-stress-test.sh -c ${CONCURR} -r ${REPS}} -a 'https://localhost:9000/sendFile' \
-X POST \
-H 'Forward-To-Internal: wss://ecc-provider:8887' \
-H 'Forward-To: idscp://ecc-consumer:8086' \
-H 'Content-Type: text/plain' \
-d 'MultipartEngineeringDEMO.txt'