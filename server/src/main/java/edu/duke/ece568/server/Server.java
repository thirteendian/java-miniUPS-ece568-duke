package edu.duke.ece568.server;

import edu.duke.ece568.server.Amazon.ASeqNumCounter;
import edu.duke.ece568.server.Amazon.AmazonCommand;
import edu.duke.ece568.server.Amazon.AmazonListenerRunnable;
import edu.duke.ece568.server.Amazon.AmazonResponse;
import edu.duke.ece568.server.World.TruckUpdateRunnable;
import edu.duke.ece568.server.World.WorldCommand;
import edu.duke.ece568.server.World.WorldListenerRunnable;
import edu.duke.ece568.server.World.WorldResponse;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    //Server Communicate with World
    private Socket serverToWorldSocket;
    private final String serverToWorldHost;
    private final Integer serverToWorldPortNum;

    //Server Communicate with Client
    private final int serverToClientPortNum;
    private final ServerSocket serverSocket;

    //Server Communicate with Amazon
    private Socket serverToAmazonSocket;
    private final String serverToAmazonHost;
    private final int serverToAmazonPortNum;


    //private HashMap<String, Socket> clientSocketHashMap;
    public Server(String serverToWorldHost, int serverToWorldPortNum, int serverToClientPortNum, String serverToAmazonHost, int serverToAmazonPortNum) throws IOException {
        //Establish Connection with World
        this.serverToWorldHost = serverToWorldHost;
        this.serverToWorldPortNum = serverToWorldPortNum;
        this.serverToWorldSocket = new Socket(this.serverToWorldHost,this.serverToWorldPortNum);
        //Establish Connection with Client
        this.serverToClientPortNum = serverToClientPortNum;
        this.serverSocket = new ServerSocket(this.serverToClientPortNum);
        //Establish Connection with Amazon
        this.serverToAmazonPortNum = serverToAmazonPortNum;
        this.serverToAmazonHost = serverToAmazonHost;
        //this.serverToAmazonSocket = new Socket(this.serverToAmazonHost,this.serverToAmazonPortNum);
        //Establish SequenceNumberCounter
        ASeqNumCounter worldSeqNum = new ASeqNumCounter();
        ASeqNumCounter amazonSeqNum = new ASeqNumCounter();
        //Establish Command and Response
        WorldCommand worldCommand = new WorldCommand(serverToWorldSocket);
        WorldResponse worldResponse = new WorldResponse(serverToWorldSocket);
        AmazonCommand amazonCommand = new AmazonCommand(serverToAmazonSocket);
        AmazonResponse amazonResponse = new AmazonResponse(serverToAmazonSocket);

        //WorldListenerThread
        WorldListenerRunnable worldListenerRunnable = new WorldListenerRunnable(serverToWorldSocket,serverToAmazonSocket,worldResponse,worldCommand,amazonCommand);
        Thread worldListener = new Thread(worldListenerRunnable);

        //WorldResendThread

        //AmazonListenerThread
        AmazonListenerRunnable amazonListenerRunnable = new AmazonListenerRunnable(serverToWorldSocket,serverToAmazonSocket,amazonResponse,amazonCommand,worldCommand);
        Thread amazonListener = new Thread(amazonListenerRunnable);

        //AmazonResendThread
        //TruckUpdateThread
        TruckUpdateRunnable truckUpdateRunnable = new TruckUpdateRunnable(serverToWorldSocket,serverToAmazonSocket,amazonCommand,worldCommand,amazonResponse);
        Thread truckUpdator = new Thread(truckUpdateRunnable);
        worldListener.start();
        amazonListener.start();
        truckUpdator.start();
    }

    public Socket acceptConnection() throws IOException {
        Socket clientSocket = this.serverSocket.accept();
        return clientSocket;
    }

    public Socket getServerToWorldSocket() {
        return serverToWorldSocket;
    }

    public String getServerToWorldHost() {
        return serverToWorldHost;
    }

    public Integer getServerToWorldPortNum() {
        return serverToWorldPortNum;
    }

    public int getServerToClientPortNum() {
        return serverToClientPortNum;
    }

    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    public static void main(String[] args) {
        final String WORLD_HOST = "192.168.1.8";
        final Integer WORLD_PORTNUM = 23456;
        final Integer CLIENT_PORTNUM = 34487;
        final String AMAZON_HOST = "0.0.0.0";
        final Integer AMAZON_PORTNUM = 4444;

        try {
            //Establish SQL
            PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC();
            postgreSQLJDBC.dropAllTable();
            postgreSQLJDBC.createAllTable();
            postgreSQLJDBC.close();
            //Connect to World, Amazon, Establish Server to Client
            Server server = new Server(WORLD_HOST,WORLD_PORTNUM, CLIENT_PORTNUM, AMAZON_HOST, AMAZON_PORTNUM);
            //Client Thread
            while(true){
                Socket clientSocket = server.acceptConnection();
                ClientCommunicationRunnable clientCommunicationRunnable = new ClientCommunicationRunnable(clientSocket);
                Thread c = new Thread(clientCommunicationRunnable);
                c.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
