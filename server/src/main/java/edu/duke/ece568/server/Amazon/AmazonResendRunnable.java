package edu.duke.ece568.server.Amazon;

import edu.duke.ece568.server.PostgreSQLJDBC;
import edu.duke.ece568.server.protocol.UpsAmazon;
import edu.duke.ece568.shared.Status;

import java.net.Socket;
import java.util.ArrayList;
import java.util.UUID;

public class AmazonResendRunnable implements Runnable {
    private Socket socketToAmazon;
    private AmazonCommand amazonCommand;

    public AmazonResendRunnable(Socket socketToAmazon, AmazonCommand amazonCommand) {
        this.socketToAmazon = socketToAmazon;
        this.amazonCommand = amazonCommand;
    }

    @Override
    public void run() {
        //3 Second Step
        System.out.println("AmazonResendRunnable Start!");
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        UpsAmazon.AUResponse.Builder auResponse = UpsAmazon.AUResponse.newBuilder();
        PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC();
        /********************************RESEND ALL UShippingResponse**************************************/
        ArrayList<Long> UShippingResponseASeqNums = postgreSQLJDBC.getAllUShippingResponse();
        for (Long aSeqNum : UShippingResponseASeqNums) {
            Long truckID = postgreSQLJDBC.getUShippingResponseTruckID(aSeqNum);
            UpsAmazon.UShippingResponse.Builder uShippingResponseBuilder = UpsAmazon.UShippingResponse.newBuilder();
            ArrayList<Long> packageIDArrayList = postgreSQLJDBC.getShipmentPackageIDWithTruckID(truckID);
            for (Long packageID : packageIDArrayList) {
                UpsAmazon.UTracking.Builder uTrackingBuilder = UpsAmazon.UTracking.newBuilder().setPackageId(packageID).setTrackingNumber(packageID.toString());
                uShippingResponseBuilder.addUTracking(uTrackingBuilder.build());
//                System.out.println("        [uTracking]: packageID : "+ packageID);
            }
            //create UShippingResponse
            Long ASeqNum = ASeqNumCounter.getInstance().getCurrSeqNum();
            uShippingResponseBuilder.setSeqnum(ASeqNum);
            auResponse.addShippingResponse(uShippingResponseBuilder.build());
            System.out.println("[ARESEND] UShippingResponse: seq: " + ASeqNum);
        }
        /********************************RESEND ALL uTruckArrivedNotification**************************************/
        ArrayList<Long> UTruckArrivedNotificaionSeqNums = postgreSQLJDBC.getAllUTruckArrivedNotification();
        for (Long aSeqNum : UTruckArrivedNotificaionSeqNums) {
            UpsAmazon.UTruckArrivedNotification.Builder uTruckArrivedNotification = UpsAmazon.UTruckArrivedNotification.newBuilder();
            uTruckArrivedNotification.setTruckId(postgreSQLJDBC.getUTruckArrivedNotificationTruckID(aSeqNum));
            uTruckArrivedNotification.setSeqnum(aSeqNum);
            auResponse.addArrived(uTruckArrivedNotification);
            System.out.println("[ARESEND] TruckArrivedNotification: seq: " + aSeqNum + "truckID: " + postgreSQLJDBC.getUTruckArrivedNotificationTruckID(aSeqNum));
        }
        /********************************RESEND UShipmentStatusUpdate**************************************/
        ArrayList<Long> uShipmentStatusUpdateASeqNums = postgreSQLJDBC.getAllUShipmentStatusUpdate();
        for (Long aSeqNum : uShipmentStatusUpdateASeqNums) {
            UpsAmazon.UShipmentStatusUpdate.Builder uShipmentStatusUpdate = UpsAmazon.UShipmentStatusUpdate.newBuilder();
            ArrayList<Long> auShipmentUpdatePackageIds = postgreSQLJDBC.getAllAUShipmentUpdate();

            for (Long packageID : auShipmentUpdatePackageIds) {
                UpsAmazon.AUShipmentUpdate.Builder auShipmentUpdate = UpsAmazon.AUShipmentUpdate.newBuilder();
                auShipmentUpdate.setPackageId(packageID).setStatus(new Status().getStatus(postgreSQLJDBC.getAUShipmentUpdateStatus(packageID)));
                uShipmentStatusUpdate.addAuShipmentUpdate(auShipmentUpdate.build());
            }
            auResponse.addShipmentStatusUpdate(uShipmentStatusUpdate);
            System.out.println("[ARESEND] UShipmentStatusUpdate: seq: " + aSeqNum);

        }
        amazonCommand.sendAUResponse(auResponse.build());
        postgreSQLJDBC.close();
    }
}
