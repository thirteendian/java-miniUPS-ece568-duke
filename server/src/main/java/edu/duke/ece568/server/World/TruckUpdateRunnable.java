package edu.duke.ece568.server.World;

import edu.duke.ece568.server.Amazon.ASeqNumCounter;
import edu.duke.ece568.server.Amazon.AmazonCommand;
import edu.duke.ece568.server.Amazon.AmazonResponse;
import edu.duke.ece568.server.protocol.UpsAmazon;
import edu.duke.ece568.server.protocol.WorldUps;
import edu.duke.ece568.shared.PostgreSQLJDBC;
import edu.duke.ece568.shared.Status;

import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;

public class TruckUpdateRunnable implements Runnable {
    private Socket serverToWorldSocket;
    private Socket serverToAmazonSocket;
    private AmazonCommand amazonCommand;
    private WorldCommand worldCommand;
    private AmazonResponse amazonResponse;
//    private HashSet<Long>

    public TruckUpdateRunnable(Socket serverToWorldSocket, Socket serverToAmazonSocket, AmazonCommand amazonCommand, WorldCommand worldCommand, AmazonResponse amazonResponse) {
        this.serverToWorldSocket = serverToWorldSocket;
        this.serverToAmazonSocket = serverToAmazonSocket;
        this.amazonCommand = amazonCommand;
        this.worldCommand = worldCommand;
        this.amazonResponse = amazonResponse;
    }

    private void popTruckwithClosestDistance(ArrayList<Long> idelTruckID) {
        PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC();
        for (Integer i = 0; i < idelTruckID.size(); i++) {
            postgreSQLJDBC.getTruckStatus(idelTruckID.get(i));
        }
    }

