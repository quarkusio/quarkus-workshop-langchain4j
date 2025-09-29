#!/bin/bash

# This script sets up the environment for section-2/step-04 of the Quarkus LangChain4j workshop
# It combines commands from both "Before you begin" sections in step-04.md

# Part 1: Setup for multi-agent-system
echo "Setting up multi-agent-system..."
mv step-01 multi-agent-system
mkdir step-01
mv multi-agent-system step-01
cd step-01/multi-agent-system
cp ../../step-04/multi-agent-system/pom.xml ./pom.xml
cp ../../step-04/multi-agent-system/src/main/java/com/carmanagement/model/CarInfo.java ./src/main/java/com/carmanagement/model/CarInfo.java
cp ../../step-04/multi-agent-system/src/main/java/com/carmanagement/model/CarStatus.java ./src/main/java/com/carmanagement/model/CarStatus.java
cp ../../step-04/multi-agent-system/src/main/java/com/carmanagement/service/CarService.java ./src/main/java/com/carmanagement/service/CarService.java
cp ../../step-04/multi-agent-system/src/main/resources/static/css/styles.css ./src/main/resources/static/css/styles.css
cp ../../step-04/multi-agent-system/src/main/resources/static/js/app.js ./src/main/resources/static/js/app.js
cp ../../step-04/multi-agent-system/src/main/resources/templates/index.html ./src/main/resources/templates/index.html

# Return to section-2 directory
cd ../../

# Part 2: Setup for remote-a2a-agent
echo "Setting up remote-a2a-agent..."
cd ./step-01
mkdir remote-a2a-agent
cd remote-a2a-agent
cp ../../step-04/remote-a2a-agent/mvnw.cmd ./mvnw.cmd
cp ../../step-04/remote-a2a-agent/pom.xml ./pom.xml
cp ../../step-04/remote-a2a-agent/mvnw ./mvnw
mkdir -p ./src/main/resources
mkdir -p ./src/main/java/com/demo
cp ../../step-04/remote-a2a-agent/src/main/resources/application.properties ./src/main/resources/application.properties

# Return to section-2 directory
cd ../../

echo "Setup complete! You can now continue with the workshop."
