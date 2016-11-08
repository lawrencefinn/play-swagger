package io.caboose.swagger.parsers;

import akka.util.ByteString;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import io.caboose.swagger.SwaggerMapper;
import play.api.http.HttpConfiguration;
import play.http.HttpErrorHandler;
import play.mvc.BodyParser;
import play.mvc.Http;

/**
 * Abstract bodyparser class to parse swagger.
 * To use:
 * Create class and extend ProtobufParser<T> with T being the swagger model class
 * Implement getClazz to return T.class.
 * Implement constructor with @Inject
 *
 * See test/java/io/caboose/api/parsers/PetParser.java
 */
public abstract class SwaggerParser<T> extends BodyParser.BufferingBodyParser<T> {


    ObjectMapper mapper = SwaggerMapper.mapper();
    Class<T> clazz;

    protected abstract Class<T> getClazz();

    @Inject
    protected SwaggerParser(HttpConfiguration httpConfiguration, HttpErrorHandler errorHandler, String errorMessage) {
        super(httpConfiguration.parser().maxMemoryBuffer(), errorHandler, errorMessage);
    }

    @Override
    protected T parse(Http.RequestHeader request, ByteString bytes) throws Exception {
        JsonNode json = play.libs.Json.parse(bytes.iterator().asInputStream());
        return mapper.convertValue(json, getClazz());
    }
}
