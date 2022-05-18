/*
 * SPDX-License-Identifier: Apache License 2.0
 */

package hu.bme;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;

import org.apache.log4j.Logger;
import org.hyperledger.fabric.contract.ClientIdentity;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.CompositeKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;


final class CrossingContractTest {
    public static Logger log = Logger.getLogger(CrossingContractTest.class.getName());
    private CrossingContract contract = new CrossingContract();
    private Context ctx;
    private ChaincodeStub stub;
    private String crossingId;
    private CompositeKey crossingCompKey;
    private String railwayAdminId = "x509::CN=RailwayOrg Admin, OU=admin::CN=RailwayOrg CA";
    @BeforeEach
    public void init(){
        ctx = mock(Context.class);
        stub = mock(ChaincodeStub.class);
        when(ctx.getStub()).thenReturn(stub);
        crossingId = "001";
        crossingCompKey = new CompositeKey(Crossing.TYPE,crossingId);
        when(stub.createCompositeKey(Crossing.TYPE, crossingId)).thenReturn(crossingCompKey);
    }


    @Nested
    class CrossingExists {
        @Test
        public void noProperCrossing() {
            when(stub.getState(crossingCompKey.toString())).thenReturn(new byte[] {});

            boolean result = contract.crossingExists(ctx,crossingId);

            assertFalse(result);
        }

        @Test
        public void crossingExists() {

            when(stub.getState(crossingCompKey.toString())).thenReturn(new byte[] {42});
            boolean result = contract.crossingExists(ctx,crossingId);

            assertTrue(result);

        }

        @Test
        public void noKey() {
            String cId = "002";
            CompositeKey compositeKey = new CompositeKey(Crossing.TYPE, cId);
            when(stub.createCompositeKey(Crossing.TYPE, cId)).thenReturn(compositeKey);

            when(stub.getState(compositeKey.toString())).thenReturn(null);
            boolean result = contract.crossingExists(ctx,cId);

            assertFalse(result);

        }
        
    }

    @Nested
    class CrossingCreates {
        private String[] laneIds;
        private Crossing crossing;
        private final String msp = "RailwayOrgMSP";
        private CompositeKey laneCompositeKey;

        @BeforeEach
        void initCreation(){
            laneIds = new String[1];
            laneIds[0] = "01";
            laneCompositeKey = new CompositeKey(Lane.TYPE, laneIds[0],crossingId);
            crossing = new Crossing(crossingId, laneIds , CrossingState.FREE_TO_CROSS, false, 0);
            when(stub.createCompositeKey(Lane.TYPE, laneIds[0],crossingId)).thenReturn(laneCompositeKey);
        }

        @Test
        public void newCrossingCreate() {

            when(stub.getState(crossingCompKey.toString())).thenReturn(new byte[] {});
            ClientIdentity clientIdentity = mock(ClientIdentity.class);
            when(clientIdentity.getId()).thenReturn(railwayAdminId);
            when(ctx.getClientIdentity()).thenReturn(clientIdentity);
            when(clientIdentity.getMSPID()).thenReturn(msp);
            when(stub.getTxTimestamp()).thenReturn(Instant.ofEpochMilli(0));

            Crossing cross = new Crossing(crossingId, laneIds, crossing.getState(), crossing.isPriorityLock(),60);
            String json = cross.toJSONString();
            contract.createCrossing(ctx, crossingId, laneIds, 1);

            verify(stub).putState(crossingCompKey.toString(), json.getBytes(UTF_8));
        }

        @Test
        public void alreadyExists() {
            when(stub.getState(crossingCompKey.toString())).thenReturn(new byte[] { 42 });
            ClientIdentity clientIdentity = mock(ClientIdentity.class);
            when(ctx.getClientIdentity()).thenReturn(clientIdentity);
            when(clientIdentity.getMSPID()).thenReturn(msp);
            when(stub.getTxTimestamp()).thenReturn(Instant.ofEpochMilli(0));

            Exception thrown = assertThrows(ChaincodeException.class, () -> {
                contract.createCrossing(ctx, crossingId, laneIds, 1);
            });

            assertTrue(thrown.getMessage().contains("already exists"));
        }
        
