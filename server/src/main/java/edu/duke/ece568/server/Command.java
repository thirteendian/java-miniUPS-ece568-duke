package edu.duke.ece568.server;

import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.Message;
import edu.duke.ece568.server.protocol.WorldUps;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class Command {
    private Socket serverToWorldSocket;

    public Command(Socket serverToWorldSocket) {
        this.serverToWorldSocket = serverToWorldSocket;
    }


    private void sendToWorld(Message msg) throws IOException {
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
            for (int i = 0; i < numOfTruck; i++) {
                WorldUps.UInitTruck.Builder uInitTruck = WorldUps.UInitTruck.newBuilder();
                uInitTruck.setX(0).setY(0).setId(i + 1);
                uInitTruck.build();
                builder.addTrucks(uInitTruck);
                //TODO: Generate SQL Table for Truck And Each Truck
            }
        }
        sendToWorld(builder.build());
    }


    public void sendUCommandDisconnect() throws IOException {
        WorldUps.UCommands.Builder builder = WorldUps.UCommands.newBuilder();
        builder.setDisconnect(true);
        builder.build();
        sendToWorld(builder.build());
    }

    public void sendUCommandUGoPickUp(Integer truckid){

        WorldUps.UCommands.Builder builder  = WorldUps.UCommands.newBuilder();
    }

}
