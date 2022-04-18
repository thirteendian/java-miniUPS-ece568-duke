package edu.duke.ece568.server;

import edu.duke.ece568.shared.Status;
import org.junit.jupiter.api.Test;

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
        postgreSQLJDBC.addShipment(1L,4L,5L,1L,1L,"vcm@duke.edu",new Status().pInWarehouse);
        assertEquals(postgreSQLJDBC.getShipmentDestX(1L),4L);
        assertEquals(postgreSQLJDBC.getShipmentDestY(1L),5L);
        assertEquals(postgreSQLJDBC.getShipmentTruckID(1L),1L);
        assertEquals(postgreSQLJDBC.getShipmentWarehouseID(1L),1L);
        assertEquals(postgreSQLJDBC.getShipmentEmailAddress(1L),"vcm@duke.edu");
        assertEquals(postgreSQLJDBC.getShipmentStatus(1L),new Status().pInWarehouse);
        postgreSQLJDBC.updateShipmentStatus(1L, new Status().pDelivered);
        assertEquals(postgreSQLJDBC.getShipmentStatus(1L),new Status().pDelivered);
        assertEquals(postgreSQLJDBC.getShipmentTableSize(),1);
        postgreSQLJDBC.deleteShipment(1L);
        assertEquals(postgreSQLJDBC.getShipmentTableSize(),0);
        //Test UGopickUp
        postgreSQLJDBC.addUGoPickup(3L,1L);
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
        postgreSQLJDBC.addShipment(1L,5L,6L,2L,1L,"vcm@duke.edu",new Status().pInWarehouse);
        postgreSQLJDBC.addUShipmentStatusUpdate(5L,1L,2L);
        assertEquals(postgreSQLJDBC.getUShipmentStatusUpdatePackageID(5L),1L);
        assertEquals(postgreSQLJDBC.getUShipmentStatusUpdateTruckID(5L),2L);
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