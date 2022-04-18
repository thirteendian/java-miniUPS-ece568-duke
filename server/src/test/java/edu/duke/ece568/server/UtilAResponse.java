package edu.duke.ece568.server;

import com.google.protobuf.CodedInputStream;
import edu.duke.ece568.server.protocol.WorldAmazon;
import edu.duke.ece568.server.protocol.WorldUps;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

public class UtilAResponse {
    private Socket serverToWorldSocket;

    public UtilAResponse(Socket serverToWorldSocket) {
        this.serverToWorldSocket = serverToWorldSocket;
    }

    public WorldAmazon.AConnected.Builder recvFromWorldAConnected() throws IOException {
        InputStream inputStream = this.serverToWorldSocket.getInputStream();
        CodedInputStream codedInputStream = CodedInputStream.newInstance(inputStream);
        //int length = codedInputStream.readRawVarint32();
        WorldAmazon.AConnected aConnected = WorldAmazon.AConnected.parseFrom(codedInputStream.readByteArray());
        return aConnected.toBuilder();
    }
}
