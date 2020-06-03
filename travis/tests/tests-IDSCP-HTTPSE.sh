echo "Stress TEST DATA over IDSCP (HTTPS internal comm) - SMALL size"
./naive-stress-test.sh -c ${CONCURR} -r ${REPS}} -a 'https://localhost:8887/incoming-data-app/multipartMessageBodyBinary' \
-X POST \
-H 'Content-Type: multipart/mixed; boundary=CQWZRdCCXr5aIuonjmRXF-QzcZ2Kyi4Dkn6' \
-H 'Forward-To: idscp://ecc-consumer:8086' \
-H 'Content-Type: text/plain' \
-d '--CQWZRdCCXr5aIuonjmRXF-QzcZ2Kyi4Dkn6
Content-Disposition: form-data; name="header"
Content-Length: 333

{
  "@type" : "ids:ArtifactResponseMessage",
  "issued" : "2019-05-27T13:09:42.306Z",
  "issuerConnector" : "http://iais.fraunhofer.de/ids/mdm-connector",
  "correlationMessage" : "http://industrialdataspace.org/connectorUnavailableMessage/1a421b8c-3407-44a8-aeb9-253f145c869a",
  "transferContract" : "https://mdm-connector.ids.isst.fraunhofer.de/examplecontract/bab-bayern-sample/",
  "modelVersion" : "1.0.2-SNAPSHOT",
  "@id" : "https://w3id.org/idsa/autogen/artifactResponseMessage/eb3ab487-dfb0-4d18-b39a-585514dd044f"
}
--CQWZRdCCXr5aIuonjmRXF-QzcZ2Kyi4Dkn6
Content-Disposition: form-data; name="payload"
Content-Length: 50

{"catalog.offers.0.resourceEndpoints.path":"/pet2"}
--CQWZRdCCXr5aIuonjmRXF-QzcZ2Kyi4Dkn6--'
