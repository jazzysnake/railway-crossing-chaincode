package hu.bme;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.log4j.Logger;
import org.junit.jupiter.api.Test;

class RequestTest {

    public static Logger log = Logger.getLogger(RequestTest.class.getName());

    @Test
    void jsonSerializableRequest() {
        Request request = new Request("0001", "01", "01", RequesterRole.TRAIN, true, true);
        String json = request.toJSONString();
        log.info(json);
        Request newRequest = Request.fromJSONString(json);
        log.info(newRequest.toJSONString());
        assertEquals(request, newRequest);
    }
}
