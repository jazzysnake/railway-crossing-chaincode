package org.example;

import org.junit.jupiter.api.Test;
import org.apache.log4j.Logger;

import static org.junit.jupiter.api.Assertions.*;

class LaneTest {
    public static Logger log = Logger.getLogger(CrossingContractTest.class.getName());

    @Test
    void jsonSerializableLane(){
        Lane lane = new Lane("123", "123", 1, 0, false);
        String json = lane.toJSONString();
        log.info(json);
        Lane newLane = Lane.fromJSONString(json);
        log.info(newLane.toJSONString());
        assertEquals(lane, newLane);
    }
}
