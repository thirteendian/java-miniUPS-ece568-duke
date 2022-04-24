package edu.duke.ece568.server;

import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.Message;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class AmazonServer {
    //Server Communicate with World
    private Socket serverToWorldSocket;
    private final String serverToWorldHost;
    private final Integer serverToWorldPortNum;

    //Server receive connection from UPS
    private ServerSocket serverToUPSSocket;
    private Socket UPSClientSocket;

    public AmazonServer(String serverToWorldHost, Integer serverToWorldPortNum) throws IOException {
        this.serverToWorldHost = serverToWorldHost;
        this.serverToWorldPortNum = serverToWorldPortNum;
        this.serverToWorldSocket = new Socket(this.serverToWorldHost,this.serverToWorldPortNum);
        this.serverToUPSSocket = new ServerSocket(11111);
    }
    public void acceptConnection() throws IOException {
        this.UPSClientSocket = this.serverToUPSSocket.accept();
    }

    public void sendToUPS(Message msg) throws IOException {
        OutputStream outputStream = this.UPSClientSocket.getOutputStream();
        CodedOutputStream codedOutputStream = CodedOutputStream.newInstance(outputStream);
        codedOutputStream.writeUInt32NoTag(msg.toByteArray().length);
        msg.writeTo(codedOutputStream);
        codedOutputStream.flush();
    }

    public Socket getServerToWorldSocket(){
        return this.serverToWorldSocket;
    }
    public Socket getServerToUPSSocket(){return this.UPSClientSocket;}
}
