package edu.duke.ece568.server.World;

import edu.duke.ece568.server.Amazon.ASeqNumCounter;
import edu.duke.ece568.server.Amazon.AmazonCommand;
import edu.duke.ece568.server.PostgreSQLJDBC;
import edu.duke.ece568.server.protocol.UpsAmazon;
import edu.duke.ece568.server.protocol.WorldUps;
import edu.duke.ece568.shared.Status;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.UUID;

public class WorldListenerRunnable implements Runnable {
    private Socket serverToWorldSocket;
    private Socket serverToAmazonSocket;
    private WorldResponse worldResponse;
    private WorldCommand worldCommand;
    private AmazonCommand amazonCommand;
    private Boolean debugMode;

    public WorldListenerRunnable(Socket serverToWorldSocket, Socket serverToAmazonSocket, WorldResponse worldResponse, WorldCommand worldCommand, AmazonCommand amazonCommand, Boolean debugMode) {
        this.serverToWorldSocket = serverToWorldSocket;
        this.serverToAmazonSocket = serverToAmazonSocket;
        this.worldResponse = worldResponse;
        this.worldCommand = worldCommand;
        this.amazonCommand = amazonCommand;
        this.debugMode = debugMode;
    }

    @Override
    public void run() {
        if (!debugMode) {
            //Connect To World
            try {
                worldCommand.sendUConnect(null, 10);
                worldResponse.recvUConnected();
                System.out.println("UPS: {result: " + worldResponse.getResult() + " worldID: " + worldResponse.getWorldid() + "}");
                amazonCommand.sendUSendWorldID(worldResponse.getWorldid(), ASeqNumCounter.getInstance().getCurrSeqNum());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("WorldListenerRunnable start !");
        while (true) {
            try {
                WorldUps.UResponses.Builder builder = worldResponse.recvUResponse();
                //UFinished Arrive Warehouse Or Finished All deliveries

                //Send to World builder
                WorldUps.UCommands.Builder uCommandsBuilder = WorldUps.UCommands.newBuilder();
                //Send to Amazon builder
                UpsAmazon.AUResponse.Builder auResponseBuilder = UpsAmazon.AUResponse.newBuilder();
                PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC();

                /************************************** RECEIVE ACK ***************************************************/
                for (Long ack : builder.getAcksList()) {
                    /**
                     * AShippingRequest[1] <= UGoPickUp[2] => UShippingResponse[3]
                     * If ACK is UGoPickUp
                     * Delete UGoPickUp Resend
                     * Send AShippingRequest ACK to Amazon
                     * Send UShppingResponse to Amazon
                     */
                    if (postgreSQLJDBC.isUGoPickupContainsWseq(ack)) {
                        System.out.println("[WLR]     RECEIVE UGoPickUp ACK : " + ack);
                        //----------------------------Send AShippingRequest[1] ACK to Amazon---------------------------/
                        //From ack(WSeqNum of UGoPickUp) get ASeqNum(corresponding AShippingRequest ACK)
                        Long AShippingRequestSeqNum = postgreSQLJDBC.getUGOPickupASeqNum(ack);
                        //add Repeated AShippingRequestSeqNum to AUResponse
                        auResponseBuilder.addAcks(AShippingRequestSeqNum);
                        System.out.println("[WLR]      [send AShippingRequest ACK]: " + AShippingRequestSeqNum);
                        //----------------------------Send UShippingResponse to Amazon---------------------------------/
                        Long truckID = postgreSQLJDBC.getUGOPickupTruckID(ack);

                        UpsAmazon.UShippingResponse.Builder uShippingResponseBuilder = UpsAmazon.UShippingResponse.newBuilder();
                        ArrayList<Long> packageIDArrayList = postgreSQLJDBC.getShipmentPackageIDWithTruckID(truckID);
                        //add Repeated UTracking to UshippingResponse
                        for (Long packageID : packageIDArrayList) {
                            UpsAmazon.UTracking.Builder uTrackingBuilder = UpsAmazon.UTracking.newBuilder().setPackageId(packageID).setTrackingNumber(postgreSQLJDBC.getShipmentTrackingNum(packageID));
                            uShippingResponseBuilder.addUTracking(uTrackingBuilder.build());
                            System.out.println("[WLR]      [uTracking]: packageID : " + packageID + " TrackingNumber: " + postgreSQLJDBC.getShipmentTrackingNum(packageID));
                        }
                        //create UShippingResponse
                        Long ASeqNum = ASeqNumCounter.getInstance().getCurrSeqNum();
                        uShippingResponseBuilder.setSeqnum(ASeqNum).setTruckId(truckID);
                        auResponseBuilder.addShippingResponse(uShippingResponseBuilder.build());
                        System.out.println("[WLR]      [UShippingResponse]: seq: " + ASeqNum);
                        //-----------------------------Add UShippingResponse to Resend Database------------------------/
                        postgreSQLJDBC.addUShippingResponse(ASeqNum, truckID);
                        System.out.println("[WLR]      [UShippingResponse]: Add to Resend");
                        //----------------------------Update Truck Status----------------------------------------------/
                        Integer truckStatus = new Status().tTraveling;
                        postgreSQLJDBC.updateTruckStatus(truckID, null, null, truckStatus, null,null);
                        System.out.println("[WLR]      [Truck Status Update]: Traveling, statusNum " + truckStatus);
                        //----------------------------Delete All UGoPickUp---------------------------------------------/
                        postgreSQLJDBC.deleteUGoPickUp(ack);
                    }


                    /**
                     * UGoDeliver
                     * If ACK is UGoDeliver
                     * Delete UGoDeliver Resend
                     * Send UShippingResponse to Amazon
                     */
                    if (postgreSQLJDBC.isUGoDeliverContainsWseq(ack)) {
                        System.out.println("[WLR]     RECEIVE UGoDeliver ACK : " + ack);
                        //----------------------------Change Truck Status to Delivering--------------------------------/
                        Long truckID = postgreSQLJDBC.getUGoDeliverTruckID(ack);
                        postgreSQLJDBC.updateTruckStatus(truckID,null,null,new Status().tDelivering,null,true);

                        UpsAmazon.UShippingResponse.Builder uShippingResponse = UpsAmazon.UShippingResponse.newBuilder();
                        //----------------------------Change Related Package(Shipment) Status to Delivering------------/
                        //----------------------------Send UShippingResponse to Amazon---------------------------------/
                        ArrayList<Long> packageIDArrayList = postgreSQLJDBC.getShipmentPackageIDWithTruckID(truckID);
                        for (Long packageID : packageIDArrayList) {
                            UpsAmazon.UTracking.Builder uTracking = UpsAmazon.UTracking.newBuilder();
                            postgreSQLJDBC.updateShipmentStatus(packageID,new Status().pDelivering);
                            System.out.println("[WLR]      [uGoDeliver ACK]: packageID : " + packageID + " Status: " + postgreSQLJDBC.getShipmentStatus(packageID));
                            uTracking.setPackageId(packageID).setTrackingNumber(String.valueOf(packageID));//TODO: ChangeBack postgreSQLJDBC.getShipmentTrackingNum(packageID));
                            uShippingResponse.addUTracking(uTracking.build());
                        }
                        Long UShippingResponseSeqNum = ASeqNumCounter.getInstance().getCurrSeqNum();
                        uShippingResponse.setSeqnum(UShippingResponseSeqNum).setTruckId(truckID);
                        auResponseBuilder.addShippingResponse(uShippingResponse.build());
                        System.out.println("[WLR]      [UShippingResponse]: truckID : " + truckID + " ASeqNum: "+ UShippingResponseSeqNum);
                        //----------------------------Delete All UGoDeliver--------------------------------------------/
                        postgreSQLJDBC.deleteUGoDeliver(ack);
                    }
//                    System.out.println("[WLR]    [Delete UGoDeliver]: ACK: " + ack);
                }

                /**********************************Update Truck Status***************************************************/
                for (WorldUps.UTruck uTruck : builder.getTruckstatusList()) {
                    //-----------------------------Delete Resend Quary Table-------------------------------------------/
                    postgreSQLJDBC.deleteQuery(uTruck.getSeqnum());
                    //-----------------------------Change Truck Status-------------------------------------------/
                    Integer truckStatus = new Status().getStatus(uTruck.getStatus());
                    postgreSQLJDBC.updateTruckStatus((long) uTruck.getTruckid(), uTruck.getX(), uTruck.getY(), null, null,null);
//                    System.out.println("[WLR]    SQLstatus: " + postgreSQLJDBC.getTruckStatus((long) uTruck.getTruckid()) + " wSeqNum: " + uTruck.getSeqnum());
                    if (truckStatus !=new Status().tLoaded) {
                        postgreSQLJDBC.updateTruckStatus((long) uTruck.getTruckid(), null, null, truckStatus, null,null);
                    }

//                    System.out.println("[WLR]    TruckID: "+ uTruck.getTruckid() +" RCVstatus: "+ uTruck.getStatus() + " SQLstatus: " + postgreSQLJDBC.getTruckStatus((long) uTruck.getTruckid()) + " wSeqNum: " + uTruck.getSeqnum());
                    //-----------------------------------------------Add ACK-------------------------------------------/
                    uCommandsBuilder.addAcks(uTruck.getSeqnum());
                }

                UpsAmazon.UShipmentStatusUpdate.Builder shipmentStatusUpdate = UpsAmazon.UShipmentStatusUpdate.newBuilder();
                Long auShipmentStatusUpdateSeqNum = ASeqNumCounter.getInstance().getCurrSeqNum();
                /**********************************Receive One Package UDelivery Made **********************************/
                for (WorldUps.UDeliveryMade uDeliveryMade : builder.getDeliveredList()) {
                    //-----------------------------Send ACK(Add ack to UCommands)--------------------------------------/
                    uCommandsBuilder.addAcks(uDeliveryMade.getSeqnum());
                    System.out.println("[WLR]     [UCommands ACK]: seq: " + uDeliveryMade.getSeqnum());
                    //-----------------------------Update Package Status to Delivered----------------------------------/
                    postgreSQLJDBC.updateShipmentStatus(uDeliveryMade.getPackageid(), new Status().pDelivered);
                    System.out.println("[WLR]     [Truck Status Update]: PackageID: " + uDeliveryMade.getPackageid() + "Delivered");
                    //-----------------------------Send UShipment Status Update----------------------------------------/
                    UpsAmazon.AUShipmentUpdate auShipmentUpdate = UpsAmazon.AUShipmentUpdate.newBuilder().setPackageId(uDeliveryMade.getPackageid()).setStatus("delivered").build();
                    shipmentStatusUpdate.addAuShipmentUpdate(auShipmentUpdate);
                    System.out.println("[WLR]     [UShipmentStatusUpdate]: seq: " + auShipmentStatusUpdateSeqNum);
                    //-----------------------------Put AUshipmentUpdate, ShipmentStatusUpdate to Resend List------------/
                    postgreSQLJDBC.addAUShipmentUpdate(uDeliveryMade.getPackageid(), new Status().pDelivered, auShipmentStatusUpdateSeqNum);
                    System.out.println("[WLR]     [AUShipmentStatusUpdate Resend]: PackageID: " + uDeliveryMade.getPackageid() + "Delivered");
                }
                shipmentStatusUpdate.setSeqnum(auShipmentStatusUpdateSeqNum).build();
                auResponseBuilder.addShipmentStatusUpdate(shipmentStatusUpdate);


                //UGoPickUp seq ==> AShippingRequest seq
                /*********COMPLETE UGOPICKUP OR COMPLETE DELIVERING ALL PACKAGES***************************************/
                for (WorldUps.UFinished finished : builder.getCompletionsList()) {
                    //Send ACK to World
                    uCommandsBuilder.addAcks(finished.getSeqnum());
                    //Update Truck
                    Long aSeqNum = ASeqNumCounter.getInstance().getCurrSeqNum();
                    //if Complete UGoPickUp, last status traveling, curr status arrive warehouse
                    Integer status = postgreSQLJDBC.getTruckStatus((long) finished.getTruckid());
                    System.out.println("[WLR]    TruckID: " + finished.getTruckid() + " SQLStatus: " + status + " WorldTruckStatus: " + finished.getStatus());
                    if (((status == new Status().tTraveling) || status == new Status().tArriveWarehouse) && (new Status().getStatus(finished.getStatus()) == new Status().tArriveWarehouse)) {
                        postgreSQLJDBC.updateTruckStatus((long) finished.getTruckid(), finished.getX(), finished.getY(), new Status().getStatus(finished.getStatus()), null,null);
                        //Send to Amazon UTruckArrivedNotification
                        amazonCommand.sendUTruckArrivedNotification((long) finished.getTruckid(), aSeqNum);
                        //Add UTruckArrivedNotification to Resend
                        postgreSQLJDBC.addUTruckArrivedNotification(aSeqNum, (long) finished.getTruckid());
                        System.out.println("[Complete]: UGoPickUp: seq: " + finished.getSeqnum() + " TruckID " + finished.getTruckid());
                    }
                    //if Complete Delivering, last status delivering, curr status idle, isSentUGoPickUp is false
                    else if (((status == new Status().tDelivering) || (status == new Status().tIdel)) && (new Status().getStatus(finished.getStatus()) == new Status().tIdel)) {
                        postgreSQLJDBC.updateTruckStatus((long) finished.getTruckid(), finished.getX(), finished.getY(), new Status().getStatus(finished.getStatus()), false,false);
                        System.out.println("[Complete]: All Delivering: seq:" + finished.getSeqnum() + " TruckID " + finished.getTruckid());
                    } else {
                        System.out.println("ERROR: Last Truck Status was not properly set.");
                    }

                }

                amazonCommand.sendAUResponse(auResponseBuilder.build());
                worldCommand.sendUCommand(uCommandsBuilder.build());
                postgreSQLJDBC.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
