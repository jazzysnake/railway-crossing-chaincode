package org.example;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.log4j.Logger;
import org.junit.jupiter.api.Test;

public class CrossingTest {
    public static Logger log = Logger.getLogger(Crossing.class.getName());

    @Test
    public void jsonSerializableCrossing(){
        String[] laneIds = {"123", "456"};
        Crossing crossing = new Crossing("123",laneIds, CrossingState.FREE_TO_CROSS,false);
        String json = crossing.toJSONString();
        log.info(json);
        Crossing newCrossing = Crossing.fromJSONString(json);
        log.info(newCrossing.toJSONString());
        assertTrue(crossing.equals(newCrossing));
    }
      
}
