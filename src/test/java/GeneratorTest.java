import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import io.caboose.swagger.utils.SwaggerServiceGeneratorUtil;
import org.apache.commons.io.FileUtils;
import org.junit.*;

import static org.junit.Assert.*;


public class GeneratorTest {

    @Test
    public void generateServiceTest() throws IOException, URISyntaxException {
        SwaggerServiceGeneratorUtil generator = new SwaggerServiceGeneratorUtil();
        Path tempDirectory = Files.createTempDirectory(Paths.get("/tmp"), null);
        String path = getClass().getClassLoader().getResource("swagger.json").getPath();
        generator.generateClassFiles(path, tempDirectory.toString(), true, "io.caboose.api");
        String outputController = String.join("\n",
                Files.readAllLines(Paths.get(tempDirectory + "/io/caboose/api/controllers/PetController.java"))
        );
        String expectedController = String.join("\n",
                Files.readAllLines(Paths.get(getClass().getClassLoader().getResource("PetController.java").toURI()))
        );
        assertEquals(expectedController, outputController);
        String outputParser = String.join("\n",
                Files.readAllLines(Paths.get(tempDirectory + "/io/caboose/api/parsers/PetParser.java"))
        );
        String expectedParser = String.join("\n",
                Files.readAllLines(Paths.get(getClass().getClassLoader().getResource("PetParser.java.txt").toURI()))
        );
        assertEquals(expectedParser, outputParser);
        FileUtils.deleteDirectory(tempDirectory.toFile());

    }



    @Test
    public void generateRoutes() throws IOException, URISyntaxException {
        SwaggerServiceGeneratorUtil generator = new SwaggerServiceGeneratorUtil();
        Path tempDirectory = Files.createTempDirectory(Paths.get("/tmp"), null);
        generator.generateRoutesFiles(getClass().getClassLoader().getResource("swagger.json").getFile(),
                tempDirectory.toString() + "/routes",
                "io.caboose.api"
        );
        String outputRoutes = String.join("\n",
                Files.readAllLines(Paths.get(tempDirectory + "/routes"))
        );
        String expectedRoutes = String.join("\n",
                Files.readAllLines(Paths.get(getClass().getClassLoader().getResource("routes").toURI()))
        );
        assertEquals(expectedRoutes, outputRoutes);
        FileUtils.deleteDirectory(tempDirectory.toFile());
    }



}
