package io.caboose.swagger.utils;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.caboose.swagger.SwaggerService;
import io.caboose.swagger.models.ErrorModel;
import io.swagger.models.*;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.PathParameter;
import io.swagger.models.parameters.QueryParameter;
import io.swagger.models.properties.*;
import org.joda.time.DateTime;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import play.libs.F;
import scala.Tuple3;
import scala.runtime.AbstractFunction1;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;
import static org.reflections.ReflectionUtils.*;

/**
 * Internal class for building swagger defs from existing code
 */
public class SwaggerBuilderUtil {
    ClassLoader classLoader;

    /**
     * Default constructor
     * @param classLoader class loader to use to instantiate objects
     */
    public SwaggerBuilderUtil(ClassLoader classLoader){
        this.classLoader = classLoader;
    }

    /**
     * Build swagger definition using models scanned from package.  Assume routes class is routes.Routes
     * @param modelPackage name of package where models live
     * @return
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws InstantiationException
     */
    public Swagger scan(String modelPackage) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Swagger swagger = new Swagger();
        List<Tuple3<String, String, String>> routes = getRoutes();
        swagger.setPaths(getPathsFromRoutes(routes));
        getModels(modelPackage, swagger);
        return swagger;
    }

    /**
     * Build swagger definition using models scanned from package and routes class
     * @param modelPackage name of package where models live
     * @param routesClassName class name of compiled routes class
     * @return
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws InstantiationException
     */
    public Swagger scan(String modelPackage, String routesClassName) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Swagger swagger = new Swagger();
        List<Tuple3<String, String, String>> routes = getRoutes(routesClassName);
        swagger.setPaths(getPathsFromRoutes(routes));
        getModels(modelPackage, swagger);
        return swagger;
    }

    /**
     * Get swagger property based on class or field
     * @param fieldOrClass Either object representing a field or class
     * @return Property
     * @throws ClassNotFoundException
     */
    protected Property classToProperty(F.Either<Field, Class<?>> fieldOrClass) throws ClassNotFoundException {
        Class<?> fieldClass;
        if (fieldOrClass.left.isPresent()){
            fieldClass = fieldOrClass.left.get().getType();
        } else {
            fieldClass = fieldOrClass.right.get();
        }
        Property prop;
        if (fieldClass.equals(Long.class)) {
            prop = PropertyBuilder.build("integer", "int64", null);
        } else if (fieldClass.equals(DateTime.class)) {
            prop = PropertyBuilder.build("string", "date-time", null);
        } else if (fieldClass.equals(List.class)) {
            Field field = fieldOrClass.left.get();
            ParameterizedType pType = (ParameterizedType) field.getGenericType();
            Class<?> genericClass = classLoader.loadClass(pType.getActualTypeArguments()[0].getTypeName());
            prop = new ArrayProperty().items(classToProperty(F.Either.Right(genericClass)));
        } else if (fieldClass.isEnum()){
            prop = new StringProperty()._enum(Arrays.stream(fieldClass.getEnumConstants()).map(v -> v.toString()).collect(Collectors.toList()));
        } else {
            prop = PropertyBuilder.build(fieldClass.getSimpleName().toLowerCase(), null , null);
        }
        if (prop == null){
            prop = new RefProperty();
            ((RefProperty)prop).set$ref(fieldClass.getSimpleName());
        }
        return prop;
    }

    /**
     * Scan for models in package
     * @param modelPackage name of package where models reside
     * @param swagger swagger object
     * @throws ClassNotFoundException
     */
    protected void getModels(String modelPackage, Swagger swagger) throws ClassNotFoundException {
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forPackage(modelPackage))
                .setScanners(new SubTypesScanner(false))
                .filterInputsBy(new FilterBuilder().includePackage(modelPackage))
                .addClassLoader(classLoader));
        for (String className : reflections.getAllTypes()) {
            ModelImpl model = new ModelImpl();
            model.setType("object");
            Class<?> aClass = classLoader.loadClass(className);
            Set<Field> fields = getAllFields(aClass, withAnnotation(JsonProperty.class));
            for (Field field : fields) {
                JsonProperty annotation = field.getAnnotation(JsonProperty.class);
                model.addProperty(annotation.value(), classToProperty(F.Either.Left(field)));
            }
            swagger.addDefinition(aClass.getSimpleName(), model);
        }

    }

    /**
     * Parse routes description to path objects
     * @param routes routes description
     * @return map of url to path objects
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    protected Map<String, Path> getPathsFromRoutes(List<Tuple3<String, String, String>> routes) throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        Map<String, Path> collect = new HashMap<>();
        for (Tuple3<String, String, String> route : routes) {

            String routeUrl = route._2().replaceAll("\\$", "{").replaceAll("<\\[\\^/\\]\\+>", "}");
            if (!collect.containsKey(routeUrl)){
                collect.put(routeUrl, new Path());
            }
            Operation op = getOperationFromFunction(route._3());
            switch (HttpMethod.valueOf(route._1())){
                case GET:
                    collect.get(routeUrl).get(op);
                break;
                case POST:
                    collect.get(routeUrl).post(op);
                break;
                case PUT:
                    collect.get(routeUrl).put(op);
                break;
                case DELETE:
                    collect.get(routeUrl).delete(op);
                break;
            }
        }
        return collect;
    }

    /**
     * Extract querystring and url params from route definition
     * @param functionName function name from routes definition
     * @return list of Type, Name, isQueryString
     */
    protected List<Tuple3<String, String, Boolean>> getParamsFromRoute(String functionName) {
        String params = functionName.substring(functionName.indexOf("(") + 1);
        params = params.substring(0, params.indexOf(")"));
        if (params.trim().isEmpty()){
            return Collections.emptyList();
        }
        return Arrays.stream(params.split(","))
                .map(param -> {
                    String[] paramSplit = param.split("\\?");
                    param = paramSplit[0];
                    String[] paramTypeName = param.split(":");
                    return new Tuple3<>(paramTypeName[1].trim(), paramTypeName[0].trim(), (paramSplit.length > 1));
                }).collect(Collectors.toList());

    }

    /**
     * Create classes from tuple3 list of params
     * @param params list of tuple3 of Type, Name, isQueryString
     * @return
     * @throws ClassNotFoundException
     */
    protected List<Class<?>> getClassesFromParamsList(List<Tuple3<String, String, Boolean>> params) throws ClassNotFoundException {
        List<Class<?>> list = new ArrayList<>(params.size());
        for (int i = 0; i < params.size(); i++) {
            list.add(classLoader.loadClass("java.lang." + params.get(i)._1()));
        }
        return list;
    }

    /**
     * Add output to operation
     * @param op operation
     * @param outputClass class to add, either pojo or exception
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    protected void addOutput(Operation op, Class<?> outputClass) throws IllegalAccessException, InstantiationException {
        if (ErrorModel.class.isAssignableFrom(outputClass)){
            ErrorModel errorModel = (ErrorModel) outputClass.newInstance();
            op.addResponse("" + errorModel.getStatusCode(), new Response().description(errorModel.getMessage()));
        } else {
            op.addProduces("application/json");
            RefProperty prop = new RefProperty();
            prop.set$ref(outputClass.getSimpleName());
            op.addResponse("200", new Response().schema(prop).description("success"));
        }

    }

    /**
     * Build operation from function in routes file
     * @param functionName function definition in routes file
     * @return Swagger operation
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    protected Operation getOperationFromFunction(String functionName) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InstantiationException {
        Operation op = new Operation();
        List<Tuple3<String, String, Boolean>> paramsFromRoute = getParamsFromRoute(functionName);
        functionName = functionName.substring(0, functionName.indexOf("("));
        String className = functionName.substring(0, functionName.lastIndexOf(".") );
        functionName = functionName.substring(functionName.lastIndexOf(".") + 1);
        op.setOperationId(functionName);
        Class<?> aClass = classLoader.loadClass(className);
        Method method = aClass.getMethod(functionName, getClassesFromParamsList(paramsFromRoute).toArray(new Class[0]));
        for (int i = 0; i < method.getAnnotations().length; i++) {
            Annotation annotation = method.getAnnotations()[i];
            if (annotation.annotationType().equals(SwaggerService.Input.class)){
                Class<?> inputClass = ((SwaggerService.Input)annotation).value();
                op.addConsumes("application/json");
                BodyParameter bodyParameter = new BodyParameter();
                bodyParameter.setIn("body");
                bodyParameter.setName("body");
                bodyParameter.setRequired(true);
                RefModel model = new RefModel();
                model.set$ref(inputClass.getSimpleName());
                bodyParameter.setSchema(model);
                op.addParameter(bodyParameter);
            } else if (annotation.annotationType().equals(SwaggerService.Output.class)){
                Class<?> outputClass = ((SwaggerService.Output)annotation).value();
                addOutput(op, outputClass);
            } else if (annotation.annotationType().equals(SwaggerService.Outputs.class)){
                for (SwaggerService.Output output : ((SwaggerService.Outputs) annotation).value()) {
                    Class<?> outputClass = output.value();
                    addOutput(op, outputClass);
                }
            }
        }
        paramsFromRoute.forEach(param -> {
            if (param._3()){
                op.addParameter(new QueryParameter().name(param._2()).type(param._1()));
            } else {
                op.addParameter(new PathParameter().name(param._2()).type(param._1()));
            }
        });

        return op;

    }


    /**
     * Get routes definition from default routes class routes.Routes
     * @return List of routes Method, URL, FunctionName
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    protected List<Tuple3<String, String, String>> getRoutes() throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        return getRoutes("router.Routes");
    }

    /**
     * Get routes definition
     * @param routesClass class name of compiled routes
     * @return List of routes Method, URL, FunctionName
     * @throws ClassNotFoundException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    protected List<Tuple3<String, String, String>> getRoutes(String routesClass) throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        Class<?> aClass = classLoader.loadClass(routesClass);
        Method method = aClass.getDeclaredMethod("documentation");
        Constructor<?> constructor = aClass.getConstructors()[0];
        Object[] args = new Object[constructor.getParameterCount()];
        for(int i = 0; i < args.length; i++){
            args[i] = null;
        }
        Object o = constructor.newInstance(args);
        scala.collection.immutable.List<scala.Tuple3<String, String, String>> invoke = (scala.collection.immutable.List<scala.Tuple3<String, String, String>>) method.invoke(o);
        List<Tuple3<String, String, String>> routesList = new ArrayList<>(invoke.size());
        invoke.foreach(new AbstractFunction1<Tuple3<String, String, String>, Object>() {
            @Override
            public Object apply(Tuple3<String, String, String> tupple) {
                routesList.add(tupple);
                return null;
            }
        });
        return routesList;
    }

}
