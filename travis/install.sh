#!/bin/bash

DOCKER_COMPOSE_VERSION=1.25.5
#secure=K3i0j0PILpZ2iSocwBQStRxYwbnlGsJdJ2ZZHBIY61S0hcdwLmLchzlazOpIEmHbGPSiw48HC899BCcmsUXKJciHtWdskNnC3YLKhZFVd7ehn6KKC4kofNkCywkfQ+o9kKwlLkGd3aheKbi6mRT79rtiSk6b4F0nA3blDJ+2huf85Xy7OOYu+AhT/X2OokjXVzOrq7C9g7iYA80nyZrBqjkswIZ6pE8hlNEOCR42gApb06hopZXqPUKDxGO5H1N5bV4w0I8HZLkRl9yNwk0UfP97s/VmKyO15xvBMNMtUknbigkJvrA88iwUJUNfCrmLoxQM0J/vllATLc9SxpFjs1eOvv09rwEQq+Bv5Mgy+zuEmZECs2o77kPm9lhnVGndAIPxK3X9D5Yg2rzseLAOZizx27sWrvgt+XEe4cI9w3uR0T16WNCqXlI3jtIZHg8X3BTCesVjlVy6zrH01bC9n0yrv75Th4m2X9kokskbvnw6/CIn9+z80G9vXM00DWkIAGdvDTanZVHy0/g/1KOrruYf05FByBehmWOcA7y4lDtAV8iMPunByk9K2/WToF25cGKTddCerLxyyeisQZ7YoUCoET6rcEheQSwRMZZEgn6ZyjDB2gy9VWccq7ISrcya5Z8WKbA3IAGr/lsRG3b3qhLGJRx2T9EbJs48YsPxlc4=
#secure=kJsDkI8Pb0UNHHoh1l7F4vZwwIsGeZyuplkWax06dH1WG917nE48y33W+GiQw/bjK2LLQDU//2S1BDsU6MxLMxqUJNtQegI6iAXCnwKGAqq4uaC/CqC4mVEZibiCsN/9SdJM6RUUUor6vY16DnQV+MrCnaLHWhD+L21nYu1Ryzzd6gaXNhu3XtIk+jE8iTxTHucju6e09dSk33m0O8PolNrJwV8IqF0ADZ+vpqeuTJ0YMAbwa95F/LNiZFQ7udTK93KnCW2O/60hskNW8G3+7+WnX9DrXbPAjwVQr7u5T2CkmBQHNr0J5F4WjSTWM9phNbRrqRes2r5PVmnesZwWqqePe9eeFJZ6gnoZA5qBqZNyrUfkcwKH+hPHSMIkbz3FX11Bro69uxwUO3lQ9/nt/NaEUwKh+0QIQns8rBVcwBbFy/5cAju+JbJ2uxfPOHItqrKlHCh0YJsPZDrz1cfl+3X6sn32hwBq6z0AL7HebkW2t9iUlelfegAtz0nN5ksKLZrg8pFxVCkrC/vRfhpF5d2nlIcbnkyJZ03Axm2c+wOLwcwfyKuWS1By4YaATDxeBk1D2cvE0dhRbj+VU69uDwhvqwTEvbdxxYXt+8iR5X278hPxftmXoBEY8JDheq9WNSlaXCQ3DeIFkBC7XGc7ABmC2djMihLGc+BtKULwq94=

SSL_KEY_PASSWORD=changeit
KEYSTORE_PASSWORD=password

mkdir $HOME/hash
cp -rf ./travis/cert $HOME

BRANCH_DATA_APP=master

if [ "$1" != "" ]; then
  BRANCH_DATA_APP=$1
fi

echo "Installing Newman CLI..."
npm install -g newman@4.5.1
newman --version
echo "Newman installed, READY TO TEST..."


echo "Downloading and Installing docker-compose..."
sudo rm /usr/local/bin/docker-compose
curl -L https://github.com/docker/compose/releases/download/${DOCKER_COMPOSE_VERSION}/docker-compose-`uname -s`-`uname -m` > docker-compose
chmod +x docker-compose
sudo mv docker-compose /usr/local/bin
echo "docker-compose correctly installed"

#echo "REMOVE ASAP-> Settings.xml copy on Travis Home_M2 at: "$HOME
#cp -f ./travis/.m2/settings.xml $HOME/.m2/settings.xml
#cp -rf ./travis/.m2/repository  $HOME/.m2/
#echo "REMOVE ASAP-> Settings.xml copy on Travis Home_M2 at: "$HOME

echo "Installing Multipart Message Lib..."
git clone https://github.com/Engineering-Research-and-Development/market4.0-ids_multipart_message_processor
cd market4.0-ids_multipart_message_processor
mvn clean install -DskipTests
cd ..
echo "Installed  Multipart Message Lib"

echo "Installing websocket-message-streamer-lib..."
git clone https://github.com/ascatox/websocket-message-streamer.git
cd websocket-message-streamer
mvn clean install -DskipTests
cd ..
echo "Installed websocket-message-streamer-lib"

echo "Cloning and Creating Docker Container from Data-App repo..."
git clone https://github.com/ascatox/market4.0-data_app_test_BE.git
cd market4.0-data_app_test_BE
git checkout ${BRANCH_DATA_APP}
mvn clean package -DskipTests
docker build -f Dockerfile -t market4.0/data-app .
cd ..
echo "Data-App is ready to start"

echo "Downloading and installing Clearing-House Model..."
git clone https://gitlab.com/eng-siena-ri/market4.0/clearing-house.git
cd clearing-house
mkdir chaincode-libs
mvn install -DskipTests
cd ..
echo "Clearing-House Model installed!"

echo "Creating Docker Container for ECCs..."
mvn clean package -DskipTests
docker build -f Dockerfile -t market4.0/execution_core_container_business .

echo "Starting services..."
docker-compose -f travis/docker/docker-compose-${NET}-${NETE}.yaml up -d