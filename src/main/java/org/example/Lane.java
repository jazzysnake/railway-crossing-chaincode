package org.example;

import org.hyperledger.fabric.contract.annotation.Property;
import org.json.JSONObject;
import org.hyperledger.fabric.contract.annotation.DataType;

@DataType()
public class Lane {
    
    @Property()
    private String id;

    @Property()
    private String crossingId;

    @Property()
    private int capacity;

    @Property()
    private int occupied;
    
    @Property()
    private boolean priorityLock;

    public static final String TYPE = "LANE";
    
    public Lane(String id, String crossingId, int capacity, int occupied, boolean priorityLock){
        this.id = id;
        this.crossingId = crossingId;
        this.capacity = capacity;
        this.occupied = occupied;
        this.priorityLock = priorityLock;
    }
    
    public boolean isFree(){
        return occupied<capacity && !priorityLock;
    }
    
    public String toJSONString(){
        return new JSONObject(this).toString();
    }
    
    public static Lane fromJSONString(String json) {
        JSONObject jsonObject = new JSONObject(json);
        String id = jsonObject.getString("id");
        String crossingId = jsonObject.getString("crossingId");
        int capacity = jsonObject.getInt("capacity");
        int occupied = jsonObject.getInt("occupied");
        boolean priorityLock = jsonObject.getBoolean("priorityLock");
        return new Lane(id, crossingId, capacity, occupied, priorityLock);
        
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

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getOccupied() {
        return occupied;
    }

    public void setOccupied(int occupied) {
        this.occupied = occupied;
    }

    public boolean isPriorityLock() {
        return priorityLock;
    }

    public void setPriorityLock(boolean priorityLock) {
        this.priorityLock = priorityLock;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + capacity;
        result = prime * result + ((crossingId == null) ? 0 : crossingId.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + occupied;
        result = prime * result + (priorityLock ? 1231 : 1237);
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
        Lane other = (Lane) obj;
        if (capacity != other.capacity)
            return false;
        if (crossingId == null) {
            if (other.crossingId != null)
                return false;
        } else if (!crossingId.equals(other.crossingId))
            return false;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (occupied != other.occupied)
            return false;
        if (priorityLock != other.priorityLock)
            return false;
        return true;
    }

}
