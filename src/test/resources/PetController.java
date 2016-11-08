package io.caboose.api.controllers;

import io.caboose.api.models.*;
import io.caboose.api.parsers.*;
import io.caboose.swagger.models.ErrorModel;
import play.mvc.*;
import io.caboose.swagger.SwaggerController;
import io.caboose.swagger.SwaggerService;

@SwaggerService.Service()
public class PetController extends SwaggerController {
    @SwaggerService.Output(ErrorAddPetError0.class)
    @SwaggerService.Input(.class)
    @BodyParser.Of(PetParser.class)
    public Result addPet() {
        Pet input = request().body().as(Pet.class);
        return SwaggerService.ok(input);
        //return SwaggerService.error(new ErrorAddPetError0());
    }

    @SwaggerService.Output(ErrorDeletePetError0.class)
    @SwaggerService.Output(ErrorDeletePetError1.class)
    public Result deletePet(String petId) {
        //return SwaggerService.error(new ErrorDeletePetError0());
        //return SwaggerService.error(new ErrorDeletePetError1());
    }

    @SwaggerService.Output(Pet.class)
    @SwaggerService.Output(ErrorFindPetsByStatusError0.class)
    public Result findPetsByStatus(String status) {
        //return SwaggerService.error(new ErrorFindPetsByStatusError0());
    }

    @SwaggerService.Output(Pet.class)
    @SwaggerService.Output(ErrorFindPetsByTagsError0.class)
    public Result findPetsByTags(String tags) {
        //return SwaggerService.error(new ErrorFindPetsByTagsError0());
    }

    @SwaggerService.Output(Pet.class)
    @SwaggerService.Output(ErrorGetPetByIdError0.class)
    @SwaggerService.Output(ErrorGetPetByIdError1.class)
    public Result getPetById(String petId) {
        //return SwaggerService.error(new ErrorGetPetByIdError0());
        //return SwaggerService.error(new ErrorGetPetByIdError1());
    }

    @SwaggerService.Output(ErrorUpdatePetError0.class)
    @SwaggerService.Output(ErrorUpdatePetError1.class)
    @SwaggerService.Output(ErrorUpdatePetError2.class)
    @SwaggerService.Input(.class)
    @BodyParser.Of(PetParser.class)
    public Result updatePet() {
        Pet input = request().body().as(Pet.class);
        return SwaggerService.ok(input);
        //return SwaggerService.error(new ErrorUpdatePetError0());
        //return SwaggerService.error(new ErrorUpdatePetError1());
        //return SwaggerService.error(new ErrorUpdatePetError2());
    }

    @SwaggerService.Output(ErrorUpdatePetWithFormError0.class)
    public Result updatePetWithForm(String petId) {
        //return SwaggerService.error(new ErrorUpdatePetWithFormError0());
    }

    @SwaggerService.Output(ApiResponse.class)
    public Result uploadFile(String petId) {
    }


    public static class ErrorAddPetError0 extends ErrorModel {
        public ErrorAddPetError0() {
            super(405, "Invalid input", "error0");
        }
    }

    public static class ErrorDeletePetError0 extends ErrorModel {
        public ErrorDeletePetError0() {
            super(400, "Invalid ID supplied", "error0");
        }
    }

    public static class ErrorDeletePetError1 extends ErrorModel {
        public ErrorDeletePetError1() {
            super(404, "Pet not found", "error1");
        }
    }

    public static class ErrorFindPetsByStatusError0 extends ErrorModel {
        public ErrorFindPetsByStatusError0() {
            super(400, "Invalid status value", "error0");
        }
    }

    public static class ErrorFindPetsByTagsError0 extends ErrorModel {
        public ErrorFindPetsByTagsError0() {
            super(400, "Invalid tag value", "error0");
        }
    }

    public static class ErrorGetPetByIdError0 extends ErrorModel {
        public ErrorGetPetByIdError0() {
            super(400, "Invalid ID supplied", "error0");
        }
    }

    public static class ErrorGetPetByIdError1 extends ErrorModel {
        public ErrorGetPetByIdError1() {
            super(404, "Pet not found", "error1");
        }
    }

    public static class ErrorUpdatePetError0 extends ErrorModel {
        public ErrorUpdatePetError0() {
            super(400, "Invalid ID supplied", "error0");
        }
    }

    public static class ErrorUpdatePetError1 extends ErrorModel {
        public ErrorUpdatePetError1() {
            super(404, "Pet not found", "error1");
        }
    }

    public static class ErrorUpdatePetError2 extends ErrorModel {
        public ErrorUpdatePetError2() {
            super(405, "Validation exception", "error2");
        }
    }

    public static class ErrorUpdatePetWithFormError0 extends ErrorModel {
        public ErrorUpdatePetWithFormError0() {
            super(405, "Invalid input", "error0");
        }
    }



}


