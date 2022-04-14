package edu.duke.ece568.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    //Server Communicate with World
    Socket serverToWorldSocket;
    private final String serverToWorldHost;
    private final Integer serverToWorldPortNum;
    //Server Communicate with Client
    private final int serverToClientPortNum;
    private final ServerSocket serverSocket;
    private Socket clientSocket;
    public Server(String serverToWorldHost, int serverToWorldPortNum, int serverToClientPortNum) throws IOException {
        //Establish Connection with World
        this.serverToWorldHost = serverToWorldHost;
        this.serverToWorldPortNum = serverToWorldPortNum;
        this.serverToWorldSocket = new Socket(this.serverToWorldHost,this.serverToWorldPortNum);
        //Establish Connection with Client
        this.serverToClientPortNum = serverToClientPortNum;
        this.serverSocket = new ServerSocket(this.serverToClientPortNum);
    }
    public void acceptConnection() throws IOException {
        this.clientSocket = this.serverSocket.accept();
    }

    public static void main(String[] args) {
        final String WORLD_HOST = "127.0.0.1";
        final Integer WORLD_PORTNUM = 23456;
        final Integer CLIENT_PORTNUM = 34487;
        try {
            Server server = new Server(WORLD_HOST,WORLD_PORTNUM,CLIENT_PORTNUM);
            //server.acceptConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
