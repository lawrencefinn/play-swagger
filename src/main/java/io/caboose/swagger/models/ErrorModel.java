package io.caboose.swagger.models;

import io.caboose.swagger.SwaggerService;

/**
 * Model representing error messages
 */
public class ErrorModel {
    int statusCode;
    String message;
    String code;

    /**
     * Construct error model
     * @param statusCode http status
     * @param message error message
     * @param code arbitrary code representing error
     */
    public ErrorModel(int statusCode, String message, String code) {
        this.statusCode = statusCode;
        this.message = message;
        this.code = code;
    }

    public ErrorModel(){
        this(200, "", "");
        SwaggerService.Error annotation = getClass().getAnnotation(SwaggerService.Error.class);
        if (annotation != null) {
            statusCode = annotation.httpStatus();
            message = annotation.message();
            code = annotation.code();
        }
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getClassName(){
        return getClass().getSimpleName();
    }

    public String getCamelCode(){
        return code.substring(0, 1).toUpperCase() + code.substring(1);
    }

}
