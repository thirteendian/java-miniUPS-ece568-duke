package edu.duke.ece568.server;

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
//        this.serverToAmazonSocket = new Socket(this.serverToAmazonHost,this.serverToAmazonPortNum);
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
            //World Amazon Thread
            AmazonCommunicationRunnable amazonCommunicationRunnable = new AmazonCommunicationRunnable(server.getServerToWorldSocket());
            Thread t = new Thread(amazonCommunicationRunnable);
            t.start();
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
