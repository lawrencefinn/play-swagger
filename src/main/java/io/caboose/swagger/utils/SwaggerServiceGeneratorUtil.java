package io.caboose.swagger.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.common.base.CaseFormat;
import io.caboose.swagger.models.DefinitionModel;
import io.caboose.swagger.models.ErrorModel;
import io.caboose.swagger.models.RPCModel;
import io.caboose.swagger.models.ServiceModel;
import io.swagger.models.*;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.PathParameter;
import io.swagger.models.parameters.QueryParameter;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import io.swagger.parser.SwaggerParser;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URL;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Generate swagger controllers, parsers, routes file, and swagger definitions
 */
public class SwaggerServiceGeneratorUtil {
    final MustacheFactory mf;
    Mustache serviceTemplate;
    Mustache routesTemplate;
    Mustache parserTemplate;
    ClassLoader loader;

    /**
     * Constructor using current class loader
     */
    public SwaggerServiceGeneratorUtil() {
        this(null);
        this.loader = getClass().getClassLoader();
        init();
    }

    /**
     * Constructor
     * @param loader class loader to load objects
     */
    public SwaggerServiceGeneratorUtil(ClassLoader loader) {
        mf = new DefaultMustacheFactory(".");
        this.loader = loader;
        if (loader != null) {
            init();
        }
    }

    /**
     * Init templates
     */
    protected void init(){
        serviceTemplate = mf.compile(getReaderForFile("service.template"), "service.template");
        routesTemplate = mf.compile(getReaderForFile("routes.template"), "routes.template");
        parserTemplate = mf.compile(getReaderForFile("parser.template"), "parser.template");
    }

    /**
     * Get Reader for file
     * @param file filename
     * @return
     */
    protected Reader getReaderForFile(String file){
        String path = URI.create(file).normalize().getPath();
        InputStream is = loader.getResourceAsStream(path);
        return new BufferedReader(new InputStreamReader(is, UTF_8));
    }

    /**
     * Type to class
     * @param type
     * @return
     */
    private Class<?> getType(String type){
        Class<?> clazz;
        switch(type){
            case "intger":
                clazz =  Integer.class;
            break;
            case "long":
                clazz =  Long.class;
            break;
            case "double":
                clazz =  Double.class;
            break;
            case "float":
                clazz =  Float.class;
            break;
            case "byte":
                clazz =  Byte.class;
            break;
            case "boolean":
                clazz =  Boolean.class;
            break;
            case "date":
                clazz =  Date.class;
            break;
            case "date-time":
                clazz =  Date.class;
            break;
            default:
                clazz =  String.class;
            break;
        }
        return clazz;
    }

    /**
     * Pull service name out of path
     * @param path
     * @return
     */
    private String getServiceFromPath(String path){
        String[] pieces = path.split("/");
        if (path.startsWith("/") && pieces.length > 1){
            return pieces[1];
        } else {
            return pieces[0];
        }
    }

