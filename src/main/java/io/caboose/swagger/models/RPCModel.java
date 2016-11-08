package io.caboose.swagger.models;


import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Model to describe functions / RPCs
 */
public class RPCModel {
    String functionName;
    String path;
    Map<String, Class<?>> pathInputMap = new HashMap<>();
    Map<String, Class<?>> querystringMap = new HashMap<>();
    String input;
    Boolean inputIsArray = false;
    String output;
    String inputShortName;
    String outputShortName;
    String method;
    List<ErrorModel> errors = new ArrayList<>();

    public RPCModel(){

    }

    /**
     * RPC Constructor
     * @param functionName name for function
     * @param input classname for input
     * @param output classname for output
     */
    public RPCModel(String functionName, String input, String output) {
        this.functionName = functionName;
        this.input = input;
        this.output = output;
        setInputShortNameFromName(input);
        setOutputShortNameFromName(output);
    }

    protected void setInputShortNameFromName(String name){
        this.inputShortName = name.substring(name.lastIndexOf('.') + 1).trim();
    }

    protected void setOutputShortNameFromName(String name){
        this.outputShortName = name.substring(name.lastIndexOf('.') + 1).trim();
    }



    public String getFunctionName() {
        return functionName;
    }

    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
        setInputShortNameFromName(input);
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
        setOutputShortNameFromName(output);
    }

    public String getInputShortName() {
        return inputShortName;
    }

    public String getOutputShortName() {
        return outputShortName;
    }

    public Class<?> getPathInput(String fieldName){
        return pathInputMap.get(fieldName);
    }

    public void putPathInput(String fieldName, Class<?> clazz){
        pathInputMap.put(fieldName, clazz);
    }

    public Class<?> getQuerystring(String fieldName){
        return querystringMap.get(fieldName);
    }

    public void putQuerystring(String fieldName, Class<?> clazz){
        querystringMap.put(fieldName, clazz);
    }

    public Boolean getInputIsArray() {
        return inputIsArray;
    }

    public void setInputIsArray(Boolean inputIsArray) {
        this.inputIsArray = inputIsArray;
    }

    public String getFunctionParams(){
        Stream<String> qsStream =
                querystringMap.entrySet().stream().map(es -> es.getValue().getSimpleName() + " " + es.getKey());
        Stream<String> pathStream =
                pathInputMap.entrySet().stream().map(es -> es.getValue().getSimpleName() + " " + es.getKey());
        return Stream.concat(pathStream, qsStream)
                .collect(Collectors.joining(", "));
    }

    public String getFunctionParamsForRoute(){
        Stream<String> qsStream =
                querystringMap.entrySet().stream().map(es -> es.getKey() + " : " + es.getValue().getSimpleName() + " ?= null ");
        Stream<String> pathStream =
                pathInputMap.entrySet().stream().map(es -> es.getKey() + " : " +  es.getValue().getSimpleName());
        return Stream.concat(pathStream, qsStream)
                .collect(Collectors.joining(", "));

    }

    public void addError(ErrorModel error){
        errors.add(error);
    }

    public List<ErrorModel> getErrors() {
        return errors.stream()
                .sorted((e1, e2) -> new Integer(e1.statusCode).compareTo(e2.statusCode))
                .collect(Collectors.toList());
    }

    public String getCamelFunctionName(){
        return functionName.substring(0, 1).toUpperCase() + functionName.substring(1);
        //return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, functionName);
    }

    public boolean getHasErrors() {
        return !errors.isEmpty();
    }


    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path.replaceAll("\\{", ":").replaceAll("\\}", "");
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }
}
