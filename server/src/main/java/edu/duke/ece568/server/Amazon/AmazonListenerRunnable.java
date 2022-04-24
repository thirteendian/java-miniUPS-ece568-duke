package edu.duke.ece568.server.Amazon;

import edu.duke.ece568.server.PostgreSQLJDBC;
import edu.duke.ece568.server.World.WSeqNumCounter;
import edu.duke.ece568.server.World.WorldCommand;
import edu.duke.ece568.server.protocol.UpsAmazon;
import edu.duke.ece568.server.protocol.WorldUps;
import edu.duke.ece568.shared.Status;

import java.io.IOException;
import java.net.Socket;
import java.util.UUID;

public class AmazonListenerRunnable implements Runnable{
    Socket serverToWorldSocket;
    Socket serverToAmazonSocket;
    AmazonCommand amazonCommand;
    WorldCommand worldCommand;
    AmazonResponse amazonResponse;
    public AmazonListenerRunnable(Socket serverToWorldSocket, Socket serverToAmazonSocket,AmazonResponse amazonResponse, AmazonCommand amazonCommand, WorldCommand worldCommand) {
        this.serverToWorldSocket = serverToWorldSocket;
        this.serverToAmazonSocket = serverToAmazonSocket;
        this.amazonCommand = amazonCommand;
        this.worldCommand = worldCommand;
        this.amazonResponse = amazonResponse;
    }

    @Override
    public void run() {
        System.out.println("AmazonListenerRunnable start !");
        while(true){
            try {
                UpsAmazon.AURequest.Builder builder = amazonResponse.recvFromAmazon();
                UpsAmazon.AUResponse.Builder auResponseBuilder = UpsAmazon.AUResponse.newBuilder();
                PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC();

                /**********************************DELETE ALL RESEND THAT ACKED****************************************/
                for (Long ack : builder.getAcksList()) {
                    postgreSQLJDBC.deleteUShippingResponse(ack);
                    postgreSQLJDBC.deleteUShipmentStatusUpdate(ack);
                    postgreSQLJDBC.deleteUTruckArrivedNotification(ack);
                    System.out.println("[Delete Amazon Resend]: ack: "+ ack);
                }

                /********************************RECEIVE ASHIPPING REQUEST*********************************************/
                for(UpsAmazon.AShippingRequest aShippingRequest: builder.getShippingRequestList()){
                    //AShippingRequest ACK
                    auResponseBuilder.addAcks(aShippingRequest.getSeqnum());
                    //Add ShippingRequest
                    Long shippingID = AShippingNumCounter.getInstance().getCurrSeqNum();//ASC
                    postgreSQLJDBC.addShippingRequest(shippingID,aShippingRequest.getSeqnum());
                    //Add All Shipment Table
                    for(UpsAmazon.AShipment aShipment: aShippingRequest.getShipmentList()){
                        //Add Warehouse Table
                        postgreSQLJDBC.addWarehouse((long) aShippingRequest.getLocation().getWarehouseid(),aShippingRequest.getLocation().getX(),aShippingRequest.getLocation().getY());
                        //Add AShipment Table
                        UUID uuid = UUID.randomUUID();
                        postgreSQLJDBC.addShipment(aShipment.getPackageId(),aShipment.getDestX(),aShipment.getDestY(), (long) aShippingRequest.getLocation().getWarehouseid(),aShipment.getEmailaddress(),new Status().pInWarehouse,shippingID,uuid.toString());
                        System.out.println("[ALR]      [AShippingRequest]: warehouse{ id: " + aShippingRequest.getLocation().getWarehouseid()+ "x: " + aShippingRequest.getLocation().getX() + "y: "+ aShippingRequest.getLocation().getY()+"}"
                                            +" Ashipment{packageID: "+ aShipment.getPackageId() + "DestX: "+aShipment.getDestX() +"DestY: "+aShipment.getDestY() +"ShippingID: "+ shippingID+ " TrackingNum: "+uuid.toString()+"}");
                        //Add Package Table
                        for(UpsAmazon.Product product: aShipment.getProductList()){
                            postgreSQLJDBC.addProduct(aShipment.getPackageId(),product.getDescription(), (long) product.getCount());
                            System.out.println("    [Product]: description: "+ product.getDescription() +" count: "+ product.getCount());
                        }
                    }
                }
                WorldUps.UCommands.Builder uCommands = WorldUps.UCommands.newBuilder();
                /********************************RECEIVE ATruckLoadedNotification**************************************/
                //Send UGoDeliver
                for(UpsAmazon.ATruckLoadedNotification aTruckLoadedNotification: builder.getLoadedList()){
                    auResponseBuilder.addAcks(aTruckLoadedNotification.getSeqnum());
                    Long truckID = aTruckLoadedNotification.getTruckId();
                    Integer truckStatus = new Status().tLoaded;
                    System.out.println("[ALR]     LoadedTruckID: " + aTruckLoadedNotification.getTruckId());
                    postgreSQLJDBC.updateTruckStatus(aTruckLoadedNotification.getTruckId(),null,null, truckStatus,null,true);
                    System.out.println("[ALR]     [ATruckLoadedNotification] ack: "+ aTruckLoadedNotification.getSeqnum() +" truckID: "+ aTruckLoadedNotification.getTruckId() + " truckStatus: " + postgreSQLJDBC.getTruckStatus(aTruckLoadedNotification.getTruckId()));

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
                    postgreSQLJDBC.addUGoDeliver(wSeqNum, truckID);
                    System.out.println("[ALR]    [UGoDeliver] : id: " + Math.toIntExact(truckID) + " wSeqNum: " + wSeqNum + " truckStatus: " + postgreSQLJDBC.getTruckStatus(truckID));
                    postgreSQLJDBC.updateTruckStatus(truckID,null,null,null,null,false);
                }
                worldCommand.sendUCommand(uCommands.build());
                amazonCommand.sendAUResponse(auResponseBuilder.build());
                postgreSQLJDBC.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