        @Test
        public void notFromRailwayOrg(){
            when(stub.getState(crossingCompKey.toString())).thenReturn(new byte[] { 42 });
            ClientIdentity clientIdentity = mock(ClientIdentity.class);
            when(ctx.getClientIdentity()).thenReturn(clientIdentity);
            when(clientIdentity.getMSPID()).thenReturn("NotRailway");
            when(stub.getTxTimestamp()).thenReturn(Instant.ofEpochMilli(0));

            Exception thrown = assertThrows(ChaincodeException.class, () -> {
                contract.createCrossing(ctx, crossingId, laneIds, 1);
            });
            String errorMessage = "Must be part of " + msp + " to perform this operation";
            assertEquals(errorMessage, thrown.getMessage());
        }

    }

    @Nested
    class CrossingReads{

        @Test
        public void crossingReadValid() {
            String[] laneIds = new String[1];
            laneIds[0] = "01";
            Crossing crossing = new Crossing(crossingId, laneIds, CrossingState.FREE_TO_CROSS, false, 0);
            when(stub.getState(crossingCompKey.toString())).thenReturn(crossing.toJSONString().getBytes(UTF_8));

            Crossing returnedCrossing = contract.readCrossing(ctx,crossingId);
            assertEquals(returnedCrossing,crossing);
        }
        
        @Test
        public void crossingReadNonexistent(){
            when(stub.createCompositeKey(Crossing.TYPE, "nonExistentCrossingId")).thenReturn(new CompositeKey(Crossing.TYPE, "nonExistentCrossingId"));
            when(stub.getState(crossingCompKey.toString())).thenReturn(new byte[] {});
            Exception thrown = assertThrows(ChaincodeException.class, () -> {
                contract.readCrossing(ctx, "nonExistentCrossingId");
            });
            assertTrue(thrown.getMessage().contains("doesn't exist"));
        }

    }
    
    @Nested
    class CrossingDeletes {
        private String[] laneIds;
        private CompositeKey laneCompositeKey;
        private ClientIdentity clientIdentity;

        @BeforeEach
        void initDeletion(){
            laneIds = new String[1];
            laneIds[0] = "01";
            laneCompositeKey = new CompositeKey(Lane.TYPE, laneIds[0],crossingId);
            when(stub.createCompositeKey(Lane.TYPE, laneIds[0],crossingId)).thenReturn(laneCompositeKey);
            clientIdentity = mock(ClientIdentity.class);
            when(ctx.getClientIdentity()).thenReturn(clientIdentity);
        }

        @Test
        public void deleteValid(){
            String msp = "RailwayOrgMSP";
            when(clientIdentity.getMSPID()).thenReturn(msp);
            when(clientIdentity.getId()).thenReturn(railwayAdminId);
            Lane lane = new Lane(laneIds[0], crossingId, 1, 0, false);
            when(stub.getState(laneCompositeKey.toString())).thenReturn(lane.toJSONString().getBytes(UTF_8));
            Crossing crossing = new Crossing(crossingId, laneIds, CrossingState.FREE_TO_CROSS, false, 0);
            when(stub.getState(crossingCompKey.toString())).thenReturn(crossing.toJSONString().getBytes(UTF_8));

            contract.deleteCrossing(ctx, crossingId);
            verify(stub,times(1)).delState(laneCompositeKey.toString());
            verify(stub,times(1)).delState(crossingCompKey.toString());
        }
        @Test
        public void deleteNonExistent(){
            String msp = "RailwayOrgMSP";
            when(clientIdentity.getMSPID()).thenReturn(msp);
            when(clientIdentity.getId()).thenReturn(railwayAdminId);
            when(stub.getState(crossingCompKey.toString())).thenReturn(new byte[] {});

            Exception thrown = assertThrows(ChaincodeException.class,() -> {
                contract.deleteCrossing(ctx, crossingId);
            });
            assertTrue(thrown.getMessage().contains("doesn't exist"));
        }

        @Test
        public void deleteNotAuthorized(){
            String msp = "NotRailwayOrgMSP";
            when(clientIdentity.getMSPID()).thenReturn(msp);
            when(stub.getState(crossingCompKey.toString())).thenReturn(new byte[] {});

            Exception thrown = assertThrows(ChaincodeException.class,() -> {
                contract.deleteCrossing(ctx, crossingId);
            });
            String errorMessage = "Must be part of " + "RailwayOrgMSP"+ " to perform this operation";
            assertEquals(errorMessage, thrown.getMessage());
            
        }
    }
    
    @Nested
    final class LaneExistsTest{
        private CompositeKey laneCompositeKey;
        private final String laneId = "01";
        
