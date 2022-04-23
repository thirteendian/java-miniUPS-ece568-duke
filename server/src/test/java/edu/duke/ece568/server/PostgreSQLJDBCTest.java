package edu.duke.ece568.server;

import edu.duke.ece568.server.Amazon.AShippingNumCounter;
import edu.duke.ece568.shared.Status;
import org.junit.jupiter.api.Test;

import java.sql.Array;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class PostgreSQLJDBCTest {
    @Test
    public void test_PostgreSQLJDBC_TruckWareHouseShipment() {
        PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC();
        postgreSQLJDBC.dropAllTable();
        postgreSQLJDBC.createAllTable();
        postgreSQLJDBC.addTruck(1L, 1, 2, new Status().tIdel, false);
        postgreSQLJDBC.addTruck(2L, 1, 2, new Status().tTraveling, false);
        postgreSQLJDBC.addTruck(3L, 1, 2, new Status().tArriveWarehouse, false);
        postgreSQLJDBC.addTruck(4L, 1, 2, new Status().tLoading, false);
        assertEquals(postgreSQLJDBC.getTruckX(1L),1);
        assertEquals(postgreSQLJDBC.getTruckY(1L),2);
        assertEquals(postgreSQLJDBC.getTruckStatus(1L), new Status().tIdel);
        assertEquals(postgreSQLJDBC.getTruckStatus(2L), new Status().tTraveling);
        assertEquals(postgreSQLJDBC.getTruckStatus(3L), new Status().tArriveWarehouse);
        assertEquals(postgreSQLJDBC.getTruckStatus(4L), new Status().tLoading);
        postgreSQLJDBC.addWarehouse(1L,2L,3L);
        assertEquals(postgreSQLJDBC.getWarehouseX(1L), 2L);
        assertEquals(postgreSQLJDBC.getWarehouseY(1L), 3L);
        //Test Shipment
        postgreSQLJDBC.addShipment(1L,4L,5L,1L,"vcm@duke.edu",new Status().pInWarehouse,99L);
        postgreSQLJDBC.addShipment(2L,4L,5L,1L,"vcm@duke.edu",new Status().pInWarehouse,99L);
        postgreSQLJDBC.addShipment(3L,4L,5L,1L,"vcm@duke.edu",new Status().pInWarehouse,99L);
        postgreSQLJDBC.addShipment(4L,4L,5L,1L,"vcm@duke.edu",new Status().pInWarehouse,99L);
        assertEquals(postgreSQLJDBC.getShipmentDestX(1L),4L);
        assertEquals(postgreSQLJDBC.getShipmentDestY(1L),5L);
        postgreSQLJDBC.updateShipmentTruckID(1L,1L);
        assertEquals(postgreSQLJDBC.getShipmentTruckID(1L),1L);
        assertEquals(postgreSQLJDBC.getShipmentWarehouseID(1L),1L);
        assertEquals(postgreSQLJDBC.getShipmentEmailAddress(1L),"vcm@duke.edu");
        assertEquals(postgreSQLJDBC.getShipmentStatus(1L),new Status().pInWarehouse);
        postgreSQLJDBC.updateShipmentStatus(1L, new Status().pDelivered);
        assertEquals(postgreSQLJDBC.getShipmentStatus(1L),new Status().pDelivered);
        assertEquals(postgreSQLJDBC.getShipmentTableSize(),4);
        postgreSQLJDBC.deleteShipment(1L);
        assertEquals(postgreSQLJDBC.getShipmentTableSize(),3);
        assertEquals(postgreSQLJDBC.getShipmentPackageIDWithShippingID(99L).size(),3);
        postgreSQLJDBC.updateShipmentTruckID(4L,100L);
        assertTrue(postgreSQLJDBC.getShipmentPackageIDWithTruckID(100L).contains(4L));
        //Test UGopickUp
        postgreSQLJDBC.addUGoPickup(3L,1L, 3L);
        assertTrue(postgreSQLJDBC.isUGoPickupContainsWseq(3L));
        assertEquals(postgreSQLJDBC.getUGOPickupASeqNum(3L),3L);
        assertEquals(postgreSQLJDBC.getUGOPickupTruckID(3L),1L);
        assertEquals(postgreSQLJDBC.getUGoPickupTableSize(),1);
        postgreSQLJDBC.deleteUGoPickUp(3L);
        assertEquals(postgreSQLJDBC.getUGoPickupTableSize(),0);
        //Test Query
        postgreSQLJDBC.addQuery(4L,1L);
        assertEquals(postgreSQLJDBC.getQueryTruckID(4L),1L);
        postgreSQLJDBC.deleteQuery(4L);
        assertEquals(postgreSQLJDBC.getQueryTableSize(),0);
        //Test UShipmentStatusUpdate
        postgreSQLJDBC.addWarehouse(2L,6L,7L);
        postgreSQLJDBC.addShipment(1L,5L,6L,2L,"vcm@duke.edu",new Status().pInWarehouse,100L);
        postgreSQLJDBC.addUShipmentStatusUpdate(5L,1L,2L);
        ArrayList<Long> arrayList = postgreSQLJDBC.getAllUShipmentStatusUpdate();

        assertEquals(postgreSQLJDBC.getUShipmentStatusUpdateTableSize(),1);
        postgreSQLJDBC.deleteUShipmentStatusUpdate(5L);
        assertEquals(postgreSQLJDBC.getUShipmentStatusUpdateTableSize(),0);
        //Test UShippingResponse
        postgreSQLJDBC.addUShippingResponse(6L,1L);
        assertEquals(postgreSQLJDBC.getUShippingResponseTruckID(6L),1L);
        postgreSQLJDBC.deleteUShippingResponse(6L);
        assertEquals(postgreSQLJDBC.getUShippingResponseTableSize(),0);
        //Test UTruckArrivedNotification
        postgreSQLJDBC.addUTruckArrivedNotification(6L,1L);
        assertEquals(postgreSQLJDBC.getUTruckArrivedNotificationTruckID(6L),1L);
        postgreSQLJDBC.deleteUTruckArrivedNotification(6L);
        assertEquals(postgreSQLJDBC.getUTruckArrivedNotificationTableSize(),0);
        //Test UGoDeliver
        postgreSQLJDBC.addUGoDeliver(7L,1L,2L);
        assertEquals(postgreSQLJDBC.getUGoDeliverTruckID(7L),2L);
        assertEquals(postgreSQLJDBC.getUGoDeliverPackageID(7L),1L);
        postgreSQLJDBC.deleteUGoDeliver(7L);
        assertEquals(postgreSQLJDBC.getUGoDeliverTableSize(),0);
        //Test ShippingRequest
        postgreSQLJDBC.addShippingRequest(AShippingNumCounter.getInstance().getCurrSeqNum(), 137L);
        postgreSQLJDBC.addShippingRequest(AShippingNumCounter.getInstance().getCurrSeqNum(),124L);
        postgreSQLJDBC.addShippingRequest(AShippingNumCounter.getInstance().getCurrSeqNum(),111L);
        ArrayList<Long> ans = postgreSQLJDBC.getNumOfShippingRequestByOrder(3);
        assertEquals(postgreSQLJDBC.getShippingRequestASeqNum(ans.get(0)),137L);
        postgreSQLJDBC.deleteShippingRequest(ans.get(1));
        ans = postgreSQLJDBC.getNumOfShippingRequestByOrder(3);
        assertEquals(ans.size(),2);
        assertEquals(postgreSQLJDBC.getShippingRequestASeqNum(ans.get(1)),111L);
        }

    @Test
    public void test_PostgreSQLJDBC_addAuthentication() {
        PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC();
        postgreSQLJDBC.dropAllTable();
        postgreSQLJDBC.createAllTable();
        postgreSQLJDBC.addAuthentication("vcm@duke.edu","12345");
        assertFalse(postgreSQLJDBC.isPasswordCorrect("vmc@duke.edu","12345"));
        assertFalse(postgreSQLJDBC.isPasswordCorrect("vcm@duke.edu","12432"));
        assertTrue(postgreSQLJDBC.isPasswordCorrect("vcm@duke.edu","12345"));
        postgreSQLJDBC.updateAuthentication("vcm@duke.edu","23456");
        assertTrue(postgreSQLJDBC.isPasswordCorrect("vmc@duke.edu","23456"));
    }

}