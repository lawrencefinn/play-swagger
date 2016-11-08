package io.caboose.swagger;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.caboose.swagger.models.ErrorModel;
import play.mvc.Result;
import play.mvc.Results;

import java.lang.annotation.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Annotations and function to handle protobuf input and output
 */
public interface SwaggerService {

    static Map<String, ? extends ErrorModel> errorMap = new HashMap<>();

    ObjectMapper mapper = new ObjectMapper();
    /**
     * Annotation to express output as protobuf class
     */
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @Repeatable(Outputs.class)
    @interface Output {
        Class<?> value();
    }

    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @interface Outputs {
        Output[] value();
    }

    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @interface Error {
        int httpStatus();
        String message();
        String code();
    }

    /**
     * Annotation to express input as protobuf class.  Used mostly by dynamic router
     */
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @interface Input {
        Class<?> value();
    }

    /**
     * Annotation to describe GRPC style service.  PackageName is an alias to expose for routing
     */
    @Target({ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @interface Service {
    }

    /**
     * Return a play result from protobuf object.  Should be of style return ProtobufService.ok(protobufObject);
     * @param input protobuf object
     * @param <T>
     * @return
     */
    public static <T> Result ok(T input) {
        return Results.ok(mapper.valueToTree(input).toString());
    }

    /**
     * Return a play result from protobuf object.  Should be of style return ProtobufService.ok(protobufObject);
     * @param input error object
     * @param <T>
     * @return
     */
    public static <T extends ErrorModel> Result error(Class<T> input) throws IllegalAccessException, InstantiationException {
        ErrorModel errorModel = errorMap.get(input.getName());
        if (errorModel != null){
            return Results.status(errorModel.getStatusCode(), errorModel.getMessage());
        } else {
            T out = input.newInstance();
            return Results.status(out.getStatusCode(), out.getMessage());
        }
    }

    /**
     * Return a play result from protobuf object.  Should be of style return ProtobufService.ok(protobufObject);
     * @param input error object
     * @param <T>
     * @return
     */
    public static <T extends ErrorModel> Result error(T input) {
        return Results.status(input.getStatusCode(), input.getMessage());
    }
}
