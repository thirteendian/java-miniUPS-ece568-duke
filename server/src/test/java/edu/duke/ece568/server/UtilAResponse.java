package edu.duke.ece568.server;

import com.google.protobuf.CodedInputStream;
import edu.duke.ece568.server.protocol.UpsAmazon;
import edu.duke.ece568.server.protocol.WorldAmazon;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

public class UtilAResponse {
    private Socket serverToWorldSocket;
    private Socket amazonToUPSSocket;

    public UtilAResponse(Socket serverToWorldSocket, Socket amazonToUPSSocket) {
        this.serverToWorldSocket = serverToWorldSocket;
        this.amazonToUPSSocket = amazonToUPSSocket;
    }

    public WorldAmazon.AConnected.Builder recvFromWorldAConnected() throws IOException {
        InputStream inputStream = this.serverToWorldSocket.getInputStream();
        CodedInputStream codedInputStream = CodedInputStream.newInstance(inputStream);
        //int length = codedInputStream.readRawVarint32();
        WorldAmazon.AConnected aConnected = WorldAmazon.AConnected.parseFrom(codedInputStream.readByteArray());
        return aConnected.toBuilder();
    }
    public UpsAmazon.AUResponse.Builder recvFromUPS() throws IOException {
        InputStream inputStream = this.amazonToUPSSocket.getInputStream();
        CodedInputStream codedInputStream = CodedInputStream.newInstance(inputStream);
        //int length = codedInputStream.readRawVarint32();
        UpsAmazon.AUResponse auResponse= UpsAmazon.AUResponse.parseFrom(codedInputStream.readByteArray());
        return auResponse.toBuilder();
    }

    public WorldAmazon.AResponses.Builder recvFromWorld() throws IOException {
        InputStream inputStream = this.amazonToUPSSocket.getInputStream();
        CodedInputStream codedInputStream = CodedInputStream.newInstance(inputStream);
        //int length = codedInputStream.readRawVarint32();
        WorldAmazon.AResponses aResponses= WorldAmazon.AResponses.parseFrom(codedInputStream.readByteArray());
        return aResponses.toBuilder();
    }
    public UpsAmazon.USendWorldID.Builder recvFromUPSWorldID() throws IOException {
        InputStream inputStream = this.amazonToUPSSocket.getInputStream();
        CodedInputStream codedInputStream = CodedInputStream.newInstance(inputStream);
        //int length = codedInputStream.readRawVarint32();
        UpsAmazon.USendWorldID auResponse= UpsAmazon.USendWorldID.parseFrom(codedInputStream.readByteArray());
        return auResponse.toBuilder();
    }

}
