/*
 * SPDX-License-Identifier: Apache-2.0
 */
package hu.bme;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import org.hyperledger.fabric.Logger;
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
    private long requestId = 0;
    private static final Logger log = Logger.getLogger(CrossingContract.class);
    private static final String RAILWAY_ORG_MSP = "RailwayOrgMSP";
    private static final String VEHICLE_OWNER_MSP = "VehicleOwnerOrgMSP";

    public CrossingContract() {
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public boolean crossingExists(final Context ctx, final String crossingId) {
        final String compKey = createCompKey(ctx, Crossing.TYPE, crossingId);
        final byte[] buffer = ctx.getStub().getState(compKey);
        return (buffer != null && buffer.length > 0);
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public Crossing createCrossing(final Context ctx, final String crossingId, final String[] laneIds,
            final int laneCapacity) {
        assertCallingOrg(ctx, RAILWAY_ORG_MSP);
        assertCrossingExists(ctx, crossingId, false);
        assertRailwayAdmin(ctx);
        final Crossing asset = new Crossing(crossingId, laneIds, CrossingState.FREE_TO_CROSS, false,
                ctx.getStub().getTxTimestamp().getEpochSecond()+CROSSING_VALIDITY_DURATION_S);
        createLanes(ctx, laneIds, crossingId, laneCapacity);
        final String compKey = createCompKey(ctx, Crossing.TYPE, crossingId);
        ctx.getStub().putState(compKey, asset.toJSONString().getBytes(UTF_8));
        return asset;
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public Crossing readCrossing(final Context ctx, final String crossingId) {
        assertCrossingExists(ctx, crossingId, true);
        String compKey = createCompKey(ctx, Crossing.TYPE, crossingId);
        return Crossing.fromJSONString(new String(ctx.getStub().getState(compKey), UTF_8));
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void deleteCrossing(final Context ctx, final String crossingId) {
        assertCallingOrg(ctx, RAILWAY_ORG_MSP);
        assertRailwayAdmin(ctx);
        assertCrossingExists(ctx, crossingId, true);
        Crossing crossing = readCrossing(ctx, crossingId);
        Arrays.stream(crossing.getLaneIds()).forEach((laneId -> deleteLane(ctx, laneId, crossingId)));
        String compKey = createCompKey(ctx, Crossing.TYPE, crossingId);
        ctx.getStub().delState(compKey);
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
        assertCallingOrg(ctx, RAILWAY_ORG_MSP);
        assertRailwayAdmin(ctx);
        assertCrossingExists(ctx, crossingId, true);
        assertLaneExists(ctx, laneId, crossingId, false);
        final Crossing crossing = readCrossing(ctx, crossingId);
        final Lane lane = new Lane(laneId, crossingId, capacity, 0, false);
        final String[] laneIds = Arrays.copyOf(crossing.getLaneIds(), crossing.getLaneIds().length + 1);
        laneIds[laneIds.length - 1] = laneId;

        updateCrossing(ctx, crossingId, laneIds, crossing.getState().name(), crossing.isPriorityLock(),
                calcValidity(ctx));
        String compKey = createCompKey(ctx, Lane.TYPE, laneId, crossingId);
        ctx.getStub().putState(compKey, lane.toJSONString().getBytes(UTF_8));
        return lane;
    }


    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public Lane readLane(final Context ctx, final String laneId, final String crossingId) {
        assertLaneExists(ctx, laneId, crossingId, true);
        final String compKey = createCompKey(ctx, Lane.TYPE, laneId, crossingId);
        return Lane.fromJSONString(new String(ctx.getStub().getState(compKey), UTF_8));
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void deleteLane(final Context ctx, final String laneId, final String crossingId) {
        assertCallingOrg(ctx, RAILWAY_ORG_MSP);
        assertRailwayAdmin(ctx);
        assertCrossingExists(ctx, crossingId, true);
        assertLaneExists(ctx, laneId, crossingId, true);
        final Crossing crossing = readCrossing(ctx, crossingId);
        final String compKey = createCompKey(ctx, Lane.TYPE, laneId, crossingId);
        String[] remainingLanes = Arrays.stream(crossing.getLaneIds()).filter((t -> !t.equals(laneId)))
                .toArray(String[]::new);
        // TODO lock status might change upon update
        updateCrossing(ctx, crossingId, remainingLanes, crossing.getState().name(), crossing.isPriorityLock(),crossing.getValidUntil());
        ctx.getStub().delState(compKey);
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public Request requestTrainCrossing(final Context ctx, final String crossingId) {
        assertCallingOrg(ctx, RAILWAY_ORG_MSP);
        assertCrossingExists(ctx, crossingId, true);
        Crossing crossing = readCrossing(ctx, crossingId);

        updateCrossing(ctx, crossingId, crossing.getLaneIds(), CrossingState.LOCKED.name(), true, 0L);

        requestId++;
        String compKey = createCompKey(ctx, Request.TYPE, "" + this.requestId, "N/A", crossingId);
        Request request = new Request("" + this.requestId, crossingId, "N/A", RequesterRole.TRAIN, false,
                false);
        recordClientIdentity(ctx, "" + requestId, "N/A", crossingId,ctx.getClientIdentity().getId());

        Arrays.stream(crossing.getLaneIds()).forEach(laneId -> lockLane(ctx, laneId, crossingId, true));

        if (crossing.getState() != CrossingState.FREE_TO_CROSS ||
                crossing.getValidUntil() < ctx.getStub().getTxTimestamp().getEpochSecond()) {

            ctx.getStub().putState(compKey, request.toJSONString().getBytes(UTF_8));
            return request;
        }

        request.setActive(true);
        request.setGranted(true);
        ctx.getStub().putState(compKey, request.toJSONString().getBytes(UTF_8));
        return request;
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public Request requestCarCrossing(final Context ctx, final String crossingId) {
        assertCallingOrg(ctx, VEHICLE_OWNER_MSP);
        assertCrossingExists(ctx, crossingId, true);

        Crossing crossing = readCrossing(ctx, crossingId);
        this.requestId++;
        recordClientIdentity(ctx, "" + requestId, "N/A", crossingId,ctx.getClientIdentity().getId());

        String compKey = createCompKey(ctx, Request.TYPE, "" + this.requestId, crossingId, "N/A");
        if (crossing.isPriorityLock()) {
            log.warning("Crossing request " + this.requestId + " denied because of priorityLock");
            Request request = new Request("" + this.requestId, crossingId, "N/A", RequesterRole.CAR, false,
                    false);

            ctx.getStub().putState(compKey, request.toJSONString().getBytes(UTF_8));
            return request;
        }

        String[] freeLanes = Arrays.stream(crossing.getLaneIds())
                .filter(lane -> readLane(ctx, lane, crossingId).isFree()).toArray(String[]::new);

        if (freeLanes.length == 0) {
            log.warning("Crossing request " + this.requestId + " denied because all lanes are full");
            Request request = new Request("" + this.requestId, crossingId, "N/A", RequesterRole.CAR, false,
                    false);
            ctx.getStub().putState(compKey, request.toJSONString().getBytes(UTF_8));
            return request;
        }

        Random rand = new Random(ctx.getStub().getTxTimestamp().toEpochMilli());
        int random = rand.nextInt(freeLanes.length);

        String laneId = freeLanes[random];
        lockLane(ctx, laneId, crossingId, false);
        updateCrossing(ctx, crossingId, crossing.getLaneIds(), CrossingState.LOCKED.name(), false, 0L);
        Request request = new Request("" + this.requestId, crossingId, laneId, RequesterRole.CAR, true,
                true);
        compKey = createCompKey(ctx, Request.TYPE, "" + this.requestId, crossingId, laneId);
        log.info("Crossing request " + this.requestId + " successful");
        ctx.getStub().putState(compKey, request.toJSONString().getBytes(UTF_8));
        return request;
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void releaseTrainPermission(final Context ctx, final long requestId, final String crossingId) {
        assertCallingOrg(ctx, RAILWAY_ORG_MSP);
        assertCrossingExists(ctx, crossingId, true);
        assertRequestExists(ctx, requestId, "N/A", crossingId);

        String compKey = createCompKey(ctx, Request.TYPE, "" + requestId, "N/A", crossingId);
        Request request = Request.fromJSONString(new String(ctx.getStub().getState(compKey), UTF_8));
        request.setActive(false);

        Crossing crossing = readCrossing(ctx, crossingId);
        Lane[] lanes = Arrays.stream(crossing.getLaneIds()).map(l -> readLane(ctx, l, crossingId)).toArray(Lane[]::new);
        Arrays.stream(lanes).forEach(l -> l.setPriorityLock(false));

        Lane[] freeLanes = Arrays.stream(lanes).filter(Lane::isFree).toArray(Lane[]::new);
        if (freeLanes.length == lanes.length) {
            updateCrossing(ctx, crossingId, crossing.getLaneIds(), CrossingState.FREE_TO_CROSS.name(), false,
                    calcValidity(ctx));
        }
        Arrays.stream(lanes).forEach(l -> unLockLane(ctx, l.getId(), crossingId, true));

        ctx.getStub().putState(compKey, request.toJSONString().getBytes(UTF_8));
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void releaseCarPermission(final Context ctx, final long requestId, final String crossingId,
            final String laneId) {
        assertCallingOrg(ctx, VEHICLE_OWNER_MSP);
        assertCrossingExists(ctx, crossingId, true);
        assertLaneExists(ctx, laneId, crossingId, true);
        assertRequestExists(ctx, requestId, laneId, crossingId);

        // TODO check caller identity
        String compKey = createCompKey(ctx, Request.TYPE, "" + requestId, crossingId, laneId);
        Request request = Request.fromJSONString(new String(ctx.getStub().getState(compKey), UTF_8));
        request.setActive(false);

        Crossing crossing = readCrossing(ctx, crossingId);
        Lane[] lanes = Arrays.stream(crossing.getLaneIds()).map(l -> readLane(ctx, l, crossingId)).toArray(Lane[]::new);
        for (int i = 0; i < lanes.length; i++) {
            if (lanes[i].getId().equals(laneId)) {
                lanes[i].setOccupied(lanes[i].getOccupied() - 1);
                updateLane(ctx, laneId, crossingId, lanes[i].getCapacity(), lanes[i].getOccupied(),
                        lanes[i].isPriorityLock());
                break;
            }
        }

        Lane[] freeLanes = Arrays.stream(lanes).filter(Lane::isFree).toArray(Lane[]::new);
        if (freeLanes.length == lanes.length) {
            updateCrossing(ctx, crossingId, crossing.getLaneIds(), CrossingState.FREE_TO_CROSS.name(), false,
                    calcValidity(ctx));
        }

        ctx.getStub().putState(compKey, request.toJSONString().getBytes(UTF_8));
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public Crossing renewFreeToCrossValidity(final Context ctx, final String crossingId) {
        assertCallingOrg(ctx, RAILWAY_ORG_MSP);
        assertRailwayAdmin(ctx);
        assertCrossingExists(ctx, crossingId, true);
        Crossing crossing = readCrossing(ctx, crossingId);

        if (crossing.getState() != CrossingState.FREE_TO_CROSS) {
            throw new ChaincodeException("Crossing must be in FREE_TO_CROSS state for this operation");
        }

        crossing.setValidUntil(calcValidity(ctx));
        updateCrossing(ctx, crossingId, crossing.getLaneIds(), crossing.getState().name(), crossing.isPriorityLock(),
                crossing.getValidUntil());

        return crossing;
    }

    private String createCompKey(final Context ctx, final String objectType, final String... idParts) {
        return ctx.getStub().createCompositeKey(objectType.toUpperCase(), idParts).toString();
    }

    private Lane[] createLanes(final Context ctx, final String[] laneIds, final String crossingId, final int capacity) {
        Arrays.stream(laneIds).forEach(laneId -> assertLaneExists(ctx, laneId, crossingId, false));
        final ArrayList<Lane> lanes = new ArrayList<>(laneIds.length);
        Arrays.stream(laneIds).forEach(laneId -> {
            final Lane lane = new Lane(laneId, crossingId, capacity, 0, false);
            lanes.add(lane);
            final String compKey = createCompKey(ctx, Lane.TYPE, laneId, crossingId);
            ctx.getStub().putState(compKey, lane.toJSONString().getBytes(UTF_8));
        });
        return lanes.toArray(new Lane[laneIds.length]);
    }

    private Crossing updateCrossing(final Context ctx, final String crossingId, final String[] laneIds,
            final String crossingState,
            final boolean priorityLock, final long validUntil) {
        assertCrossingExists(ctx, crossingId, true);
        final Crossing asset = new Crossing(crossingId, laneIds, CrossingState.fromString(crossingState), priorityLock,
                validUntil);
        final String compKey = createCompKey(ctx, Crossing.TYPE, crossingId);
        ctx.getStub().putState(compKey, asset.toJSONString().getBytes(UTF_8));
        return asset;
    }

    private Lane updateLane(final Context ctx, final String laneId, final String crossingId, final int capacity,
            final int occupied,
            final boolean priorityLock) {
        if (occupied > capacity) {
            throw new IllegalArgumentException("Capacity can't be a lower value than occupied");
        }
        assertCrossingExists(ctx, crossingId, true);
        assertLaneExists(ctx, laneId, crossingId, true);
        final Lane asset = new Lane(laneId, crossingId, capacity, occupied, priorityLock);
        final String compKey = createCompKey(ctx, Lane.TYPE, laneId, crossingId);
        ctx.getStub().putState(compKey, asset.toJSONString().getBytes(UTF_8));
        return asset;
    }


    private Lane lockLane(final Context ctx, final String laneId, final String crossingId, final boolean priorityLock) {
        assertCrossingExists(ctx, crossingId, true);
        assertLaneExists(ctx, laneId, crossingId, true);
        Lane lane = readLane(ctx, laneId, crossingId);

        if (priorityLock) {
            lane.setPriorityLock(priorityLock);
        } else if (lane.getCapacity() <= lane.getOccupied()) {
            throw new ChaincodeException("Lane is already fully occupied");
        } else {
            lane.setOccupied(lane.getOccupied() + 1);
        }
        final String compKey = createCompKey(ctx, Lane.TYPE, laneId, crossingId);
        ctx.getStub().putState(compKey, lane.toJSONString().getBytes(UTF_8));
        return lane;
    }

    private Lane unLockLane(final Context ctx, final String laneId, final String crossingId,
            final boolean priorityLock) {
        assertCrossingExists(ctx, crossingId, true);
        assertLaneExists(ctx, laneId, crossingId, true);
        Lane lane = readLane(ctx, laneId, crossingId);

        if (priorityLock) {
            lane.setPriorityLock(false);
        } else {
            lane.setOccupied(lane.getOccupied() - 1);
        }
        String compKey = createCompKey(ctx, Lane.TYPE, laneId, crossingId);
        ctx.getStub().putState(compKey, lane.toJSONString().getBytes(UTF_8));
        return lane;
    }

    private void throwAssetAlreadyExistsException(String assetId) {
        throw new ChaincodeException("The specified asset " + assetId + " already exists");
    }

    private void throwAssetDoesntExistException(String assetId) {
        throw new ChaincodeException("The specified asset " + assetId + " doesn't exist");
    }

    private long calcValidity(final Context ctx) {
        return ctx.getStub().getTxTimestamp().getEpochSecond() + CrossingContract.CROSSING_VALIDITY_DURATION_S;
    }

    private void assertCrossingExists(final Context ctx, final String id, boolean shouldExist) {
        final boolean exist = crossingExists(ctx, id);
        if (shouldExist && !exist) {
            throwAssetDoesntExistException(id);
        } else if (!shouldExist && exist) {
            throwAssetAlreadyExistsException(id);
        }
    }

    private void assertLaneExists(final Context ctx, final String laneId, final String crossingId,
            boolean shouldExist) {
        final boolean exist = laneExists(ctx, laneId, crossingId);
        if (shouldExist && !exist) {
            throwAssetDoesntExistException(laneId + " " + crossingId);
        } else if (!shouldExist && exist) {
            throwAssetAlreadyExistsException(laneId + " " + crossingId);
        }
    }

    private void assertRequestExists(final Context ctx, final long requestId, final String laneId,
            final String crossingId) {
        String compKey = createCompKey(ctx, Request.TYPE, "" + requestId, laneId, crossingId);
        byte[] buffer = ctx.getStub().getState(compKey);
        final boolean exists = (buffer != null && buffer.length > 0);

        if (!exists) {
            throwAssetDoesntExistException(requestId + " " + crossingId + " " + laneId);
        }
    }

    private void assertCallingOrg(final Context ctx, final String msp) {
        if (!ctx.getClientIdentity().getMSPID().equals(msp)) {
            throw new ChaincodeException("Must be part of " + msp + " to perform this operation");
        }
    }

    private void recordClientIdentity(final Context ctx, String requestId, String laneId, String crossingId, String clientId) {
        String compKey = createCompKey(ctx, RequestPrivateData.COLLECTION_NAME, ""+requestId, laneId, crossingId);
        RequestPrivateData priv = new RequestPrivateData(requestId, laneId, crossingId, clientId);
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new ChaincodeException("");
        }
        byte[] hash = digest.digest(priv.toJSONString().getBytes(UTF_8));
        ctx.getStub().putState(compKey, hash);
    }
    
    private void assertRailwayAdmin(Context ctx){
        if(!ctx.getClientIdentity().getId().equals("x509::CN=RailwayOrg Admin, OU=admin::CN=RailwayOrg CA")){
            throw new ChaincodeException("Only the RailwayOrg's Admin can perform this operation");
        }
        
    }
}