        @Test
        public void noProperLane(){
            laneCompositeKey = new CompositeKey(Lane.TYPE, laneId,crossingId);

            when(stub.createCompositeKey(Lane.TYPE, laneId,crossingId)).thenReturn(laneCompositeKey);
            when(stub.getState(laneCompositeKey.toString())).thenReturn(new byte[] {});

            boolean result = contract.laneExists(ctx,laneId,crossingId);

            assertFalse(result);
        }

        @Test
        public void laneExists() {

            laneCompositeKey = new CompositeKey(Lane.TYPE, laneId,crossingId);
            when(stub.getState(laneCompositeKey.toString())).thenReturn(new byte[] {42});
            when(stub.createCompositeKey(Lane.TYPE, laneId,crossingId)).thenReturn(laneCompositeKey);
            boolean result = contract.laneExists(ctx,laneId, crossingId);
            assertTrue(result);
        }

        @Test
        public void noKey() {
            String cId = "002";
            CompositeKey compositeKey = new CompositeKey(Lane.TYPE, laneId,cId);
            when(stub.createCompositeKey(Crossing.TYPE, cId)).thenReturn(compositeKey);

            when(stub.getState(compositeKey.toString())).thenReturn(null);
            boolean result = contract.crossingExists(ctx,cId);

            assertFalse(result);

        }

    }
    
    @Nested
    final class CreateLanes{
        
        private String laneId;
        private Crossing crossing;
        private final String msp = "RailwayOrgMSP";
        private CompositeKey laneCompositeKey;

        @BeforeEach
        void initCreation(){
            laneId = "01";
            laneCompositeKey = new CompositeKey(Lane.TYPE,laneId,crossingId);
            crossing = new Crossing(crossingId, new String[] {}, CrossingState.FREE_TO_CROSS, false, 0);
            when(stub.createCompositeKey(Lane.TYPE, laneId,crossingId)).thenReturn(laneCompositeKey);
        }

        @Test
        public void newLaneCreate() {

            when(stub.getState(laneCompositeKey.toString())).thenReturn(new byte[] {});
            when(stub.getState(crossingCompKey.toString())).thenReturn(crossing.toJSONString().getBytes(UTF_8));
            ClientIdentity clientIdentity = mock(ClientIdentity.class);
            when(clientIdentity.getId()).thenReturn(railwayAdminId);
            when(ctx.getClientIdentity()).thenReturn(clientIdentity);
            when(clientIdentity.getMSPID()).thenReturn(msp);
            when(stub.getTxTimestamp()).thenReturn(Instant.ofEpochMilli(0));

            Lane lane = new Lane(laneId, crossingId, 1, 0, false);
            Crossing updatedCrossing = new Crossing(crossingId, new String[] {laneId}, crossing.getState(), false, 60);
            contract.createLane(ctx, laneId, crossingId, lane.getCapacity());

            verify(stub).putState(crossingCompKey.toString(), updatedCrossing.toJSONString().getBytes(UTF_8));
            verify(stub).putState(laneCompositeKey.toString(), lane.toJSONString().getBytes(UTF_8));
        }

        @Test
        public void alreadyExists() {
            when(stub.getState(laneCompositeKey.toString())).thenReturn(new byte[] { 42 });
            when(stub.getState(crossingCompKey.toString())).thenReturn(new byte[] { 42 });
            ClientIdentity clientIdentity = mock(ClientIdentity.class);
            when(ctx.getClientIdentity()).thenReturn(clientIdentity);
            when(clientIdentity.getMSPID()).thenReturn(msp);
            when(clientIdentity.getId()).thenReturn(railwayAdminId);

            Exception thrown = assertThrows(ChaincodeException.class, () -> {
                contract.createLane(ctx, laneId, crossingId, 0);
            });

            assertTrue(thrown.getMessage().contains("already exists"));
        }
        
        @Test
        public void notFromRailwayOrg(){
            when(stub.getState(laneCompositeKey.toString())).thenReturn(new byte[] { 42 });
            when(stub.getState(crossingCompKey.toString())).thenReturn(new byte[] { 42 });
            ClientIdentity clientIdentity = mock(ClientIdentity.class);
            when(ctx.getClientIdentity()).thenReturn(clientIdentity);
            when(clientIdentity.getMSPID()).thenReturn("notRail");
            when(clientIdentity.getId()).thenReturn(railwayAdminId);

            Exception thrown = assertThrows(ChaincodeException.class, () -> {
                contract.createLane(ctx, laneId, crossingId, 0);
            });
            String errorMessage = "Must be part of " + msp + " to perform this operation";
            assertEquals(errorMessage, thrown.getMessage());
        }
        
    }
    
