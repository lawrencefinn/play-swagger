# play swagger

This library provides a way to easily use swagger to define your API

## Generate models
Use existing swagger cli to generate models for your swagger defintion.  Example
```
java -DmodelDocs=false -Dmodels -Dapis=false  -jar swagger-codegen-cli.jar generate -i http://petstore.swagger.io/v2/swagger.json --library=jersey2 -l java -o boops --model-package io.caboose.api.model
```

## Internals
Extend SwaggerParser class with your swagger model class as part of the definition.  Use it as a typical play bodyparser
See test/java/io/caboose/api/parsers/PetParser.java as an example parser

## Code Generation
 Use the SwaggerServiceGeneratorUtil class to generate controllers, parsers, and routes from swagger def
 Example:
 new ServiceCodeGeneratorUtil().generateClassFiles("something.swagger", "app", true, "my.package.name");
 new ServiceCodeGeneratorUtil().generateRoutesFiles("something.swagger", "conf/routes");

## Testing
./sbt test

## Building
./sbt packageLocal

