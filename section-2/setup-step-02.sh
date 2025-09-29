#!/bin/bash

# This script sets up the environment for section-2/step-02 of the Quarkus LangChain4j workshop
# It copies necessary files from step-02 to step-01 and creates required directories

# Change to the step-01 directory
cd ./step-01

# Copy UI and model files from step-02 to step-01
echo "Copying UI and model files..."
cp ../step-02/src/main/resources/static/css/styles.css ./src/main/resources/static/css/styles.css
cp ../step-02/src/main/resources/static/js/app.js ./src/main/resources/static/js/app.js
cp ../step-02/src/main/resources/templates/index.html ./src/main/resources/templates/index.html
cp ../step-02/src/main/java/com/carmanagement/service/CarService.java ./src/main/java/com/carmanagement/service/CarService.java
cp ../step-02/src/main/java/com/carmanagement/model/CarInfo.java ./src/main/java/com/carmanagement/model/CarInfo.java

# Create the workflow directory
echo "Creating workflow directory..."
mkdir -p ./src/main/java/com/carmanagement/agentic/workflow

echo "Setup complete! You can now continue with the workshop."
