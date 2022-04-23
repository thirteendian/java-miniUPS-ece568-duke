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

public class WorldListenerRunnable implements Runnable {
    Socket serverToWorldSocket;
    Socket serverToAmazonSocket;
    WorldResponse worldResponse;
    WorldCommand worldCommand;
    AmazonCommand amazonCommand;

    public WorldListenerRunnable(Socket serverToWorldSocket, Socket serverToAmazonSocket, WorldResponse worldResponse, WorldCommand worldCommand, AmazonCommand amazonCommand) {
        this.serverToWorldSocket = serverToWorldSocket;
        this.serverToAmazonSocket = serverToAmazonSocket;
        this.worldResponse = worldResponse;
        this.worldCommand = worldCommand;
        this.amazonCommand = amazonCommand;
    }

    @Override
    public void run() {
        //Connect To World
        try {
            worldCommand.sendUConnect(null, 10);
            worldResponse.recvUConnected();
            amazonCommand.sendUSendWorldID(worldResponse.getWorldid(), ASeqNumCounter.getInstance().getCurrSeqNum());
        } catch (IOException e) {
            e.printStackTrace();
        }

        while (true) {
            try {
                WorldUps.UResponses.Builder builder = worldResponse.recvUResponse();
                //UFinished Arrive Warehouse Or Finished All deliveries

                //Send to World builder
                WorldUps.UCommands.Builder uCommandsBuilder = WorldUps.UCommands.newBuilder();
                //Send to Amazon builder
                UpsAmazon.AUResponse.Builder auResponseBuilder = UpsAmazon.AUResponse.newBuilder();
                PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC();

                /**********************Receive Ack, Delete Resend***************************************************/
                for (Long ack : builder.getAcksList()) {


                    /**
                     * AShippingRequest[1] <= UGoPickUp[2] => UShippingResponse[3]
                     * If ACK is UGoPickUp
                     * Delete UGoPickUp Resend
                     * Send AShippingRequest ACK to Amazon
                     * Send UShppingResponse to Amazon
                     */
                    if(postgreSQLJDBC.isUGoPickupContainsWseq(ack)){
                        //----------------------------Send AShippingRequest[1] ACK to Amazon---------------------------/
                        //From ack(WSeqNum of UGoPickUp) get ASeqNum(corresponding AShippingRequest ACK)
                        Long AShippingRequestSeqNum = postgreSQLJDBC.getUGOPickupASeqNum(ack);
                        //add Repeated AShippingRequestSeqNum to AUResponse
                        auResponseBuilder.addAcks(AShippingRequestSeqNum);

                        //----------------------------Send UShippingResponse to Amazon---------------------------------/
                        Long truckID = postgreSQLJDBC.getUGoDeliverTruckID(ack);
                        UpsAmazon.UShippingResponse.Builder uShippingResponseBuilder = UpsAmazon.UShippingResponse.newBuilder();
                        ArrayList<Long> packageIDArrayList = postgreSQLJDBC.getShipmentPackageIDWithTruckID(truckID);
                        //add Repeated UTracking to UshippingResponse
                        for(Long packageID: packageIDArrayList){
                            UpsAmazon.UTracking.Builder uTrackingBuilder = UpsAmazon.UTracking.newBuilder().setPackageId(packageID).setTrackingNumber("123");
                            uShippingResponseBuilder.addUTracking(uTrackingBuilder.build());
                        }
                        //create UShippingResponse
                        Long ASeqNum = ASeqNumCounter.getInstance().getCurrSeqNum();
                        uShippingResponseBuilder.setSeqnum(ASeqNum);
                        auResponseBuilder.addShippingResponse(uShippingResponseBuilder.build());
                        //-----------------------------Add UShippingResponse to Resend Database------------------------/
                        postgreSQLJDBC.addUShippingResponse(ASeqNum,truckID);
                        //----------------------------Update Truck Status----------------------------------------------/
                        postgreSQLJDBC.updateTruckStatus(truckID,null,null ,new Status().tTraveling,false);
                    }
                    /**
                     * Delete All ACK Resend PostgreSQL
                     */
                    postgreSQLJDBC.deleteUGoDeliver(ack);
                    postgreSQLJDBC.deleteUGoPickUp(ack);

                }

                /**********************************Update Truck Status***************************************************/
                for(WorldUps.UTruck uTruck : builder.getTruckstatusList()){
                    //-----------------------------Delete Resend Quary Table-------------------------------------------/
                    postgreSQLJDBC.deleteQuery(uTruck.getSeqnum());
                    //-----------------------------Change Truck Status-------------------------------------------/
                    Integer truckStatus = new Status().getStatus(uTruck.getStatus());
                    postgreSQLJDBC.updateTruckStatus((long) uTruck.getTruckid(),uTruck.getX(),uTruck.getY(),truckStatus,false);
                }

                UpsAmazon.UShipmentStatusUpdate.Builder shipmentStatusUpdate = UpsAmazon.UShipmentStatusUpdate.newBuilder();
                Long auShipmentStatusUpdateSeqNum = ASeqNumCounter.getInstance().getCurrSeqNum();
                /**********************************Receive One Package UDelivery Made **********************************/
                for(WorldUps.UDeliveryMade uDeliveryMade: builder.getDeliveredList()){
                    //-----------------------------Send ACK(Add ack to UCommands)--------------------------------------/
                    uCommandsBuilder.addAcks(uDeliveryMade.getSeqnum());
                    //-----------------------------Update Package Status to Delivered----------------------------------/
                    postgreSQLJDBC.updateShipmentStatus(uDeliveryMade.getPackageid(),new Status().pDelivered);
                    //-----------------------------Send UShipment Status Update----------------------------------------/
                    UpsAmazon.AUShipmentUpdate auShipmentUpdate = UpsAmazon.AUShipmentUpdate.newBuilder().setPackageId(uDeliveryMade.getPackageid()).setStatus("delivered").build();
                    shipmentStatusUpdate.addAuShipmentUpdate(auShipmentUpdate);
                    //-----------------------------Put AUshipmentUpdate, ShipmentStatusUpdate to Resend List------------/
                    postgreSQLJDBC.addAUShipmentStatusUpdate(uDeliveryMade.getPackageid(),new Status().pDelivered,auShipmentStatusUpdateSeqNum);
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
                    if ((status == new Status().tTraveling) && (new Status().getStatus(finished.getStatus()) == new Status().tArriveWarehouse)) {
                        postgreSQLJDBC.updateTruckStatus((long) finished.getTruckid(), finished.getX(), finished.getY(), new Status().getStatus(finished.getStatus()), false);
                        //Send to Amazon UTruckArrivedNotification
                        amazonCommand.sendUTruckArrivedNotification((long) finished.getTruckid(), aSeqNum);
                        //Add UTruckArrivedNotification to Resend
                        postgreSQLJDBC.addUTruckArrivedNotification(aSeqNum, (long) finished.getTruckid());
                    }
                    //if Complete Delivering, last status delivering, curr status idle
                    else if ((status == new Status().tDelivering) && (new Status().getStatus(finished.getStatus()) == new Status().tIdel)) {
                        postgreSQLJDBC.updateTruckStatus((long) finished.getTruckid(), finished.getX(), finished.getY(), new Status().getStatus(finished.getStatus()), false);
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
