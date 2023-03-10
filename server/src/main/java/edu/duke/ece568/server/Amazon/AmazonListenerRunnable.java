package edu.duke.ece568.server.Amazon;

import edu.duke.ece568.server.World.WSeqNumCounter;
import edu.duke.ece568.server.World.WorldCommand;
import edu.duke.ece568.server.protocol.UpsAmazon;
import edu.duke.ece568.server.protocol.WorldUps;
import edu.duke.ece568.shared.PostgreSQLJDBC;
import edu.duke.ece568.shared.Status;

import java.io.IOException;
import java.net.Socket;
import java.util.HashSet;
import java.util.Properties;
import java.util.UUID;

public class AmazonListenerRunnable implements Runnable {
    private Socket serverToWorldSocket;
    private Socket serverToAmazonSocket;
    private AmazonCommand amazonCommand;
    private WorldCommand worldCommand;
    private AmazonResponse amazonResponse;
    private HashSet<Long> aTruckLoadedNotificationASeqNumHashSet;

    public AmazonListenerRunnable(Socket serverToWorldSocket, Socket serverToAmazonSocket, AmazonResponse amazonResponse, AmazonCommand amazonCommand, WorldCommand worldCommand) {
        this.serverToWorldSocket = serverToWorldSocket;
        this.serverToAmazonSocket = serverToAmazonSocket;
        this.amazonCommand = amazonCommand;
        this.worldCommand = worldCommand;
        this.amazonResponse = amazonResponse;
        this.aTruckLoadedNotificationASeqNumHashSet = new HashSet<>();
    }

