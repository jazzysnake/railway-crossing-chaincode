/*
 * SPDX-License-Identifier: Apache-2.0
 */

package hu.bme;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;
import org.json.JSONObject;

@DataType()
public class Crossing {

    @Property()
    private String id;

    @Property()
    private String[] laneIds;

    @Property()
    private CrossingState state;

    @Property()
    private boolean priorityLock;

    @Property()
    private long validUntil;

    public static final String TYPE = "CROSSING";

    public Crossing(String id, String[] laneIds, CrossingState state, boolean priorityLock, long validUntil) {
        this.id = id;
        this.laneIds = laneIds;
        this.state = state;
        this.priorityLock = priorityLock;
        this.validUntil = validUntil;
    }

    public String toJSONString() {
        return new JSONObject(this).toString();
    }

    public static Crossing fromJSONString(String json) {
        JSONObject jsonObject = new JSONObject(json);
        String id = jsonObject.getString("id");
        CrossingState crossingState = jsonObject.getEnum(CrossingState.class, "state");
        List<Object> laneIdObjetList = jsonObject.getJSONArray("laneIds").toList();
        boolean priorityLock = jsonObject.getBoolean("priorityLock");
        long validUntil = jsonObject.getLong("validUntil");
        ArrayList<String> laneIdList = new ArrayList<>();
        laneIdList.addAll((List<String>) (Object) laneIdObjetList);
        String[] laneIds = laneIdList.stream().toArray(String[]::new);
        return new Crossing(id, laneIds, crossingState, priorityLock, validUntil);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String[] getLaneIds() {
        return laneIds;
    }

    public void setLaneIds(String[] laneIds) {
        this.laneIds = laneIds;
    }

    public CrossingState getState() {
        return state;
    }

    public void setState(CrossingState state) {
        this.state = state;
    }

    public boolean isPriorityLock() {
        return priorityLock;
    }

    public void setPriorityLock(boolean priorityLock) {
        this.priorityLock = priorityLock;
    }

    public long getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(long validUntil) {
        this.validUntil = validUntil;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + Arrays.hashCode(laneIds);
        result = prime * result + (priorityLock ? 1231 : 1237);
        result = prime * result + ((state == null) ? 0 : state.hashCode());
        result = prime * result + (int) (validUntil ^ (validUntil >>> 32));
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
        Crossing other = (Crossing) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (!Arrays.equals(laneIds, other.laneIds))
            return false;
        if (priorityLock != other.priorityLock)
            return false;
        if (state != other.state)
            return false;
        if (validUntil != other.validUntil)
            return false;
        return true;
    }

}
