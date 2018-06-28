package com.bimal.mag_positioning;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by Bimal Bhattarai on 9/28/2017.
 */

public class MovingAverage {
    private static float filterSum = 0;
    private static float filterResult = 0;
    private final static Queue<Float> maWindow = new LinkedList<Float>();

    public static float movingAverage(float accModule, int length) {

        filterSum += accModule;
        maWindow.add(accModule);
        if (maWindow.size() > length) {
            float head = maWindow.remove();
            filterSum -= head;
        }
        if (! maWindow.isEmpty()) {
            filterResult = filterSum / maWindow.size();
        }
        return filterResult;
    }
}