    @Override
    public void run() {
        System.out.println("AmazonListenerRunnable start !");
        while (true) {
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
            try {
                UpsAmazon.AURequest.Builder builder = amazonResponse.recvFromAmazon();
                UpsAmazon.AUResponse.Builder auResponseBuilder = UpsAmazon.AUResponse.newBuilder();
                PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC();

                /**********************************DELETE ALL RESEND THAT ACKED****************************************/
                for (Long ack : builder.getAcksList()) {
                    postgreSQLJDBC.deleteUShippingResponse(ack);
                    postgreSQLJDBC.deleteUShipmentStatusUpdate(ack);
                    postgreSQLJDBC.deleteUTruckArrivedNotification(ack);
                    System.out.println("[Delete Amazon Resend]: ack: " + ack);
                }

                /********************************RECEIVE ASHIPPING REQUEST*********************************************/
                for (UpsAmazon.AShippingRequest aShippingRequest : builder.getShippingRequestList()) {
                    //TODO: Avoid do same seqnum's ShippingRequest from amazon
                    //AShippingRequest ACK
                    auResponseBuilder.addAcks(aShippingRequest.getSeqnum());
                    if (!postgreSQLJDBC.isShippingRequestExist(aShippingRequest.getSeqnum())) {
                        //Add ShippingRequest
                        Long shippingID = AShippingNumCounter.getInstance().getCurrSeqNum();//ASC
                        postgreSQLJDBC.addShippingRequest(shippingID, aShippingRequest.getSeqnum());
                        //Add All Shipment Table
                        //TODO: insert warehouse should be atomic
                        for (UpsAmazon.AShipment aShipment : aShippingRequest.getShipmentList()) {
                            //Add Warehouse Table
                            if (!postgreSQLJDBC.isWarehouseExist((long) aShippingRequest.getLocation().getWarehouseid())) {
                                postgreSQLJDBC.addWarehouse((long) aShippingRequest.getLocation().getWarehouseid(), aShippingRequest.getLocation().getX(), aShippingRequest.getLocation().getY());
                            }
                            //TODO: insert package should be atomic
                            if (!postgreSQLJDBC.isShipmentPackageIDExist(aShipment.getPackageId())) {
                                //Add AShipment Table
                                UUID uuid = UUID.randomUUID();
                                postgreSQLJDBC.addShipment(aShipment.getPackageId(), aShipment.getDestX(), aShipment.getDestY(), (long) aShippingRequest.getLocation().getWarehouseid(), aShipment.getEmailaddress(), new Status().pInWarehouse, shippingID, uuid.toString());
                                System.out.println("[ALR]      [AShippingRequest]: warehouse{ id: " + aShippingRequest.getLocation().getWarehouseid() + "x: " + aShippingRequest.getLocation().getX() + "y: " + aShippingRequest.getLocation().getY() + "}"
                                        + " Ashipment{packageID: " + aShipment.getPackageId() + "DestX: " + aShipment.getDestX() + "DestY: " + aShipment.getDestY() + "ShippingID: " + shippingID + " TrackingNum: " + uuid.toString() + "}");
                                //Add Package Table
                                for (UpsAmazon.Product product : aShipment.getProductList()) {
                                    postgreSQLJDBC.addProduct(aShipment.getPackageId(), product.getDescription(), (long) product.getCount());
                                    System.out.println("    [Product]: description: " + product.getDescription() + " count: " + product.getCount());
                                }

//                                //Send Email
//                                Properties prop = new Properties();
//                                prop.put("mail.smtp.auth", true)
//                                String emailAddress = aShipment.getEmailaddress();
//                                String emailHost = "dukeece568yxkc@gmail.com";
                            }
                        }
                    }
                }

                WorldUps.UCommands.Builder uCommands = WorldUps.UCommands.newBuilder();
                /********************************RECEIVE ATruckLoadedNotification**************************************/
                //Send UGoDeliver
                for (UpsAmazon.ATruckLoadedNotification aTruckLoadedNotification : builder.getLoadedList()) {
                    //TODO: Avoid do same seqnum multiple times
                    auResponseBuilder.addAcks(aTruckLoadedNotification.getSeqnum());

//                    if (!aTruckLoadedNotificationASeqNumHashSet.contains(aTruckLoadedNotification.getSeqnum())) {
//                        aTruckLoadedNotificationASeqNumHashSet.add(aTruckLoadedNotification.getSeqnum());
                    if (!postgreSQLJDBC.isUGoDeliverASeqNumExist(aTruckLoadedNotification.getSeqnum())) {
                        Long truckID = aTruckLoadedNotification.getTruckId();
                        Integer truckStatus = new Status().tLoaded;
                        System.out.println("[ALR]     LoadedTruckID: " + aTruckLoadedNotification.getTruckId());
                        postgreSQLJDBC.updateTruckStatus(aTruckLoadedNotification.getTruckId(), null, null, truckStatus, null, true);
                        System.out.println("[ALR]     [ATruckLoadedNotification] ack: " + aTruckLoadedNotification.getSeqnum() + " truckID: " + aTruckLoadedNotification.getTruckId() + " truckStatus: " + postgreSQLJDBC.getTruckStatus(aTruckLoadedNotification.getTruckId()));


                        //---------------------------------------Send UGoDeliver-------------------------------------------/
                        Long wSeqNum = WSeqNumCounter.getInstance().getCurrSeqNum();
                        WorldUps.UGoDeliver.Builder UGoDeliver = WorldUps.UGoDeliver.newBuilder();
                        UGoDeliver.setTruckid(Math.toIntExact(aTruckLoadedNotification.getTruckId())).setSeqnum(wSeqNum);
                        for (Long packageID : postgreSQLJDBC.getShipmentPackageIDWithTruckID(truckID)) {
                            WorldUps.UDeliveryLocation.Builder uDeliveryLocation = WorldUps.UDeliveryLocation.newBuilder();
                            uDeliveryLocation.setX(Math.toIntExact(postgreSQLJDBC.getShipmentDestX(packageID))).setY(Math.toIntExact(postgreSQLJDBC.getShipmentDestY(packageID)));
                            uDeliveryLocation.setPackageid(packageID);
                            UGoDeliver.addPackages(uDeliveryLocation.build());
                        }
                        uCommands.addDeliveries(UGoDeliver.build());
                        //Add to Resend Database
                        postgreSQLJDBC.addUGoDeliver(wSeqNum, truckID, aTruckLoadedNotification.getSeqnum());
                        System.out.println("[ALR]    [UGoDeliver] : id: " + Math.toIntExact(truckID) + " wSeqNum: " + wSeqNum + " truckStatus: " + postgreSQLJDBC.getTruckStatus(truckID));
                        postgreSQLJDBC.updateTruckStatus(truckID, null, null, null, null, false);
                    }
                }
                amazonCommand.sendAUResponse(auResponseBuilder.build());
                worldCommand.sendUCommand(uCommands.build());
                //  amazonCommand.sendAUResponse(auResponseBuilder.build());
                postgreSQLJDBC.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
