/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.example;

import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;
import org.json.JSONObject;

@DataType()
public class Request {
    
    public static final String TYPE = "REQUEST";

    @Property()
    private String id;

    @Property()
    private String crossingId;

    @Property()
    private String laneId;

    @Property()
    private RequesterRole roleOfRequester;

    @Property()
    private boolean granted;
    
    @Property()
    private boolean active;

    public Request(final String id, final String crossingId, final String laneId, final RequesterRole roleOfRequester,final boolean granted, final boolean active){
        this.id = id;
        this.crossingId = crossingId;
        this.laneId = laneId;
        this.roleOfRequester = roleOfRequester;
        this.granted = granted;
        this.active = active;
    }

    public String toJSONString() {
        return new JSONObject(this).toString();
    }

    public static Request fromJSONString(final String json) {
        final JSONObject jsonObject = new JSONObject(json);
        final String id = jsonObject.getString("id");
        final String crossingId = jsonObject.getString("crossingId");
        final String laneId = jsonObject.getString("laneId");
        final RequesterRole roleOfRequester = jsonObject.getEnum(RequesterRole.class,"roleOfRequester");
        final boolean isGranted = jsonObject.getBoolean("granted");
        final boolean isActive = jsonObject.getBoolean("active");
        return new Request(id, crossingId, laneId, roleOfRequester, isGranted, isActive);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCrossingId() {
        return crossingId;
    }

    public void setCrossingId(String crossingId) {
        this.crossingId = crossingId;
    }

    public String getLaneId() {
        return laneId;
    }

    public void setLaneId(String laneId) {
        this.laneId = laneId;
    }


    public RequesterRole getRoleOfRequester() {
        return roleOfRequester;
    }

    public void setRoleOfRequester(RequesterRole roleOfRequester) {
        this.roleOfRequester = roleOfRequester;
    }

    public boolean isGranted() {
        return granted;
    }

    public void setGranted(boolean isGranted) {
        this.granted = isGranted;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean isActive) {
        this.active = isActive;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (active ? 1231 : 1237);
        result = prime * result + ((crossingId == null) ? 0 : crossingId.hashCode());
        result = prime * result + (granted ? 1231 : 1237);
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((laneId == null) ? 0 : laneId.hashCode());
        result = prime * result + ((roleOfRequester == null) ? 0 : roleOfRequester.hashCode());
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
        Request other = (Request) obj;
        if (active != other.active)
            return false;
        if (crossingId == null) {
            if (other.crossingId != null)
                return false;
        } else if (!crossingId.equals(other.crossingId))
            return false;
        if (granted != other.granted)
            return false;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (laneId == null) {
            if (other.laneId != null)
                return false;
        } else if (!laneId.equals(other.laneId))
            return false;
        if (roleOfRequester != other.roleOfRequester)
            return false;
        return true;
    }

}