    @Override
    public void run() {
        System.out.println("TruckUpdateRunnable start!");
        while (true) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            WorldUps.UCommands.Builder uCommands = WorldUps.UCommands.newBuilder();
            UpsAmazon.AUResponse.Builder auResponse = UpsAmazon.AUResponse.newBuilder();
            PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC();
            /********************************Search Idel Truck, Send UGoPickUp*****************************************/
            ArrayList<Long> IdelTruckId = postgreSQLJDBC.getTruckGroupOfStatus(new Status().tIdel);
            popTruckwithClosestDistance(IdelTruckId);
            //Get Number of ShippingRequest by Limit of IdelTruck Number (<= IdelTruckNumber)
            ArrayList<Long> shippingRequestID = postgreSQLJDBC.getNumOfShippingRequestByOrder(IdelTruckId.size());
//            System.out.println("[TUR]     All ShippingRequestID: " + shippingRequestID);
//            System.out.println("[TUR] Search Idel Truck");
            for (Integer i = 0; i < shippingRequestID.size(); i++) {

                Long truckId = IdelTruckId.get(i);
                //SELECT PACKAGE ID FROM SHIPMENT WHERE SHIPPINGID = id
                //get all packages within one shippingrequest that has same warehouseid
                ArrayList<Long> packageIDs = postgreSQLJDBC.getShipmentPackageIDWithShippingID(shippingRequestID.get(i));
                if (packageIDs.size() != 0) {
                    for (Long packageId : packageIDs) {
                        postgreSQLJDBC.updateShipmentTruckID(packageId, IdelTruckId.get(i));
                    }

                    //Corresponding AShippingRquest ASeqNum
                    Long ASeqNum = postgreSQLJDBC.getShippingRequestASeqNum(shippingRequestID.get(i));

                    //New WSeqNum
                    Long wSeqNum = WSeqNumCounter.getInstance().getCurrSeqNum();

                    System.out.println("[TUR]     [Match Idel Truck] [ShippingRequest] DELETE ShippingRequestID: " + shippingRequestID.get(i) + " ASeqNum: " + ASeqNum + "PackageIDs : " + packageIDs);
                    //delete this shipping request
                    postgreSQLJDBC.deleteShippingRequest(shippingRequestID.get(i));
                    //Add UGoPickUp to uCommands
                    Long wareHouseID = postgreSQLJDBC.getShipmentWarehouseID(packageIDs.get(0));//Any package should contains the same wareHouse ID
                    WorldUps.UGoPickup.Builder uGoPickup = WorldUps.UGoPickup.newBuilder();
                    //Note since all packages has same warehouseid within same Shipment
                    uGoPickup.setTruckid(Math.toIntExact(truckId)).setSeqnum(wSeqNum).setWhid(Math.toIntExact(wareHouseID));
                    uCommands.addPickups(uGoPickup);
                    //Add UGoPickUp to Resend
                    postgreSQLJDBC.addUGoPickup(wSeqNum, truckId, ASeqNum, wareHouseID);
                    System.out.println("[TUR]    [UGoPickUp] : id: " + truckId + " wSeqNum: " + wSeqNum);
                }
            }

            /********************************Send U Query to Ask Truck Status*****************************************/
            WorldUps.UQuery.Builder uQuery = WorldUps.UQuery.newBuilder();
            for (Long truckID : postgreSQLJDBC.getAllTruckID()) {
                Long wSeqNum = WSeqNumCounter.getInstance().getCurrSeqNum();
                uQuery.setSeqnum(wSeqNum).setTruckid(Math.toIntExact(truckID));
                uCommands.addQueries(uQuery);
//                System.out.println("[TUR]    [UQuery] : id: "+ truckID + " wSeqNum: " + wSeqNum);
            }
            /********************************Send UtruckedArrivedNotification to Amazon********************************/
            ArrayList<Long> arriveWarehouseTruckID = postgreSQLJDBC.getTruckGroupOfStatus(new Status().tArriveWarehouse);
            for (Long truckID : arriveWarehouseTruckID) {
//                System.out.println("[TUR]    IsSentUTruckArrived: "+ postgreSQLJDBC.getTruckIsSentUTruckArrived(truckID));
                if (!postgreSQLJDBC.getTruckIsSentUTruckArrived(truckID)) {
                    //Send To Amazon UtruckArrivedNotification
                    Long aSeqNum = ASeqNumCounter.getInstance().getCurrSeqNum();
                    UpsAmazon.UTruckArrivedNotification.Builder uTruckArrivedNotification = UpsAmazon.UTruckArrivedNotification.newBuilder();
                    uTruckArrivedNotification.setTruckId(truckID).setSeqnum(aSeqNum);
                    auResponse.addArrived(uTruckArrivedNotification.build());
                    //Add to Resend
                    postgreSQLJDBC.addUTruckArrivedNotification(aSeqNum, truckID);
                    postgreSQLJDBC.updateTruckStatus(truckID, null, null, null, true, null);
                    System.out.println("[TUR]       [UTrukArrivedNotification] sent to Amazon : id: " + truckID + " aSeqNum: " + aSeqNum);
                }
            }
//            /********************************Send UGoDeliver to World For Loaded Truck********************************/
//            ArrayList<Long> loadedTruckID = postgreSQLJDBC.getTruckGroupOfStatus(new Status().tLoaded);
//            System.out.println("[TUR]     LoadedTruckID: " + loadedTruckID);
//            for (Long truckID : loadedTruckID) {
//                if (postgreSQLJDBC.getTruckIsSentUGoDeliver(truckID)) {
//                    Long wSeqNum = WSeqNumCounter.getInstance().getCurrSeqNum();
//                    WorldUps.UGoDeliver.Builder UGoDeliver = WorldUps.UGoDeliver.newBuilder();
//                    UGoDeliver.setTruckid(Math.toIntExact(truckID)).setSeqnum(wSeqNum);
//                    for (Long packageID : postgreSQLJDBC.getShipmentPackageIDWithTruckID(truckID)) {
//                        WorldUps.UDeliveryLocation.Builder uDeliveryLocation = WorldUps.UDeliveryLocation.newBuilder();
//                        uDeliveryLocation.setX(Math.toIntExact(postgreSQLJDBC.getShipmentDestX(packageID))).setY(Math.toIntExact(postgreSQLJDBC.getShipmentDestY(packageID)));
//                        uDeliveryLocation.setPackageid(packageID);
//                        UGoDeliver.addPackages(uDeliveryLocation.build());
//                    }
//                    uCommands.addDeliveries(UGoDeliver.build());
//
//                    //UGoDeliver Change WorldSpeed Faster
//                    uCommands.setSimspeed(500);
//
//                    postgreSQLJDBC.addUGoDeliver(wSeqNum, truckID);
//                    System.out.println("[TUR]    [UGoDeliver] : id: " + Math.toIntExact(truckID) + " wSeqNum: " + wSeqNum + " truckStatus: " + postgreSQLJDBC.getTruckStatus(truckID));
//                    postgreSQLJDBC.updateTruckStatus(truckID,null,null,null,null,false);
//                }
//            }
            postgreSQLJDBC.close();
            worldCommand.sendUCommand(uCommands.build());
            amazonCommand.sendAUResponse(auResponse.build());
        }
    }
}
