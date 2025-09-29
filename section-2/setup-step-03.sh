#!/bin/bash

# This script sets up the environment for section-2/step-03 of the Quarkus LangChain4j workshop
# It copies necessary files from step-03 to step-01

# Change to the step-01 directory
cd ./step-01

# Copy UI and model files from step-03 to step-01
echo "Copying UI and model files..."
cp ../step-03/src/main/resources/static/css/styles.css ./src/main/resources/static/css/styles.css
cp ../step-03/src/main/resources/static/js/app.js ./src/main/resources/static/js/app.js
cp ../step-03/src/main/resources/templates/index.html ./src/main/resources/templates/index.html
cp ../step-03/src/main/java/com/carmanagement/service/CarService.java ./src/main/java/com/carmanagement/service/CarService.java
cp ../step-03/src/main/java/com/carmanagement/model/CarStatus.java ./src/main/java/com/carmanagement/model/CarStatus.java

echo "Setup complete! You can now continue with the workshop."
