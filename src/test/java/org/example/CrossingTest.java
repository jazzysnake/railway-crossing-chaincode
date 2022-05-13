package org.example;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.log4j.Logger;
import org.junit.jupiter.api.Test;

class CrossingTest {
    public static Logger log = Logger.getLogger(Crossing.class.getName());

    @Test
    void jsonSerializableCrossing(){
        String[] laneIds = {"123", "456"};
        Crossing crossing = new Crossing("123",laneIds, CrossingState.FREE_TO_CROSS,false);
        String json = crossing.toJSONString();
        log.info(json);
        Crossing newCrossing = Crossing.fromJSONString(json);
        log.info(newCrossing.toJSONString());
        assertEquals(crossing, newCrossing);
    }
      
}
