package edu.duke.ece568.server.World;

import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.Message;
import edu.duke.ece568.server.PostgreSQLJDBC;
import edu.duke.ece568.server.protocol.WorldUps;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class WorldCommand {
    private Socket serverToWorldSocket;

    public WorldCommand(Socket serverToWorldSocket) {
        this.serverToWorldSocket = serverToWorldSocket;
    }


    private synchronized void sendToWorld(Message msg) throws IOException {
        OutputStream outputStream = this.serverToWorldSocket.getOutputStream();
        CodedOutputStream codedOutputStream = CodedOutputStream.newInstance(outputStream);
        codedOutputStream.writeUInt32NoTag(msg.toByteArray().length);
        msg.writeTo(codedOutputStream);
        codedOutputStream.flush();
    }

    public void sendUConnect(Long worldid, Integer numOfTruck) throws IOException {
        WorldUps.UConnect.Builder builder = WorldUps.UConnect.newBuilder();
        builder.setIsAmazon(false);
        //Set WorldID, if exist
        if (worldid != null) {
            builder.setWorldid(worldid);
        } else {
            //If WorldID is Null, create New world, create number of Trucks
            //Generate Num of Truck
            PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC();
            for (int i = 0; i < numOfTruck; i++) {
                WorldUps.UInitTruck.Builder uInitTruck = WorldUps.UInitTruck.newBuilder();
                uInitTruck.setX(0).setY(0).setId(i + 1);
                uInitTruck.build();
                builder.addTrucks(uInitTruck);
                //TODO: Update Truck status only if ack is received
//                System.out.print("Create Truck ID = ");
//                System.out.println(i+1);
                postgreSQLJDBC.addTruck((long) (i+1),0,0,1,false);
            }
            postgreSQLJDBC.close();
        }

        sendToWorld(builder.build());
    }


    public void sendUCommandDisconnect() throws IOException {
        WorldUps.UCommands.Builder builder = WorldUps.UCommands.newBuilder();
        builder.setDisconnect(true);
        builder.build();
        sendToWorld(builder.build());
    }

    public void sendUCommandUGoPickUp(Integer truckid, Integer whid, Long seqnum){
        WorldUps.UGoPickup.Builder uGoPickUpBuilder = WorldUps.UGoPickup.newBuilder();
        uGoPickUpBuilder.setSeqnum(seqnum);
        uGoPickUpBuilder.setWhid(whid);
        uGoPickUpBuilder.setTruckid(truckid);
        uGoPickUpBuilder.build();
        WorldUps.UCommands.Builder builder  = WorldUps.UCommands.newBuilder();
        builder.addPickups(uGoPickUpBuilder);
        try {
            sendToWorld(builder.build());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void sendUCommand(WorldUps.UCommands uCommands){
        try {
            sendToWorld(uCommands);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
