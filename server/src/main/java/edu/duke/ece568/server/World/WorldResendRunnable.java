package edu.duke.ece568.server.World;

import edu.duke.ece568.server.PostgreSQLJDBC;
import edu.duke.ece568.server.protocol.WorldUps;

import java.net.Socket;
import java.util.ArrayList;

public class WorldResendRunnable implements Runnable{
    Socket serverToWorldSocket;
    WorldCommand worldCommand;

    public WorldResendRunnable(Socket serverToWorldSocket, WorldCommand worldCommand) {
        this.serverToWorldSocket = serverToWorldSocket;
        this.worldCommand = worldCommand;
    }

    @Override
    public void run() {
        //3 Second Step
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        WorldUps.UCommands.Builder uCommands = WorldUps.UCommands.newBuilder();
        PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC();

        /********************************RESEND ALL UGOPICKUP**************************************/
        ArrayList<Long> uGoPickUpWSeqNums = postgreSQLJDBC.getUGoPickUpWSeqNums();
        for(Long wSeqNum: uGoPickUpWSeqNums){
            WorldUps.UGoPickup.Builder uGoPickupBuilder = WorldUps.UGoPickup.newBuilder();
            Integer truckID = Math.toIntExact(postgreSQLJDBC.getUGOPickupTruckID(wSeqNum));
            uGoPickupBuilder.setSeqnum(wSeqNum).setTruckid(truckID).setWhid(Math.toIntExact(postgreSQLJDBC.getUGoPickupWareHouseID(wSeqNum)));
            uCommands.addPickups(uGoPickupBuilder.build());
            System.out.println("[WRESEND]: UGOPICKUP seq:" + wSeqNum + "truckID: " + truckID);
        }
        /********************************RESEND ALL UGODeliver**************************************/
        ArrayList<Long> uGoDeliverWSeqNums = postgreSQLJDBC.getUGoDeliverWSeqNums();
        for(Long wSeqNum: uGoDeliverWSeqNums){
            Long truckID = postgreSQLJDBC.getUGoDeliverTruckID(wSeqNum);
            WorldUps.UGoDeliver.Builder uGoDeliverBuilder = WorldUps.UGoDeliver.newBuilder();
            uGoDeliverBuilder.setTruckid(Math.toIntExact(truckID)).setSeqnum(wSeqNum);

            ArrayList<Long> packageID = postgreSQLJDBC.getShipmentPackageIDWithTruckID(truckID);
            //add all packages
            for(Long id: packageID){
                WorldUps.UDeliveryLocation.Builder uDeliveryLocation = WorldUps.UDeliveryLocation.newBuilder();
                uDeliveryLocation.setX(Math.toIntExact(postgreSQLJDBC.getShipmentDestX(id))).setY(Math.toIntExact(postgreSQLJDBC.getShipmentDestY(id)));
                uDeliveryLocation.setPackageid(id);
                uGoDeliverBuilder.addPackages(uDeliveryLocation.build());
            }
            uCommands.addDeliveries(uGoDeliverBuilder.build());
            System.out.println("[WRESEND]: UGoDeliver seq: " + wSeqNum + "truckid: " +truckID);
        }

        worldCommand.sendUCommand(uCommands.build());
        postgreSQLJDBC.close();

    }
}
