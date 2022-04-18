package edu.duke.ece568.server;

import com.google.protobuf.CodedInputStream;
import edu.duke.ece568.server.protocol.WorldUps;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

public class Response {
    private Socket serverToWorldSocket;
    private WorldUps.UConnected.Builder uConnected;
    public Response(Socket serverToWorldSocket) {
        this.serverToWorldSocket = serverToWorldSocket;
    }

    private WorldUps.UConnected.Builder recvFromWorldUConnected() throws IOException {
        InputStream inputStream = this.serverToWorldSocket.getInputStream();
        CodedInputStream codedInputStream = CodedInputStream.newInstance(inputStream);
        //int length = codedInputStream.readRawVarint32();
        WorldUps.UConnected uConnected = WorldUps.UConnected.parseFrom(codedInputStream.readByteArray());
        return uConnected.toBuilder();
    }

    public WorldUps.UResponses.Builder recvFromWorld() throws IOException {
        InputStream inputStream = this.serverToWorldSocket.getInputStream();
        CodedInputStream codedInputStream = CodedInputStream.newInstance(inputStream);
        //int length = codedInputStream.readRawVarint32();
        WorldUps.UResponses  uResponses = WorldUps.UResponses.parseFrom(codedInputStream.readByteArray());
        return uResponses.toBuilder();
    }

    public void recvUConnected(){
        try {
            this.uConnected = recvFromWorldUConnected();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //UConnected
    public Long getWorldid(){
        return this.uConnected.getWorldid();
    }
    //UConnected
    public String getResult(){
        return this.uConnected.getResult();
    }
}
