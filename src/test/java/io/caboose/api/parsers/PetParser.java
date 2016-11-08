package io.caboose.api.parsers;

import io.caboose.api.models.*;

import com.google.inject.Inject;
import io.caboose.swagger.parsers.SwaggerParser;
import play.api.http.HttpConfiguration;
import play.http.HttpErrorHandler;

public class PetParser extends SwaggerParser<Pet> {

    @Override
    protected Class<Pet> getClazz() {
        return Pet.class;
    }

    @Inject
    protected PetParser(HttpConfiguration httpConfiguration, HttpErrorHandler errorHandler, String errorMessage
                          ) throws NoSuchMethodException  {
        super(httpConfiguration, errorHandler, errorMessage);
    }

}
