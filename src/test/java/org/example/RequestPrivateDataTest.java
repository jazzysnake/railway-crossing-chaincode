package org.example;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.log4j.Logger;
import org.junit.jupiter.api.Test;

class RequestPrivateDataTest {
    public static Logger log = Logger.getLogger(RequestPrivateData.class.getName());

    @Test
    void jsonSerializableRequestPrivateData(){
        String requestId = "123";
        String laneId = "123";
        String crossingId = "123";
        String clientId = "123";
        RequestPrivateData pvdata = new RequestPrivateData(requestId, laneId,crossingId,clientId);
        String json = pvdata.toJSONString();
        log.info(json);
        RequestPrivateData newPvData = RequestPrivateData.fromJSONString(json);
        log.info(newPvData.toJSONString());
        assertEquals(pvdata, newPvData);
    }
      
}
