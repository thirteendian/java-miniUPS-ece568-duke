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


                /**********************************DELETE ALL RESEND***************************************************/
                for (Long ack : builder.getAcksList()) {
                    PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC();
                    //From ack(WSeqNum of UGoPickUp) get ASeqNum(corresponding AShippingRequest ACK)
                    if(postgreSQLJDBC.isUGoPickupContainsWseq(ack)){
                        //Send AShippingRequest ACK to Amazon
                        Long AShippingRequestSeqNum = postgreSQLJDBC.getUGOPickupASeqNum(ack);
                        auResponseBuilder.addAcks(AShippingRequestSeqNum);
                        //Send UShippingResponse to Amazon
                        Long truckID = postgreSQLJDBC.getUGoDeliverTruckID(ack);
                        UpsAmazon.UShippingResponse.Builder uShippingResponseBuilder = UpsAmazon.UShippingResponse.newBuilder();
                        ArrayList<Long> packageIDArrayList = postgreSQLJDBC.getShipmentPackageIDWithTruckID(truckID);
                            //add Repeated UTracking to UshippingResponse
                        for(Long packageID: packageIDArrayList){
                            UpsAmazon.UTracking.Builder uTrackingBuilder = UpsAmazon.UTracking.newBuilder().setPackageId(packageID).setTrackingNumber("123");
                            uShippingResponseBuilder.addUTracking(uTrackingBuilder.build());
                        }
                        Long ASeqNum = ASeqNumCounter.getInstance().getCurrSeqNum();
                        uShippingResponseBuilder.setSeqnum(ASeqNum);
                        auResponseBuilder.addShippingResponse(uShippingResponseBuilder.build());
                        //Add UShippingResponse to Resend Database
                        postgreSQLJDBC.addUShippingResponse(ASeqNum,truckID);
                        //Update Truck Status to Traveling
                        postgreSQLJDBC.updateTruckStatus(truckID,null,null ,new Status().tTraveling,false);
                    }
                    postgreSQLJDBC.deleteUGoDeliver(ack);
                    postgreSQLJDBC.deleteUGoPickUp(ack);
                }


                //UGoPickUp seq ==> AShippingRequest seq
                /*********COMPLETE UGOPICKUP OR COMPLETE DELIVERING ALL PACKAGES***************************************/
                for (WorldUps.UFinished finished : builder.getCompletionsList()) {
                    //Send ACK to World
                    uCommandsBuilder.addAcks(finished.getSeqnum());
                    //Update Truck
                    PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC();
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

                PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC();
                amazonCommand.sendAUResponse(auResponseBuilder.build());
                worldCommand.sendUCommand(uCommandsBuilder.build());
                postgreSQLJDBC.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
