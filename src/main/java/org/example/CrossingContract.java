/*
 * SPDX-License-Identifier: Apache-2.0
 */
package org.example;

import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Default;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.contract.annotation.Contact;
import org.hyperledger.fabric.contract.annotation.Info;
import org.hyperledger.fabric.contract.annotation.License;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.ArrayList;
import java.util.Arrays;

@Contract(name = "CrossingContract",
    info = @Info(title = "Crossing contract",
                description = "My Smart Contract",
                version = "0.0.1",
                license =
                        @License(name = "Apache-2.0",
                                url = ""),
                                contact =  @Contact(email = "railway-crossing@example.com",
                                                name = "railway-crossing",
                                                url = "http://railway-crossing.me")))
@Default
public class CrossingContract implements ContractInterface {
    private static final String CROSSING = "CROSSING";
    private static final String LANE = "LANE";

    public  CrossingContract(){}

    @Transaction()
    public boolean crossingExists(Context ctx, String crossingId) {
        String compKey = createCompKey(ctx, CROSSING, crossingId);
        byte[] buffer = ctx.getStub().getState(compKey);
        return (buffer != null && buffer.length > 0);
    }

    @Transaction()
    public Crossing createCrossing(Context ctx, String crossingId, String[] laneIds ,int laneCapacity) {
        boolean exists = crossingExists(ctx,crossingId);
        if (exists) {
            throwAssetAlreadyExistsException(crossingId);
        }
        Crossing asset = new Crossing(crossingId,laneIds,CrossingState.FREE_TO_CROSS,false);
        createLanes(ctx, laneIds, crossingId, laneCapacity);
        String compKey = createCompKey(ctx, CROSSING, crossingId);
        ctx.getStub().putState(compKey, asset.toJSONString().getBytes(UTF_8));
        return asset;
    }

    @Transaction()
    public Crossing readCrossing(Context ctx, String crossingId) {
        boolean exists = crossingExists(ctx,crossingId);
        if (!exists) {
            throwAssetDoesntExistException(crossingId);
        }
        String compKey = createCompKey(ctx, CROSSING, crossingId);
        return Crossing.fromJSONString(new String(ctx.getStub().getState(compKey),UTF_8));
    }

    @Transaction()
    public Crossing updateCrossing(Context ctx, String crossingId, String[] laneIds, String crossingState,boolean priorityLock){
        boolean exists = crossingExists(ctx,crossingId);
        if (!exists) {
            throwAssetDoesntExistException(crossingId);
        }
        Crossing asset = new Crossing(crossingId, laneIds, CrossingState.fromString(crossingState),priorityLock);
        String compKey = createCompKey(ctx, CROSSING, crossingId);
        ctx.getStub().putState(compKey, asset.toJSONString().getBytes(UTF_8));
        return asset;
    }

    @Transaction()
    public void deleteCrossing(Context ctx, String crossingId) {
        boolean exists = crossingExists(ctx, crossingId);
        if (!exists) {
            throwAssetDoesntExistException(crossingId);
        }
        Crossing crossing = readCrossing(ctx, crossingId);
        Arrays.stream(crossing.getLaneIds()).forEach((laneId-> deleteLane(ctx, laneId, crossingId)));
        String compKey = createCompKey(ctx, CROSSING, crossingId);
        ctx.getStub().delState(compKey);
    }
    
    private String createCompKey(Context ctx, String objectType, String... idParts){
        return ctx.getStub().createCompositeKey(objectType.toUpperCase(), idParts).toString();
    }
    
    @Transaction()
    public boolean laneExists(Context ctx, String laneId, String crossingId) {
        String compKey = createCompKey(ctx, LANE, laneId, crossingId);
        byte[] buffer = ctx.getStub().getState(compKey);
        return (buffer != null && buffer.length > 0);
    }

