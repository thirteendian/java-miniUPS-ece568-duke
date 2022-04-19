package edu.duke.ece568.server.Amazon;

import edu.duke.ece568.server.PostgreSQLJDBC;
import edu.duke.ece568.server.World.WorldCommand;
import edu.duke.ece568.server.protocol.UpsAmazon;
import edu.duke.ece568.shared.Status;

import java.io.IOException;
import java.net.Socket;

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
        while(true){
            try {
                UpsAmazon.AURequest.Builder builder = amazonResponse.recvFromAmazon();
                /**********************************DELETE ALL RESEND THAT ACKED****************************************/
                for (Long ack : builder.getAcksList()) {
                    PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC();
                    postgreSQLJDBC.deleteUShippingResponse(ack);
                    postgreSQLJDBC.deleteUShipmentStatusUpdate(ack);
                    postgreSQLJDBC.deleteUTruckArrivedNotification(ack);
                    postgreSQLJDBC.close();
                }
                /********************************RECEIVE ASHIPPING REQUEST*********************************************/
                for(UpsAmazon.AShippingRequest aShippingRequest: builder.getShippingRequestList()){
                    PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC();
                    //Add ShippingRequest
                    Long shippingID = AShippingNumCounter.getInstance().getCurrSeqNum();
                    postgreSQLJDBC.addShippingRequest(shippingID,aShippingRequest.getSeqnum());
                    //Add All Shipment Table
                    for(UpsAmazon.AShipment aShipment: aShippingRequest.getShipmentList()){
                        //Generate Warehouse Table
                        postgreSQLJDBC.addWarehouse((long) aShippingRequest.getLocation().getWarehouseid(),aShippingRequest.getLocation().getX(),aShippingRequest.getLocation().getY());
                        //Generate AShipment Table
                        postgreSQLJDBC.addShipment(aShipment.getPackageId(),aShipment.getDestX(),aShipment.getDestY(), (long) aShippingRequest.getLocation().getWarehouseid(),aShipment.getEmailaddress(),new Status().pInWarehouse,shippingID);
                    }
                    postgreSQLJDBC.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
