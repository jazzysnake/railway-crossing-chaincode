/*
 * SPDX-License-Identifier: Apache-2.0
 */
package org.example;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.ArrayList;
import java.util.Arrays;

import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contact;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Default;
import org.hyperledger.fabric.contract.annotation.Info;
import org.hyperledger.fabric.contract.annotation.License;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeException;

@Contract(name = "CrossingContract", info = @Info(title = "Crossing contract", description = "My Smart Contract", version = "0.0.1", license = @License(name = "Apache-2.0", url = ""), contact = @Contact(email = "railway-crossing@example.com", name = "railway-crossing", url = "http://railway-crossing.me")))
@Default
public class CrossingContract implements ContractInterface {
    private static final int CROSSING_VALIDITY_DURATION_S = 60;
    public CrossingContract() {}

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public boolean crossingExists(final Context ctx, final String crossingId) {
        final String compKey = createCompKey(ctx, Crossing.TYPE, crossingId);
        final byte[] buffer = ctx.getStub().getState(compKey);
        return (buffer != null && buffer.length > 0);
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public Crossing createCrossing(final Context ctx, final String crossingId, final String[] laneIds, final int laneCapacity) {
        assertCrossingExists(ctx, crossingId, false);
        final Crossing asset = new Crossing(crossingId, laneIds, CrossingState.FREE_TO_CROSS, false,
                ctx.getStub().getTxTimestamp().toEpochMilli() + 300 * 60);
        createLanes(ctx, laneIds, crossingId, laneCapacity);
        final String compKey = createCompKey(ctx, Crossing.TYPE, crossingId);
        ctx.getStub().putState(compKey, asset.toJSONString().getBytes(UTF_8));
        return asset;
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public Crossing readCrossing(final Context ctx, final String crossingId) {
        assertCrossingExists(ctx, crossingId, true);
        String compKey = createCompKey(ctx, Crossing.TYPE, crossingId);
        return Crossing.fromJSONString(new String(ctx.getStub().getState(compKey),UTF_8));
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public Crossing updateCrossing(final Context ctx, final String crossingId, final String[] laneIds, final String crossingState,
            final boolean priorityLock, final long validUntil) {
        assertCrossingExists(ctx, crossingId, true);
        final Crossing asset = new Crossing(crossingId, laneIds, CrossingState.fromString(crossingState), priorityLock,
                validUntil);
        final String compKey = createCompKey(ctx, Crossing.TYPE, crossingId);
        ctx.getStub().putState(compKey, asset.toJSONString().getBytes(UTF_8));
        return asset;
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void deleteCrossing(final Context ctx, final String crossingId) {
        assertCrossingExists(ctx, crossingId, true);
        Crossing crossing = readCrossing(ctx, crossingId);
        Arrays.stream(crossing.getLaneIds()).forEach((laneId-> deleteLane(ctx, laneId, crossingId)));
        String compKey = createCompKey(ctx, Crossing.TYPE, crossingId);
        ctx.getStub().delState(compKey);
    }

    private String createCompKey(final Context ctx, final String objectType, final String... idParts) {
        return ctx.getStub().createCompositeKey(objectType.toUpperCase(), idParts).toString();
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public boolean laneExists(final Context ctx, final String laneId, final String crossingId) {
        final String compKey = createCompKey(ctx, Lane.TYPE, laneId, crossingId);
        final byte[] buffer = ctx.getStub().getState(compKey);
        return (buffer != null && buffer.length > 0);
    }

    // TODO each lane creation and update might bring the crossing into an unwanted
    // state
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public Lane createLane(final Context ctx, final String laneId, final String crossingId, final int capacity) {
        assertCrossingExists(ctx, crossingId, true);
        assertLaneExists(ctx, laneId, crossingId, false);
        final Crossing crossing = readCrossing(ctx, crossingId);
        final Lane lane = new Lane(laneId, crossingId, capacity, 0, false);
        final String[] laneIds = Arrays.copyOf(crossing.getLaneIds(), crossing.getLaneIds().length + 1);
        laneIds[laneIds.length - 1] = laneId;

        updateCrossing(ctx, crossingId, laneIds, crossing.getState().name(), crossing.isPriorityLock(),calcValidity(ctx));
        String compKey = createCompKey(ctx, Lane.TYPE,laneId, crossingId);
        ctx.getStub().putState(compKey, lane.toJSONString().getBytes(UTF_8));
        return lane;
    }

    private Lane[] createLanes(final Context ctx, final String[] laneIds, final String crossingId, final int capacity) {
        Arrays.stream(laneIds).forEach(laneId -> {
            assertLaneExists(ctx, laneId, crossingId, false);
        });
        final ArrayList<Lane> lanes = new ArrayList<>(laneIds.length);
        Arrays.stream(laneIds).forEach(laneId -> {
            final Lane lane = new Lane(laneId, crossingId, capacity, 0, false);
            lanes.add(lane);
            final String compKey = createCompKey(ctx, Lane.TYPE, laneId, crossingId);
            ctx.getStub().putState(compKey, lane.toJSONString().getBytes(UTF_8));
        });
        return lanes.toArray(new Lane[laneIds.length]);
    }
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public Lane readLane(final Context ctx, final String laneId, final String crossingId) {
        assertLaneExists(ctx, laneId, crossingId, true);
        final String compKey = createCompKey(ctx, Lane.TYPE, laneId, crossingId);
        return Lane.fromJSONString(new String(ctx.getStub().getState(compKey), UTF_8));
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public Lane updateLane(final Context ctx, final String laneId, final String crossingId, final int capacity, final int occupied,
            final boolean priorityLock) {
        if (occupied > capacity) {
            throw new IllegalArgumentException("Capacity can't be a lower value than occupied");
        }
        assertCrossingExists(ctx, crossingId, true);
        assertLaneExists(ctx, laneId, crossingId, true);
        // TODO update crossing?
        final Lane asset = new Lane(laneId, crossingId, capacity, occupied, priorityLock);
        final String compKey = createCompKey(ctx, Lane.TYPE, laneId, crossingId);
        ctx.getStub().putState(compKey, asset.toJSONString().getBytes(UTF_8));
        return asset;
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void deleteLane(final Context ctx, final String laneId, final String crossingId) {
        assertCrossingExists(ctx, crossingId, true);
        assertLaneExists(ctx, laneId, crossingId, true);
        final Crossing crossing = readCrossing(ctx, crossingId);
        final String compKey = createCompKey(ctx, Lane.TYPE, laneId, crossingId);
        String[] remainingLanes = Arrays.stream(crossing.getLaneIds()).filter((t -> !t.equals(laneId))).toArray(String[]::new);
        //TODO lock status might change upon update
        updateCrossing(ctx, crossingId, remainingLanes, crossing.getState().name(), crossing.isPriorityLock(),calcValidity(ctx));
        ctx.getStub().delState(compKey);
    }
    
    private void throwAssetAlreadyExistsException(String assetId){
        throw new ChaincodeException("The specified asset "+assetId+" already exists");
    }

    private void throwAssetDoesntExistException(String assetId){
        throw new ChaincodeException("The specified asset "+assetId+" doesn't exist");
    }
    
    private long calcValidity(final Context ctx){
        return ctx.getStub().getTxTimestamp().getEpochSecond()+CrossingContract.CROSSING_VALIDITY_DURATION_S;
    }

    private void assertCrossingExists(final Context ctx, final String id, boolean shouldExist){
        final boolean exist = crossingExists(ctx, id);
        if (shouldExist && !exist) {
            throwAssetDoesntExistException(id);
        }else if(!shouldExist && exist){
            throwAssetAlreadyExistsException(id);
        }
    }

    private void assertLaneExists(final Context ctx, final String laneId, final String crossingId,boolean shouldExist){
        final boolean exist = laneExists(ctx, laneId, crossingId);
        if (shouldExist && !exist) {
            throwAssetDoesntExistException(laneId + " " + crossingId);
        }else if(!shouldExist && exist){
            throwAssetAlreadyExistsException(laneId + " " + crossingId);
        }
    }
}
