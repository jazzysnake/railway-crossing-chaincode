/*
 * SPDX-License-Identifier: Apache License 2.0
 */

package org.example;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.apache.log4j.Logger;

import static org.junit.jupiter.api.Assertions.*;



final class CrossingContractTest {
    public static Logger log = Logger.getLogger(CrossingContractTest.class.getName());

    //@Nested
    //class AssetExists {
    //    @Test
    //    public void noProperAsset() {

    //        CrossingContract contract = new  CrossingContract();
    //        Context ctx = mock(Context.class);
    //        ChaincodeStub stub = mock(ChaincodeStub.class);
    //        when(ctx.getStub()).thenReturn(stub);

    //        when(stub.getState("10001")).thenReturn(new byte[] {});
    //        boolean result = contract.crossingExists(ctx,"10001");

    //        assertFalse(result);
    //    }

    //    @Test
    //    public void assetExists() {

    //        CrossingContract contract = new  CrossingContract();
    //        Context ctx = mock(Context.class);
    //        ChaincodeStub stub = mock(ChaincodeStub.class);
    //        when(ctx.getStub()).thenReturn(stub);

    //        when(stub.getState("10001")).thenReturn(new byte[] {42});
    //        boolean result = contract.crossingExists(ctx,"10001");

    //        assertTrue(result);

    //    }

    //    @Test
    //    public void noKey() {
    //        CrossingContract contract = new  CrossingContract();
    //        Context ctx = mock(Context.class);
    //        ChaincodeStub stub = mock(ChaincodeStub.class);
    //        when(ctx.getStub()).thenReturn(stub);

    //        when(stub.getState("10002")).thenReturn(null);
    //        boolean result = contract.crossingExists(ctx,"10002");

    //        assertFalse(result);

    //    }

    //}

    //@Nested
    //class AssetCreates {

    //    @Test
    //    public void newAssetCreate() {
    //        CrossingContract contract = new  CrossingContract();
    //        Context ctx = mock(Context.class);
    //        ChaincodeStub stub = mock(ChaincodeStub.class);
    //        when(ctx.getStub()).thenReturn(stub);

    //        String json = "{\"value\":\"TheCrossing\"}";

    //        //contract.createCrossing(ctx, "10001", "TheCrossing");

    //        verify(stub).putState("10001", json.getBytes(UTF_8));
    //    }

    //    @Test
    //    public void alreadyExists() {
    //        CrossingContract contract = new  CrossingContract();
    //        Context ctx = mock(Context.class);
    //        ChaincodeStub stub = mock(ChaincodeStub.class);
    //        when(ctx.getStub()).thenReturn(stub);

    //        when(stub.getState("10002")).thenReturn(new byte[] { 42 });

    //        Exception thrown = assertThrows(RuntimeException.class, () -> {
    //            //contract.createCrossing(ctx, "10002", "TheCrossing");
    //        });

    //        assertEquals(thrown.getMessage(), "The asset 10002 already exists");

    //    }

    //}

    //@Test
    //public void assetRead() {
    //    CrossingContract contract = new  CrossingContract();
    //    Context ctx = mock(Context.class);
    //    ChaincodeStub stub = mock(ChaincodeStub.class);
    //    when(ctx.getStub()).thenReturn(stub);

    //    //Crossing asset = new  Crossing();
    //    //asset.setValue("Valuable");

    //    //String json = asset.toJSONString();
    //    //when(stub.getState("10001")).thenReturn(json.getBytes(StandardCharsets.UTF_8));

    //    Crossing returnedAsset = contract.readCrossing(ctx, "10001");
    //    //assertEquals(returnedAsset.getValue(), asset.getValue());
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
