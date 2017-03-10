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
import java.security.SecureRandom;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@SpringBootApplication
public class Launch implements CommandLineRunner {
    final static SecureRandom secureRandom = new SecureRandom();

    private String getSaveFile;
    private static final String saveFile = "C:/car/save.json";
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
            getSaveFile = saveFile;
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
//        List<CarView> cars = IntStream.range(0, 20)
//                .mapToObj(i -> Car.random().toCarView())
//                .collect(Collectors.toList());

        List<CarScoreView> carScores = unserialize();

        carScores = evaluate(carScores.stream().map(carScoreView -> carScoreView.car).collect(Collectors.toList()));

        for (int i = 0; i < 10; i++) {

            final List<CarScoreView> collect1 = carScores.stream().sorted((carScore1, carScore2) -> -Float.compare(carScore1.score, carScore2.score))
                    .collect(Collectors.toList());
            final Stream<CarScoreView> limit = collect1.stream()
                    .limit(5);
            final List<CarView> collect = limit.map(carScoreView -> carScoreView.car).collect(Collectors.toList());
            for (int j=collect.size(); j<10; j++) {
                collect.add(Car.random().toCarView());
            }

            System.out.println(carScores);
            final CarView best = collect.get(0);
            final Stream<CarView> carViewStream = collect.parallelStream().map(carView -> croisement(carToArray(best), carToArray(carView)))
                    .map(floats -> toCarView(floats));
            final Stream<CarView> concat = Stream.concat(collect.stream(), carViewStream);

            carScores = evaluate(concat.collect(Collectors.toList()));
        }
        CarScoreView champion = carScores.stream()
                .max((carScore1, carScore2) -> Float.compare(carScore1.score, carScore2.score))
                .get();


        System.out.println("OK");
        LOGGER.info("Mon champion est {}", champion);

        serialize(carScores);
        LOGGER.info("save list");
    }


    private float[] carToArray(CarView carView) {
        return Car.createFrom(carView).coords;
    }

    private float[] croisement(float[] car1, float[] car2) {
        float[] newCar = new float[car1.length];
        for (int i = 0; i < car1.length; i++) {
            if (secureRandom.nextFloat() > 0.5) {
                newCar[i] = car1[i];
            } else {
                newCar[i] = car2[i];
            }
        }
        return newCar;
    }

    public CarView toCarView(float[] carCoords) {
        CarView carView = new CarView();

        IntStream.range(0, 16)
                .forEach(i -> carView.chassi.vecteurs.add(carCoords[i]));
        carView.chassi.densite = carCoords[16];

        carView.wheel1.density = carCoords[17];
        carView.wheel1.radius = carCoords[18];
        carView.wheel1.vertex = Float.valueOf(carCoords[19]).intValue();

        carView.wheel2.density = carCoords[20];
        carView.wheel2.radius = carCoords[21];
        carView.wheel2.vertex = Float.valueOf(carCoords[22]).intValue();

        return carView;
    }

    public void serialize(List<CarScoreView> carScoreViews) throws URISyntaxException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        //Object to JSON in file
        final File resultFile = new File(getSaveFile);
        mapper.writeValue(resultFile, new CarViewList().setCarViewList(carScoreViews));
    }

    public List<CarScoreView> unserialize() throws URISyntaxException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        String jsonInString = "{'name' : 'mkyong'}";

//JSON from file to Object
        return mapper.readValue(new File(getSaveFile), CarViewList.class).getCarViewList();
    }
}