    //TODO each lane creation and update might bring the crossing into an unwanted state
    @Transaction()
    public Lane createLane(Context ctx, String laneId, String crossingId,int capacity) {
        boolean exists = laneExists(ctx,laneId,crossingId);
        boolean crossingExists = crossingExists(ctx, crossingId);
        if (exists) {
            throwAssetDoesntExistException(laneId + " " + crossingId);
        }
        if (!crossingExists) {
            throwAssetDoesntExistException(crossingId);
        }
        Crossing crossing = readCrossing(ctx, crossingId);
        Lane lane = new Lane(laneId, crossingId, capacity, 0, false);
        String[] laneIds = Arrays.copyOf(crossing.getLaneIds(), crossing.getLaneIds().length+1);
        laneIds[laneIds.length-1]= laneId;

        updateCrossing(ctx, crossingId, laneIds, crossing.getState().name(), crossing.isPriorityLock());
        String compKey = createCompKey(ctx, LANE,laneId, crossingId);
        ctx.getStub().putState(compKey, lane.toJSONString().getBytes(UTF_8));
        return lane;
    }

    private Lane[] createLanes(Context ctx, String[] laneIds, String crossingId,int capacity) {
        Arrays.stream(laneIds).forEach(laneId->{
            if (laneExists(ctx, laneId, crossingId)) {
                throwAssetAlreadyExistsException(laneId);
            }
        });
        ArrayList<Lane> lanes = new ArrayList<>(laneIds.length);
        Arrays.stream(laneIds).forEach(laneId->{
            Lane lane = new Lane(laneId, crossingId, capacity, 0, false);
            lanes.add(lane);
            String compKey = createCompKey(ctx, LANE,laneId, crossingId);
            ctx.getStub().putState(compKey, lane.toJSONString().getBytes(UTF_8));
        });
        return lanes.toArray(new Lane[laneIds.length]);
    }

    @Transaction()
    public Lane readLane(Context ctx,String laneId, String crossingId) {
        boolean exists = laneExists(ctx,laneId,crossingId);
        if (!exists) {
            throwAssetDoesntExistException(laneId + " " + crossingId);
        }
        String compKey = createCompKey(ctx, LANE,laneId, crossingId);
        return Lane.fromJSONString(new String(ctx.getStub().getState(compKey),UTF_8));
    }

    @Transaction()
    public Lane updateLane(Context ctx, String laneId, String crossingId, int capacity, int occupied,boolean priorityLock) {
        if (occupied>capacity) {
            throw new IllegalArgumentException("Capacity can't be a lower value than occupied");
        }
        boolean exists = laneExists(ctx, laneId, crossingId);
        boolean crossingExists = crossingExists(ctx,crossingId);
        if (!exists) {
            throwAssetDoesntExistException(laneId + " " + crossingId);
        }
        if (!crossingExists) {
            throwAssetDoesntExistException(crossingId);
        }
        //TODO update crossing?
        Lane asset = new Lane(laneId, crossingId, capacity, occupied, priorityLock);
        String compKey = createCompKey(ctx, LANE,laneId, crossingId);
        ctx.getStub().putState(compKey, asset.toJSONString().getBytes(UTF_8));
        return asset;
    }

    @Transaction()
    public void deleteLane(Context ctx,String laneId, String crossingId) {
        boolean exists = laneExists(ctx, laneId, crossingId);
        boolean crossingExists = crossingExists(ctx, crossingId);
        if (!exists) {
            throwAssetDoesntExistException(laneId+" "+crossingId);
        }
        if (!crossingExists) {
            throwAssetDoesntExistException(crossingId);
        }
        Crossing crossing = readCrossing(ctx, crossingId);
        String[] remainingLanes = Arrays.stream(crossing.getLaneIds()).filter((t -> !t.equals(laneId))).toArray(String[]::new);
        //TODO lock status might change upon update
        updateCrossing(ctx, crossingId, remainingLanes, crossing.getState().name(), crossing.isPriorityLock());
        String compKey = createCompKey(ctx, LANE,laneId, crossingId);
        ctx.getStub().delState(compKey);
    }
    
    private void throwAssetAlreadyExistsException(String assetId){
        throw new ChaincodeException("The specified asset "+assetId+" already exists");
    }

    private void throwAssetDoesntExistException(String assetId){
        throw new ChaincodeException("The specified asset "+assetId+" doesn't exist");
    }

}
