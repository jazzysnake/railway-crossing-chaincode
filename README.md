# Safe Railway-crossing for autonomous vehicles

_This project is created for the Blockchain course of BME._

## Deployment and usage

The recommended way to test the project out is to install the [VSCode IBM blockchain platform plugin](https://marketplace.visualstudio.com/items?itemName=IBMBlockchain.ibm-blockchain-platform) after which a custom network can be started with the included shell script.
To start the microfab network run the following command from the root of the project:
```
./start-network.sh
```
After this, follow the instructions of the VSCode extension to add a locally running instance of microfabric. Similarly, the extension provides 
instructions to package and deploy the chaincode.

This is a gradle project and can be built with the included wrapper if you have JDK insalled:
```
./gradlew build
```
The tests can also be run very similarly. This also generates a coverage report with JaCoCo, which can be found in the build folder.
```
./gradlew test
```
If you happen to have a running sonarqube instance, after changing the sonarqube host url in the gradle.properties file, you can perform
static analysis of the code by running the sonarqube gradle task, according to the configurations of your sonarqube server.
```
./gradlew sonarqube
```

## Documentation

The assignement details and documentation can be found in pdf and markdown form [here](/docs/). 
It can also be viewed online on [hackmd](https://hackmd.io/@hacktap123/ryY-Ollr9)
