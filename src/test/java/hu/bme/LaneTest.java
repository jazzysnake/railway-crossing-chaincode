package hu.bme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.log4j.Logger;
import org.junit.jupiter.api.Test;

class LaneTest {
    public static Logger log = Logger.getLogger(CrossingContractTest.class.getName());

    @Test
    void jsonSerializableLane() {
        Lane lane = new Lane("123", "123", 1, 0, false);
        String json = lane.toJSONString();
        log.info(json);
        Lane newLane = Lane.fromJSONString(json);
        log.info(newLane.toJSONString());
        assertEquals(lane, newLane);
    }

    @Test
    void laneIsFree() {
        Lane lane = new Lane("", "", 1, 0, false);
        assertTrue(lane.isFree());
        lane.setOccupied(1);
        assertFalse(lane.isFree());
        lane.setOccupied(0);
        lane.setPriorityLock(true);
        assertFalse(lane.isFree());
    }
}
