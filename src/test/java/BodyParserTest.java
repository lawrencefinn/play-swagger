import akka.NotUsed;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import akka.util.ByteStringBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.caboose.api.models.Category;
import io.caboose.api.models.Pet;
import io.caboose.api.parsers.PetParser;
import io.caboose.swagger.SwaggerMapper;
import org.junit.*;

import play.Application;
import play.inject.guice.GuiceApplicationBuilder;
import play.libs.F;
import play.libs.streams.Accumulator;
import play.mvc.Http;
import play.mvc.Result;
import play.test.*;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import static play.test.Helpers.*;
import static org.junit.Assert.*;
public class BodyParserTest extends WithApplication {
    @Override
    protected Application provideApplication() {
        return new GuiceApplicationBuilder().build();
    }

    ObjectMapper mapper = SwaggerMapper.mapper();

    @Test
    public void testTextOutput() throws NoSuchMethodException, ExecutionException, InterruptedException, IOException {
        Category cat = new Category().id(34L).name("category");

        Pet pet = new Pet();
        pet.setId(1234L);
        pet.setCategory(cat);
        pet.setStatus(Pet.StatusEnum.PENDING);
        pet.setPhotoUrls(Arrays.asList("a", "b"));
        JsonNode json = mapper.valueToTree(pet);
        Http.RequestImpl post = fakeRequest().bodyJson(json).path("/test").method("POST").build();
        PetParser petParser = app.injector().instanceOf(PetParser.class);
        Accumulator<ByteString, F.Either<Result, Pet>> apply = petParser.apply(post);
        ByteString result = ByteString.createBuilder().putBytes(json.toString().getBytes(Charset.forName("UTF-8"))).result();
        Source<ByteString, NotUsed> single = Source.single(result);
        CompletionStage<F.Either<Result, Pet>> run = apply.run(single, mat);
        Pet output = run.toCompletableFuture().get().right.get();
        assertEquals(pet.getId(), output.getId());
        assertEquals(pet.getName(), output.getName());
        assertEquals(pet.getCategory(), output.getCategory());
        assertEquals(pet.getStatus(), output.getStatus());
        assertEquals(pet.getPhotoUrls().get(0), output.getPhotoUrls().get(0));
        assertEquals(pet.getPhotoUrls().get(1), output.getPhotoUrls().get(1));
    }
}
