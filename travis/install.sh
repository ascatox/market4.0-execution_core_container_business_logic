#!/bin/bash

mkdir $HOME/hash
mkdir $HOME/cert

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

echo "REMOVE ASAP-> Settings.xml copy on Travis Home_M2 at: "$HOME
cp -f ./travis/.m2/settings.xml $HOME/.m2/settings.xml
cp -rf ./travis/.m2/repository  $HOME/.m2/
echo "REMOVE ASAP-> Settings.xml copy on Travis Home_M2 at: "$HOME

echo "Installing websocket-message-streamer-lib..."
git clone https://github.com/ascatox/websocket-message-streamer.git
cd websocket-message-streamer
mvn clean install -DskipTests
cd ..
echo "Installed websocket-message-streamer-lib"

echo "Cloning and Dockerizing Data-App repo..."
git clone https://github.com/ascatox/market4.0-data_app_test_BE.git
cd market4.0-data_app_test_BE
git checkout master
sh dockerize.sh
cd ..
echo "Data-App is ready to start"


echo "Downloading and installing Clearing-House Model..."
git clone https://gitlab.com/eng-siena-ri/market4.0/clearing-house.git
cd clearing-house
mkdir chaincode-libs
mvn install -DskipTests
cd ..
echo "Clearing-House Model installed!"

echo "Dockerizing ECCs..."
mvn clean package -DskipTests
docker build -f Dockerfile -t market4.0/execution_core_container_business .

echo "Starting services..."
docker-compose up -d

echo "SERVICES STARTED :-)"
