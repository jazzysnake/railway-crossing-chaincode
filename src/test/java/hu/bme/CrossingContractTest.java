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

            Crossing cross = new Crossing(crossingId, laneIds, crossing.getState(), crossing.isPriorityLock(), 0+300*60);
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

        private String msp = "RailwayOrgMSP";
        @Test
        public void crossingReadValid() {
            String[] laneIds = new String[1];
            laneIds[0] = "01";
            Crossing crossing = new Crossing(crossingId, laneIds, CrossingState.FREE_TO_CROSS, false, 0);
            when(stub.getState(crossingCompKey.toString())).thenReturn(crossing.toJSONString().getBytes(UTF_8));
            ClientIdentity clientIdentity = mock(ClientIdentity.class);
            when(ctx.getClientIdentity()).thenReturn(clientIdentity);
            when(clientIdentity.getMSPID()).thenReturn(msp);

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
    
    //@Nested
    //public class PrivateDataTest{
    //    long requestId = 1;
    //    @Test
    //    public void savingPrivateDataValid(){
    //        CompositeKey compkey = new CompositeKey(RequestPrivateData.COLLECTION_NAME, ""+requestId,"N/A",crossingId);
    //        when(stub.createCompositeKey(RequestPrivateData.COLLECTION_NAME, ""+requestId,"N/A",crossingId))
    //        .thenReturn(compkey);
    //        contract.recordClientIdentity(ctx, ""+requestId, "N/A", crossingId,railwayAdminId);
    //        ClientIdentity clientIdentity = mock(ClientIdentity.class);
    //        when(clientIdentity.getId()).thenReturn(railwayAdminId);
    //        verify(stub).putPrivateData(RequestPrivateData.COLLECTION_NAME, compkey.toString(), railwayAdminId.getBytes(UTF_8));
    //    }
    //}

    //@Nested
    //class AssetUpdates {
    //    @Test
    //    public void updateExisting() {
    //        CrossingContract contract = new  CrossingContract();
    //        Context ctx = mock(Context.class);
    //        ChaincodeStub stub = mock(ChaincodeStub.class);
    //        when(ctx.getStub()).thenReturn(stub);
    //        when(stub.getState("10001")).thenReturn(new byte[] { 42 });

    //        //contract.updateCrossing(ctx, "10001", "updates");

    //        String json = "{\"value\":\"updates\"}";
    //        verify(stub).putState("10001", json.getBytes(UTF_8));
    //    }

    //    @Test
    //    public void updateMissing() {
    //        CrossingContract contract = new  CrossingContract();
    //        Context ctx = mock(Context.class);
    //        ChaincodeStub stub = mock(ChaincodeStub.class);
    //        when(ctx.getStub()).thenReturn(stub);

    //        when(stub.getState("10001")).thenReturn(null);

    //        Exception thrown = assertThrows(RuntimeException.class, () -> {
    //            //contract.updateCrossing(ctx, "10001", "TheCrossing");
    //        });

    //        assertEquals(thrown.getMessage(), "The asset 10001 does not exist");
    //    }

    //}

    //@Test
    //public void assetDelete() {
    //    CrossingContract contract = new  CrossingContract();
    //    Context ctx = mock(Context.class);
    //    ChaincodeStub stub = mock(ChaincodeStub.class);
    //    when(ctx.getStub()).thenReturn(stub);
    //    when(stub.getState("10001")).thenReturn(null);

    //    Exception thrown = assertThrows(RuntimeException.class, () -> {
    //        contract.deleteCrossing(ctx, "10001");
    //    });

    //    assertEquals(thrown.getMessage(), "The asset 10001 does not exist");
    //}

}
