package edu.duke.ece568.server.Amazon;

import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.Message;
import edu.duke.ece568.server.protocol.UpsAmazon;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class AmazonCommand {
    Socket serverToAmazonSocket;

    public AmazonCommand(Socket serverToAmazonSocket) {
        this.serverToAmazonSocket = serverToAmazonSocket;
    }

    private synchronized void sendToAmazon(Message msg) throws IOException {
        OutputStream outputStream = this.serverToAmazonSocket.getOutputStream();
        CodedOutputStream codedOutputStream = CodedOutputStream.newInstance(outputStream);
        codedOutputStream.writeUInt32NoTag(msg.toByteArray().length);
        msg.writeTo(codedOutputStream);
        codedOutputStream.flush();
    }

    public void sendUSendWorldID(Long worldid, Long seqnum) throws IOException {
        UpsAmazon.USendWorldID.Builder builder = UpsAmazon.USendWorldID.newBuilder();
        builder.setSeqnum(seqnum).setWorldId(worldid);
        sendToAmazon(builder.build());
    }

    public void sendUTruckArrivedNotification(Long truckid, Long seqnum){
        UpsAmazon.UTruckArrivedNotification.Builder builder = UpsAmazon.UTruckArrivedNotification.newBuilder();
        builder.setTruckId(truckid).setSeqnum(seqnum).build();
        UpsAmazon.AUResponse.Builder auResponseBuilder = UpsAmazon.AUResponse.newBuilder();
        auResponseBuilder.addArrived(builder);
        try {
            sendToAmazon(auResponseBuilder.build());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //Possible we only need this method
    public void sendAUResponse(UpsAmazon.AUResponse auResponse){
        try {
            sendToAmazon(auResponse);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
