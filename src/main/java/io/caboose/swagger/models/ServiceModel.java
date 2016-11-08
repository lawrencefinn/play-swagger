package io.caboose.swagger.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Model describing service
 */
public class ServiceModel {
    Optional<String> javaPackage = Optional.empty();
    String name;
    List<RPCModel> functions;

    /**
     * Default empty constructor
     */
    public ServiceModel() {
        this(Optional.empty(), null, new ArrayList<>());
    }

    /**
     * Constructor
     * @param javaPackage optional string with java package name.  used in code generation
     * @param name name of service
     * @param functions list of functions / RPCs
     */
    public ServiceModel(Optional<String> javaPackage, String name, List<RPCModel> functions) {
        this.javaPackage = javaPackage;
        this.name = name;
        this.functions = functions;
    }

    public Optional<String> getJavaPackage() {
        return javaPackage;
    }

    public void setJavaPackage(Optional<String> javaPackage) {
        this.javaPackage = javaPackage;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<RPCModel> getFunctions() {
        return functions.stream().sorted((e1, e2) -> e1.getFunctionName().compareTo(e2.getFunctionName()))
                .collect(Collectors.toList());
    }

    public void setFunctions(List<RPCModel> functions) {
        this.functions = functions;
    }

    public void addFunction(RPCModel function){
        functions.add(function);
    }


    public String getErrorsMap(){
        String instantiations = functions.stream().filter(func -> !func.getErrors().isEmpty()).map(func -> {
            String funcDef = "\n\t\t\"" + func.getFunctionName() + "\", ImmutableMap.of(\n";
            String errorDefs = func.getErrors().stream().map(error -> {
                return "\t\t\t\"" + error.getCode() + "\", new ErrorModel(" + error.getStatusCode() + ", \"" + error.getMessage() + "\", \"" + error.getCode() + "\")";
            }).collect(Collectors.joining(", \n"));
            return funcDef + errorDefs;
        }).filter(line -> !line.isEmpty()).collect(Collectors.joining("\n\t\t), ")) + "\n\t\t)";
        return "\tprotected Map<String, Map<String, ErrorModel>> functionCodeErrorMap = ImmutableMap.of(" + instantiations + "\n\t);";
    }
}