    @Nested
    class LaneDeletes {
        private String laneId;
        private CompositeKey laneCompositeKey;
        private ClientIdentity clientIdentity;

        @BeforeEach
        void initDeletion(){
            laneId = "01";
            laneCompositeKey = new CompositeKey(Lane.TYPE, laneId,crossingId);
            when(stub.createCompositeKey(Lane.TYPE, laneId,crossingId)).thenReturn(laneCompositeKey);
            clientIdentity = mock(ClientIdentity.class);
            when(ctx.getClientIdentity()).thenReturn(clientIdentity);
        }

        @Test
        public void deleteValid(){
            String msp = "RailwayOrgMSP";
            when(clientIdentity.getMSPID()).thenReturn(msp);
            when(clientIdentity.getId()).thenReturn(railwayAdminId);

            Lane lane = new Lane(laneId, crossingId, 1, 0, false);
            Crossing crossing = new Crossing(crossingId, new String[] {laneId}, CrossingState.FREE_TO_CROSS, false, 0);

            when(stub.getState(laneCompositeKey.toString())).thenReturn(lane.toJSONString().getBytes(UTF_8));
            when(stub.getState(crossingCompKey.toString())).thenReturn(crossing.toJSONString().getBytes(UTF_8));

            contract.deleteLane(ctx,laneId, crossingId);
            
            crossing.setLaneIds(new String[] {});
            verify(stub,times(1)).delState(laneCompositeKey.toString());
            verify(stub,times(1)).putState(crossingCompKey.toString(),crossing.toJSONString().getBytes(UTF_8));
        }
        @Test
        public void deleteNonExistent(){
            String msp = "RailwayOrgMSP";
            String nonExistentLane = "nonExistentLane";
            when(clientIdentity.getMSPID()).thenReturn(msp);
            when(clientIdentity.getId()).thenReturn(railwayAdminId);

            CompositeKey nonExistentCompositeKey = new CompositeKey(Lane.TYPE, nonExistentLane,crossingId);
            when(stub.getState(nonExistentCompositeKey.toString())).thenReturn(new byte[] {});
            when(stub.createCompositeKey(Lane.TYPE, nonExistentLane,crossingId)).thenReturn(nonExistentCompositeKey);

            Exception thrown = assertThrows(ChaincodeException.class,() -> {
                contract.deleteLane(ctx, nonExistentLane, crossingId);
            });
            assertTrue(thrown.getMessage().contains("doesn't exist"));
        }

        @Test
        public void deleteNotAuthorized(){
            String msp = "NotRailwayOrgMSP";
            when(clientIdentity.getMSPID()).thenReturn(msp);
            when(stub.getState(crossingCompKey.toString())).thenReturn(new byte[] {});

            Exception thrown = assertThrows(ChaincodeException.class,() -> {
                contract.deleteLane(ctx, laneId, crossingId);
            });
            String errorMessage = "Must be part of " + "RailwayOrgMSP"+ " to perform this operation";
            assertEquals(errorMessage, thrown.getMessage());
            
        }
    }
    
    @Nested
    class LaneReads{

        private CompositeKey laneCompKey;
        String laneId = "01";
        Lane lane;
        Crossing crossing;

        @BeforeEach
        void init(){
            crossing = new Crossing(crossingId, new String[] {laneId}, CrossingState.FREE_TO_CROSS, false, 0);
            lane = new Lane(laneId, crossingId, 1, 0, false);

            laneCompKey = new CompositeKey(Lane.TYPE, crossingId);
            when(stub.createCompositeKey(Lane.TYPE, laneId,crossingId)).thenReturn(laneCompKey);
        }
        @Test
        public void laneReadValid() {
            when(stub.getState(laneCompKey.toString())).thenReturn(lane.toJSONString().getBytes(UTF_8));
            when(stub.getState(crossingCompKey.toString())).thenReturn(crossing.toJSONString().getBytes(UTF_8));
            Lane returnedLane = contract.readLane(ctx,laneId,crossingId);
            assertEquals(lane,returnedLane);
        }
        @Test
        public void laneReadNonExistentLane() {
            when(stub.getState(crossingCompKey.toString())).thenReturn(crossing.toJSONString().getBytes(UTF_8));
            Exception thrown = assertThrows(ChaincodeException.class, () -> {
                contract.readLane(ctx, laneId, crossingId);
            });
            assertTrue(thrown.getMessage().contains("doesn't exist"));
        }
        
