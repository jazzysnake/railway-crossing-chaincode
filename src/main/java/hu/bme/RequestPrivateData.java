package hu.bme;

import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;
import org.json.JSONObject;

@DataType()
public class RequestPrivateData {

    public static final String COLLECTION_NAME = "REQUEST_PRIVDATA";
    @Property()
    private String requestId;
    @Property()
    private String laneId;
    @Property()
    private String crossingId;
    @Property()
    private String clientId;

    public RequestPrivateData(String requestId, String laneId, String crossingId, String clientId) {
        this.requestId = requestId;
        this.laneId = laneId;
        this.crossingId = crossingId;
        this.clientId = clientId;
    }

    public String toJSONString() {
        return new JSONObject(this).toString();
    }

    public static RequestPrivateData fromJSONString(String json) {
        JSONObject jsonObject = new JSONObject(json);
        String requestId = jsonObject.getString("requestId");
        String laneId = jsonObject.getString("laneId");
        String crossingId = jsonObject.getString("crossingId");
        String clientId = jsonObject.getString("clientId");
        return new RequestPrivateData(requestId, laneId, crossingId, clientId);
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getLaneId() {
        return laneId;
    }

    public void setLaneId(String laneId) {
        this.laneId = laneId;
    }

    public String getCrossingId() {
        return crossingId;
    }

    public void setCrossingId(String crossingId) {
        this.crossingId = crossingId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((clientId == null) ? 0 : clientId.hashCode());
        result = prime * result + ((crossingId == null) ? 0 : crossingId.hashCode());
        result = prime * result + ((laneId == null) ? 0 : laneId.hashCode());
        result = prime * result + ((requestId == null) ? 0 : requestId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        RequestPrivateData other = (RequestPrivateData) obj;
        if (clientId == null) {
            if (other.clientId != null)
                return false;
        } else if (!clientId.equals(other.clientId))
            return false;
        if (crossingId == null) {
            if (other.crossingId != null)
                return false;
        } else if (!crossingId.equals(other.crossingId))
            return false;
        if (laneId == null) {
            if (other.laneId != null)
                return false;
        } else if (!laneId.equals(other.laneId))
            return false;
        if (requestId == null) {
            if (other.requestId != null)
                return false;
        } else if (!requestId.equals(other.requestId))
            return false;
        return true;
    }

}
