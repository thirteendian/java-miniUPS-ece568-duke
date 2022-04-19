package edu.duke.ece568.server.Amazon;

import com.google.protobuf.CodedInputStream;
import edu.duke.ece568.server.protocol.UpsAmazon;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

public class AmazonResponse {
    private Socket serverToAmazonSocket;
    public AmazonResponse(Socket serverToAmazonSocket) {
        this.serverToAmazonSocket = serverToAmazonSocket;
    }

    public UpsAmazon.AURequest.Builder recvFromAmazon() throws IOException {
        InputStream inputStream = this.serverToAmazonSocket.getInputStream();
        CodedInputStream codedInputStream = CodedInputStream.newInstance(inputStream);
        //int length = codedInputStream.readRawVarint32();
        UpsAmazon.AURequest auRequest = UpsAmazon.AURequest.parseFrom(codedInputStream.readByteArray());
        return auRequest.toBuilder();
    }

}
