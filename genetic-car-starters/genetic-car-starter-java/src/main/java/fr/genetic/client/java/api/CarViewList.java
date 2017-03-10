package fr.genetic.client.java.api;

import java.util.List;

/**
 * Created by PC on 10/03/2017.
 */
public class CarViewList {
    public List<CarScoreView> carViewList;

    public List<CarScoreView> getCarViewList() {
        return carViewList;
    }

    public CarViewList setCarViewList(List<CarScoreView> carViewList) {
        this.carViewList = carViewList;
        return this;
    }
}
