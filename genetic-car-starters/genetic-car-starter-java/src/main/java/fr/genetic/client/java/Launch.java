package fr.genetic.client.java;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.genetic.client.java.algo.Car;
import fr.genetic.client.java.api.CarScoreView;
import fr.genetic.client.java.api.CarView;
import fr.genetic.client.java.api.CarViewList;
import fr.genetic.client.java.api.Team;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URLClassLoader;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@SpringBootApplication
public class Launch implements CommandLineRunner {

    private String getSaveFile;
    private static final String saveFile = "save.json";
    private static final Logger LOGGER = LoggerFactory.getLogger(Launch.class);

    @Value("${genetic.server.host}")
    private String host;

    @Autowired
    private RestTemplate restTemplate;

    private Team team = Team.ORANGE;


    public static void main(String[] args) {
        SpringApplication.run(Launch.class, args);
    }

    @Override
    public void run(String... args) {
        try {
            getSaveFile = URLClassLoader.getSystemClassLoader().getResource(saveFile).toURI().toString();
            doMyAlgo();
        } catch (Exception restException) {
            LOGGER.error(restException.getMessage());
        }
    }

    private List<CarScoreView> evaluate(List<CarView> cars) {
        String url = host + "/simulation/evaluate/" + team.name();
        return restTemplate.exchange(url, HttpMethod.POST,
                new HttpEntity(cars), new ParameterizedTypeReference<List<CarScoreView>>() {
                }).getBody();
    }

    protected void doMyAlgo() throws IOException, URISyntaxException {

//        unserialize()

        List<CarView> cars = IntStream.range(0, 20)
                .mapToObj(i -> Car.random().toCarView())
                .collect(Collectors.toList());


        List<CarScoreView> carScores = evaluate(cars);
        // selection
        // croisement
        // elitisme
        CarScoreView champion = carScores.stream()
                .max((carScore1, carScore2) -> Float.compare(carScore1.score, carScore2.score))
                .get();


//        final Stream<CarScoreView> tenMostScoredCars = carScores.stream().sorted((carScore1, carScore2) -> Float.compare(carScore1.score, carScore2.score))
//                .limit(10);

        System.out.println("OK");
        LOGGER.info("Mon champion est {}", champion);

        serialize(carScores);
        LOGGER.info("save list");
    }

    public void serialize(List<CarScoreView> carScoreViews) throws URISyntaxException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        //Object to JSON in file
        mapper.writeValue(new File(getSaveFile), new CarViewList().setCarViewList(carScoreViews));
    }

    public List<CarScoreView> unserialize() throws URISyntaxException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        String jsonInString = "{'name' : 'mkyong'}";

//JSON from file to Object
        return mapper.readValue(new File(getSaveFile), CarViewList.class).getCarViewList();
    }
}
