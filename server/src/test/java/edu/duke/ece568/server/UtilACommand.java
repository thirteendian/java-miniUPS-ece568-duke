package edu.duke.ece568.server;

import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.Message;
import edu.duke.ece568.server.protocol.WorldAmazon;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class UtilACommand {
    private Socket serverToWorldSocket;

    public UtilACommand(Socket serverToWorldSocket) {
        this.serverToWorldSocket = serverToWorldSocket;
    }
    private void sendToWorld(Message msg) throws IOException {
        OutputStream outputStream = this.serverToWorldSocket.getOutputStream();
        CodedOutputStream codedOutputStream = CodedOutputStream.newInstance(outputStream);
        codedOutputStream.writeUInt32NoTag(msg.toByteArray().length);
        msg.writeTo(codedOutputStream);
        codedOutputStream.flush();
    }

    public void sendAConnect(Long wordid) throws IOException {
        WorldAmazon.AInitWarehouse aInitWarehouse = WorldAmazon.AInitWarehouse.newBuilder().setY(1).setX(1).setId(1).build();
        WorldAmazon.AConnect aConnect = WorldAmazon.AConnect.newBuilder().setWorldid(wordid).setIsAmazon(true).addInitwh(aInitWarehouse).build();
        sendToWorld(aConnect);
    }

}