        @Test
        public void laneReadNonExistentCrossing(){
            when(stub.getState(laneCompKey.toString())).thenReturn(lane.toJSONString().getBytes(UTF_8));
            Exception thrown = assertThrows(ChaincodeException.class, () -> {
                contract.readLane(ctx, laneId, crossingId);
            });
            assertTrue(thrown.getMessage().contains("doesn't exist"));
        }

    }
    
    @Nested
    class TrainRequestTests {
        private String[] laneIds;
        private Crossing crossing;
        private final String msp = "RailwayOrgMSP";
        private CompositeKey laneCompositeKey;
        private Lane lane;
        private long requestId;

        @BeforeEach
        void init(){
            requestId = 1;
            laneIds = new String[] {"01"};
            laneCompositeKey = new CompositeKey(Lane.TYPE, laneIds[0],crossingId);
            crossing = new Crossing(crossingId, laneIds , CrossingState.FREE_TO_CROSS, false, 0);
            lane = new Lane(laneIds[0], crossingId, 1, 0, false);
            when(stub.createCompositeKey(Lane.TYPE, laneIds[0],crossingId)).thenReturn(laneCompositeKey);
            when(stub.createCompositeKey(Crossing.TYPE, crossingId)).thenReturn(crossingCompKey);
        }
        
        @Test
        public void trainRequestGranted(){
            when(stub.getTxTimestamp()).thenReturn(Instant.ofEpochMilli(0));
            ClientIdentity clientIdentity = mock(ClientIdentity.class);
            when(ctx.getClientIdentity()).thenReturn(clientIdentity);
            when(clientIdentity.getMSPID()).thenReturn(msp);
            when(clientIdentity.getId()).thenReturn(railwayAdminId);
            Lane lane = new Lane(laneIds[0], crossingId, 1, 0, false);
            Request request = new Request(""+requestId, crossingId, "N/A", RequesterRole.TRAIN, true, true);
            CompositeKey requestCompositeKey = new CompositeKey(Request.TYPE, "" + this.requestId, "N/A", crossingId);
            RequestPrivateData privateData = new RequestPrivateData(""+requestId, "N/A", crossingId, railwayAdminId);
            CompositeKey requestPrivateDataCompositeKey = new CompositeKey(RequestPrivateData.COLLECTION_NAME, ""+requestId, "N/A", crossingId);

            when(stub.createCompositeKey(RequestPrivateData.COLLECTION_NAME, ""+requestId, "N/A", crossingId)).thenReturn(requestPrivateDataCompositeKey);
            when(stub.createCompositeKey(Request.TYPE, ""+requestId, "N/A", crossingId)).thenReturn(requestCompositeKey);

            when(stub.getState(laneCompositeKey.toString())).thenReturn(lane.toJSONString().getBytes(UTF_8));
            when(stub.getState(crossingCompKey.toString())).thenReturn(crossing.toJSONString().getBytes(UTF_8));

            Request returnedRequest = contract.requestTrainCrossing(ctx, crossingId);

            assertTrue(returnedRequest.isGranted());

            crossing.setPriorityLock(true);
            crossing.setState(CrossingState.LOCKED);
            verify(stub).putState(crossingCompKey.toString(), crossing.toJSONString().getBytes(UTF_8));
            MessageDigest digest;
            try {
                digest = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException e) {
                throw new ChaincodeException("");
            }
            byte[] hash = digest.digest(privateData.toJSONString().getBytes(UTF_8));
    
            
            lane.setPriorityLock(true);
            verify(stub).putState(laneCompositeKey.toString(), lane.toJSONString().getBytes(UTF_8));
            verify(stub).putState(requestCompositeKey.toString(), request.toJSONString().getBytes(UTF_8));
            verify(stub).putState(requestPrivateDataCompositeKey.toString(), hash);
        }

