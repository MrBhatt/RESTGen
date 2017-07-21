# RESTGen
[![Build Status](https://travis-ci.org/MrBhatt/RESTGen.svg?branch=master)](https://travis-ci.org/MrBhatt/RESTGen)

Utility to generate a Spring Boot and gradle based application (deployable as a microservice) for exposing a resource via RESTFUL APIs. The generation of the application / APIscan be controlled via declarative config provided in appconfig.yaml. 

Given a resource (say car):

Generated code:
 - Spring Boot main application class 'Application.java' (if set to true in appconfig)
 - Model for the resource (Car.java) backed by a Abstract entity (AbstractEntity) for common attributes like ID etc
 - Controller to route http requests (CarController)
 - Service interfaces and implementation (CarService, CarServiceImpl)
 - Spring data Repository (CarRepository.java)

## How to generate your REST application code:
### Download binary (zipped archive): 
#### Versioning: RESTGen tries to follow semantic versioning as described here: http://semver.org/ 
[ ![Download](https://api.bintray.com/packages/anupambhatt/RESTGen/RESTGen-CommandLine/images/download.svg) ](https://bintray.com/anupambhatt/RESTGen/RESTGen-CommandLine/_latestVersion)

### Download source code, build and execute:
RESTGen is a gradle based project. 

To generate code:
- With default config yaml which comes with this source code: `gradle run`

- With specified config yaml file: `gradle run -PappArgs="['path to yaml config']"`
  - example: `gradle run -PappArgs="['appconfig.yml']"` where appconfig.yml exists in the current directory

## Sample config yaml
```
boot-application: true
package: com.mrbhatt.test
resource: plane
resource-attributes:
   make: string
   model: string
   color: string
   price: int
```
