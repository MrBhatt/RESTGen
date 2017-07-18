# RESTCodeGen
[![Build Status](https://travis-ci.org/MrBhatt/RESTCodeGen.svg?branch=master)](https://travis-ci.org/MrBhatt/RESTCodeGen)

Utility to generate a Spring Boot and gradle based application (deployable as a microservice) for exposing a resource via RESTFUL APIs. The generation of the application / APIscan be controlled via declarative config provided in appconfig.yaml. 

Given a resource (say car):

Generated code:
 - Spring Boot main application class 'Application.java' (if set to true in appconfig)
 - Model for the resource (Car.java) backed by a Abstract entity (AbstractEntity) for common attributes like ID etc
 - Controller to route http requests (CarController)
 - Service interfaces and implementation (CarService, CarServiceImpl)
 - Spring data Repository (CarRepository.java)

## How to generate code:
RESTCodeGen is a gradle based project. 

To execute (with default config yaml which comes with this source code): 
gradle run

To execute (with user specified config yaml file):
gradle run -PappArgs="['<<path to yaml config>>']"
 example: gradle run -PappArgs="['appconfig.yml']" where appconfig.yml exists in the current directory