    private String toCamelCase(String input){
        return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, input);
    }

    /**
     * Get services from swagger file
     * @param infile swagger file
     * @param packageName package for services
     * @return
     */
    public Map<String, ServiceModel> getServices(String infile, String packageName){
        Swagger swagger = new SwaggerParser().read(infile);
        Map<String, ServiceModel> servicesMap = new HashMap<>();
        swagger.getPaths().entrySet().forEach(set -> {
            String serviceName = getServiceFromPath(set.getKey());
            ServiceModel service = servicesMap.getOrDefault(serviceName, new ServiceModel());
            service.setJavaPackage(Optional.ofNullable(packageName));
            service.setName(toCamelCase(serviceName));
            set.getValue().getOperationMap().entrySet().forEach(methodSet -> {

                RPCModel function = new RPCModel();
                function.setFunctionName(methodSet.getValue().getOperationId());

                String functionName = methodSet.getValue().getOperationId();
                methodSet.getValue().getParameters().forEach(param -> {
                    switch(param.getIn()){
                        case "query":
                            function.putQuerystring(param.getName(),
                                    getType(((QueryParameter) param).getType())
                            );
                            break;
                        case "header":
                            break;
                        case "formData":
                            break;
                        case "path":
                            function.putPathInput(param.getName(),
                                    getType(((PathParameter) param).getType())
                            );

                            break;
                        case "body":
                            String ref;
                            Model schema = ((BodyParameter)param).getSchema();
                            boolean isArray = false;
                            if (schema instanceof ArrayModel){
                                RefProperty refProperty = (RefProperty) (((ArrayModel) schema).getItems());
                                ref = refProperty.getSimpleRef();
                                isArray = true;
                            } else {
                                ref = new RefModel(((BodyParameter) param).getSchema().getReference()).getSimpleRef();
                            }

                            function.setInput(ref);
                            function.setInputIsArray(isArray);
                            break;
                    }

                });
                Response response200 = methodSet.getValue().getResponses().get("200");
                if (response200 != null){
                    Property responseSchema = response200.getSchema();
                    if (responseSchema instanceof ArrayProperty) {
                        RefProperty innerProperty = (RefProperty) ((ArrayProperty) responseSchema).getItems();
                        function.setOutput(innerProperty.getSimpleRef());
                    } else if (responseSchema instanceof RefProperty){
                        function.setOutput(((RefProperty) responseSchema).getSimpleRef());
                    }
                }
                methodSet.getValue().getResponses().entrySet().stream()
                        .filter(entry -> !entry.getKey().equals("200") && !entry.getKey().equals("default"))
                        .forEach(entry -> {
                            String errorCode = "error" +  function.getErrors().size();
                            function.addError(new ErrorModel(Integer.parseInt(entry.getKey()),
                                    entry.getValue().getDescription(), errorCode));
                        });
                function.setMethod(methodSet.getKey().name());
                function.setPath(set.getKey());
                service.addFunction(function);

            });
            servicesMap.put(serviceName, service);
        });
        return servicesMap;
    }

    /**
     * Generate controllers / parsers for swagger definition
     * @param infile swagger file
     * @param outputPath base path to generate files (package will add structure)
     * @param generateParsers to generate parsers or not
     * @param packageName java package name of target objects
     * @throws IOException
     */
    public void generateClassFiles(String infile, String outputPath, boolean generateParsers, String packageName) throws IOException {
        Swagger swagger = new SwaggerParser().read(infile);
        Map<String, ServiceModel> servicesMap = getServices(infile, packageName);
        String path = outputPath + "/" + packageName.replaceAll("\\.", "/");
        servicesMap.forEach((name, service) -> {
            try {
                boolean mkdir = new File(path + "/controllers").mkdirs();
                File outFile = new File(path + "/controllers/" + service.getName() + "Controller.java");
                try(FileWriter writer = new FileWriter(outFile)){
                    serviceTemplate.execute(writer, service).flush();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        if (generateParsers){
            swagger.getDefinitions().forEach((dataName, dataModel) -> {
                try {
                    boolean mkdir = new File(path + "/parsers").mkdirs();
                    File outFile = new File(path + "/parsers/" + dataName + "Parser.java");
                    try(FileWriter writer = new FileWriter(outFile)){
                        parserTemplate.execute(writer, new DefinitionModel(packageName, dataName)).flush();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    /**
     * Generate the play routes file
     * @param inputPath proto file
     * @param outputPathWithFilename full path with filename to output routes to
     * @throws IOException
     */
    public void generateRoutesFiles(String inputPath, String outputPathWithFilename, String packageName) throws IOException {
        File file = new File(inputPath);
        ServiceList serviceList = new ServiceList(getServices(inputPath, packageName).values());
        File outFile = new File(outputPathWithFilename);
        try(FileWriter writer = new FileWriter(outFile)){
            routesTemplate.execute(writer, serviceList).flush();
        }

    }

    /**
     * Generate swagger file from routes class and models package.  Useful for exposing documentation
     * @param modelsPackage package name of models to scan
     * @param routesClassName class name of routes
     * @throws IOException
     */
    public JsonNode generateSwagger(String modelsPackage, String routesClassName) throws IOException,
            ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException,
            IllegalAccessException {
        SwaggerBuilderUtil swaggerBuilderUtil = new SwaggerBuilderUtil(loader);
        Swagger swagger;
        if (routesClassName != null){
            swagger = swaggerBuilderUtil.scan(modelsPackage, routesClassName);
        } else {
            swagger = swaggerBuilderUtil.scan(modelsPackage);
        }
        return mapper().valueToTree(swagger);
    }

    ObjectMapper mapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    /**
     * Generate swagger file from routes class and models package using default routes class name.
     * Useful for exposing documentation
     * @param modelsPackage package name of models to scan
     * @throws IOException
     */
    public JsonNode generateSwagger(String modelsPackage) throws IOException,
            ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException,
            IllegalAccessException {
        return generateSwagger(modelsPackage, null);
    }

    public static class ServiceList {
        List<ServiceModel> services;

        public ServiceList(List<ServiceModel> services) {
            this.services = services;
        }

        public ServiceList(Collection<ServiceModel> services) {
            this.services = new ArrayList<>(services);
        }
    }
}


