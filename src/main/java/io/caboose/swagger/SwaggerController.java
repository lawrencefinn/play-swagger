package io.caboose.swagger;

import io.caboose.swagger.models.ErrorModel;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Results;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * Controller wrapper to expose error to result function
 */
public abstract class SwaggerController extends Controller {


    protected Result errorToResult(ErrorModel error){
        return Results.status(error.getStatusCode(), error.getMessage());
    }

}