        @Test
        public void trainRequestDeniedFreeToCrossExpired(){
            when(stub.getTxTimestamp()).thenReturn(Instant.ofEpochMilli(99999));
            ClientIdentity clientIdentity = mock(ClientIdentity.class);
            when(ctx.getClientIdentity()).thenReturn(clientIdentity);
            when(clientIdentity.getMSPID()).thenReturn(msp);
            when(clientIdentity.getId()).thenReturn(railwayAdminId);
            Lane lane = new Lane(laneIds[0], crossingId, 1, 0, false);
            Request request = new Request(""+requestId, crossingId, "N/A", RequesterRole.TRAIN, false,false);
            CompositeKey requestCompositeKey = new CompositeKey(Request.TYPE, "" + this.requestId, "N/A", crossingId);
            RequestPrivateData privateData = new RequestPrivateData(""+requestId, "N/A", crossingId, railwayAdminId);
            CompositeKey requestPrivateDataCompositeKey = new CompositeKey(RequestPrivateData.COLLECTION_NAME, ""+requestId, "N/A", crossingId);

            when(stub.createCompositeKey(RequestPrivateData.COLLECTION_NAME, ""+requestId, "N/A", crossingId)).thenReturn(requestPrivateDataCompositeKey);
            when(stub.createCompositeKey(Request.TYPE, ""+requestId, "N/A", crossingId)).thenReturn(requestCompositeKey);

            when(stub.getState(laneCompositeKey.toString())).thenReturn(lane.toJSONString().getBytes(UTF_8));
            when(stub.getState(crossingCompKey.toString())).thenReturn(crossing.toJSONString().getBytes(UTF_8));

            Request returnedRequest = contract.requestTrainCrossing(ctx, crossingId);

            assertFalse(returnedRequest.isGranted());

            crossing.setPriorityLock(true);
            crossing.setState(CrossingState.LOCKED);
            verify(stub).putState(crossingCompKey.toString(), crossing.toJSONString().getBytes(UTF_8));
            MessageDigest digest;
            try {
                digest = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException e) {
                throw new ChaincodeException("");
            }
            byte[] hash = digest.digest(privateData.toJSONString().getBytes(UTF_8));
    
            
            lane.setPriorityLock(true);
            verify(stub).putState(laneCompositeKey.toString(), lane.toJSONString().getBytes(UTF_8));
            verify(stub).putState(requestCompositeKey.toString(), request.toJSONString().getBytes(UTF_8));
            verify(stub).putState(requestPrivateDataCompositeKey.toString(), hash);
        }

        @Test
        public void trainRequestDeniedCrossingLocked(){
            crossing.setState(CrossingState.LOCKED);
            when(stub.getTxTimestamp()).thenReturn(Instant.ofEpochMilli(0));
            ClientIdentity clientIdentity = mock(ClientIdentity.class);
            when(ctx.getClientIdentity()).thenReturn(clientIdentity);
            when(clientIdentity.getMSPID()).thenReturn(msp);
            when(clientIdentity.getId()).thenReturn(railwayAdminId);
            Lane lane = new Lane(laneIds[0], crossingId, 1, 0, false);
            Request request = new Request(""+requestId, crossingId, "N/A", RequesterRole.TRAIN, false,false);
            CompositeKey requestCompositeKey = new CompositeKey(Request.TYPE, "" + this.requestId, "N/A", crossingId);
            RequestPrivateData privateData = new RequestPrivateData(""+requestId, "N/A", crossingId, railwayAdminId);
            CompositeKey requestPrivateDataCompositeKey = new CompositeKey(RequestPrivateData.COLLECTION_NAME, ""+requestId, "N/A", crossingId);

            when(stub.createCompositeKey(RequestPrivateData.COLLECTION_NAME, ""+requestId, "N/A", crossingId)).thenReturn(requestPrivateDataCompositeKey);
            when(stub.createCompositeKey(Request.TYPE, ""+requestId, "N/A", crossingId)).thenReturn(requestCompositeKey);

            when(stub.getState(laneCompositeKey.toString())).thenReturn(lane.toJSONString().getBytes(UTF_8));
            when(stub.getState(crossingCompKey.toString())).thenReturn(crossing.toJSONString().getBytes(UTF_8));

            Request returnedRequest = contract.requestTrainCrossing(ctx, crossingId);

            assertFalse(returnedRequest.isGranted());

            crossing.setPriorityLock(true);
            crossing.setState(CrossingState.LOCKED);
            verify(stub).putState(crossingCompKey.toString(), crossing.toJSONString().getBytes(UTF_8));
            MessageDigest digest;
            try {
                digest = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException e) {
                throw new ChaincodeException("");
            }
            byte[] hash = digest.digest(privateData.toJSONString().getBytes(UTF_8));
    
            
            lane.setPriorityLock(true);
            verify(stub).putState(laneCompositeKey.toString(), lane.toJSONString().getBytes(UTF_8));
            verify(stub).putState(requestCompositeKey.toString(), request.toJSONString().getBytes(UTF_8));
            verify(stub).putState(requestPrivateDataCompositeKey.toString(), hash);
        }


    }
    
}
