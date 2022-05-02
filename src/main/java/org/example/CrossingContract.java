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
    public CrossingContract() {

    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public boolean crossingExists(final Context ctx, final String crossingId) {
        final String compKey = createCompKey(ctx, Crossing.TYPE, crossingId);
        final byte[] buffer = ctx.getStub().getState(compKey);
        return (buffer != null && buffer.length > 0);
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public Crossing createCrossing(final Context ctx, final String crossingId, final String[] laneIds, final int laneCapacity) {
        final boolean exists = crossingExists(ctx, crossingId);
        if (exists) {
            throw new ChaincodeException("The asset " + crossingId + " already exists");
        }
        final Crossing asset = new Crossing(crossingId, laneIds, CrossingState.FREE_TO_CROSS, false,
                ctx.getStub().getTxTimestamp().toEpochMilli() + 300 * 60);
        createLanes(ctx, laneIds, crossingId, laneCapacity);
        final String compKey = createCompKey(ctx, Crossing.TYPE, crossingId);
        ctx.getStub().putState(compKey, asset.toJSONString().getBytes(UTF_8));
        return asset;
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public Crossing readCrossing(final Context ctx, final String crossingId) {
        final boolean exists = crossingExists(ctx, crossingId);
        if (!exists) {
            throw new ChaincodeException("The asset " + crossingId + " does not exist");
        }
        final String compKey = createCompKey(ctx, Crossing.TYPE, crossingId);
        return Crossing.fromJSONString(new String(ctx.getStub().getState(compKey), UTF_8));
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public Crossing updateCrossing(final Context ctx, final String crossingId, final String[] laneIds, final String crossingState,
            final boolean priorityLock, final long validUntil) {
        final boolean exists = crossingExists(ctx, crossingId);
        if (!exists) {
            throw new ChaincodeException("The asset " + crossingId + " does not exist");
        }
        final Crossing asset = new Crossing(crossingId, laneIds, CrossingState.fromString(crossingState), priorityLock,
                validUntil);
        final String compKey = createCompKey(ctx, Crossing.TYPE, crossingId);
        ctx.getStub().putState(compKey, asset.toJSONString().getBytes(UTF_8));
        return asset;
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void deleteCrossing(final Context ctx, final String crossingId) {
        final boolean exists = crossingExists(ctx, crossingId);
        if (!exists) {
            throw new ChaincodeException("The asset " + crossingId + " does not exist");
        }
        final Crossing crossing = readCrossing(ctx, crossingId);
        Arrays.stream(crossing.getLaneIds()).forEach((laneId -> deleteLane(ctx, laneId, crossingId)));
        final String compKey = createCompKey(ctx, Crossing.TYPE, crossingId);
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
        final boolean exists = laneExists(ctx, laneId, crossingId);
        final boolean crossingExists = crossingExists(ctx, crossingId);
        if (exists) {
            throw new ChaincodeException("The asset " + laneId + " " + crossingId + " already exists");
        }
        if (!crossingExists) {
            throw new ChaincodeException("The specified crossing with id " + crossingId + " doesn't exist");
        }
        final Crossing crossing = readCrossing(ctx, crossingId);
        final Lane lane = new Lane(laneId, crossingId, capacity, 0, false);
        final String[] laneIds = Arrays.copyOf(crossing.getLaneIds(), crossing.getLaneIds().length + 1);
        laneIds[laneIds.length - 1] = laneId;

        updateCrossing(ctx, crossingId, laneIds, crossing.getState().name(), crossing.isPriorityLock(),
                crossing.getValidUntil());
        final String compKey = createCompKey(ctx, Lane.TYPE, laneId, crossingId);
        ctx.getStub().putState(compKey, lane.toJSONString().getBytes(UTF_8));
        return lane;
    }

    private Lane[] createLanes(final Context ctx, final String[] laneIds, final String crossingId, final int capacity) {
        Arrays.stream(laneIds).forEach(laneId -> {
            if (laneExists(ctx, laneId, crossingId)) {
                throw new ChaincodeException("The specified laneId " + laneId + " already exists");
            }
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
        final boolean exists = laneExists(ctx, laneId, crossingId);
        if (!exists) {
            throw new ChaincodeException("The asset " + laneId + " " + crossingId + " does not exist");
        }
        final String compKey = createCompKey(ctx, Lane.TYPE, laneId, crossingId);
        final Lane newAsset = Lane.fromJSONString(new String(ctx.getStub().getState(compKey), UTF_8));
        return newAsset;
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public Lane updateLane(final Context ctx, final String laneId, final String crossingId, final int capacity, final int occupied,
            final boolean priorityLock) {
        if (occupied > capacity) {
            throw new IllegalArgumentException("Capacity can't be a lower value than occupied");
        }
        final boolean exists = laneExists(ctx, laneId, crossingId);
        final boolean crossingExists = crossingExists(ctx, crossingId);
        if (!exists) {
            throw new ChaincodeException("The asset " + laneId + " " + crossingId + " does not exist");
        }
        if (!crossingExists) {
            throw new ChaincodeException("The specified crossing with id " + crossingId + " doesn't exist");
        }
        // TODO update crossing?
        final Lane asset = new Lane(laneId, crossingId, capacity, occupied, priorityLock);
        final String compKey = createCompKey(ctx, Lane.TYPE, laneId, crossingId);
        ctx.getStub().putState(compKey, asset.toJSONString().getBytes(UTF_8));
        return asset;
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void deleteLane(final Context ctx, final String laneId, final String crossingId) {
        final boolean exists = laneExists(ctx, laneId, crossingId);
        final boolean crossingExists = crossingExists(ctx, crossingId);
        if (!exists) {
            throw new ChaincodeException("The asset " + laneId + " " + crossingId + " does not exist");
        }
        if (!crossingExists) {
            throw new ChaincodeException("The specified crossing with id " + crossingId + " doesn't exist");
        }
        final Crossing crossing = readCrossing(ctx, crossingId);
        final String[] remainingLanes = Arrays.stream(crossing.getLaneIds()).filter((t -> {
            return !t.equals(laneId);
        })).toArray(String[]::new);
        // TODO lock status might change upon update
        updateCrossing(ctx, crossingId, remainingLanes, crossing.getState().name(), crossing.isPriorityLock(),
                crossing.getValidUntil());
        final String compKey = createCompKey(ctx, Lane.TYPE, laneId, crossingId);
        ctx.getStub().delState(compKey);
    }

}
