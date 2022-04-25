package edu.duke.ece568.server;

import edu.duke.ece568.server.Amazon.*;
import edu.duke.ece568.server.World.*;
import edu.duke.ece568.shared.PostgreSQLJDBC;

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
    private ServerSocket serverToAmazonServerSocket;

    //Command
    private WorldCommand worldCommand;
    private WorldResponse worldResponse;
    private AmazonCommand amazonCommand;
    private AmazonResponse amazonResponse;
    //private HashMap<String, Socket> clientSocketHashMap;
    public Server(String serverToWorldHost, int serverToWorldPortNum, int serverToClientPortNum, String serverToAmazonHost, int serverToAmazonPortNum) throws IOException {
        //Construct Server DATABASE
        PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC();
        postgreSQLJDBC.dropAllTable();
        postgreSQLJDBC.createAllTable();
        postgreSQLJDBC.close();
        //Establish Connection with World
        this.serverToWorldHost = serverToWorldHost;
        this.serverToWorldPortNum = serverToWorldPortNum;
        this.serverToWorldSocket = new Socket(this.serverToWorldHost,this.serverToWorldPortNum);
        //Establish Connection with Client
        this.serverToClientPortNum = serverToClientPortNum;
        this.serverSocket = new ServerSocket(this.serverToClientPortNum);
        //Establish Connection with Amazon(If as Client)
        this.serverToAmazonPortNum = serverToAmazonPortNum;
        this.serverToAmazonHost = serverToAmazonHost;
//        this.serverToAmazonServerSocket = new ServerSocket(11111);
    }

    public Socket acceptConnection() throws IOException {
        Socket clientSocket = this.serverSocket.accept();
        return clientSocket;
    }
    public void connectToAmazon() throws IOException {
        this.serverToAmazonSocket = new Socket(serverToAmazonHost, serverToAmazonPortNum);
        this.worldCommand = new WorldCommand(serverToWorldSocket);
        this.worldResponse = new WorldResponse(serverToWorldSocket);
        this.amazonCommand = new AmazonCommand(serverToAmazonSocket);
        this.amazonResponse = new AmazonResponse(serverToAmazonSocket);
    }
    public void startWorldListenerThread(Boolean debugMode){
        //WorldListenerThread
        WorldListenerRunnable worldListenerRunnable = new WorldListenerRunnable(serverToWorldSocket,serverToAmazonSocket,worldResponse,worldCommand,amazonCommand,debugMode);
        Thread worldListener = new Thread(worldListenerRunnable);
        worldListener.start();
    }
    public void startWorldResendThread(){
        //WorldResendThread
        WorldResendRunnable worldResendRunnable = new WorldResendRunnable(serverToWorldSocket,worldCommand);
        Thread worldResend = new Thread(worldResendRunnable);
        worldResend.start();
    }
    public void startTruckUpdateThread(){
        //TruckUpdateThread
        TruckUpdateRunnable truckUpdateRunnable = new TruckUpdateRunnable(serverToWorldSocket,serverToAmazonSocket,amazonCommand,worldCommand,amazonResponse);
        Thread truckUpdator = new Thread(truckUpdateRunnable);
        truckUpdator.start();
    }
    public void startAmazonListenerThread(){
        //AmazonListenerThread
        AmazonListenerRunnable amazonListenerRunnable = new AmazonListenerRunnable(serverToWorldSocket,serverToAmazonSocket,amazonResponse,amazonCommand,worldCommand);
        Thread amazonListener = new Thread(amazonListenerRunnable);
        amazonListener.start();
    }
    public void startAmazonResendThread(){
        //AmazonResendThread
        AmazonResendRunnable amazonResendRunnable = new AmazonResendRunnable(serverToAmazonSocket,amazonCommand);
        Thread amazonResend = new Thread(amazonResendRunnable);
        amazonResend.start();
    }


    public Socket getServerToWorldSocket() {
        return serverToWorldSocket;
    }
    public Socket getServerToAmazonSocket(){
        return serverToAmazonSocket;
    }

    public static void main(String[] args) {
//        final String WORLD_HOST = "192.168.1.8";
        final Integer WORLD_PORTNUM = 12345;
        final Integer CLIENT_PORTNUM = 34487;
        final String AMAZON_HOST = "vcm-24627.vm.duke.edu";
        final Integer AMAZON_PORTNUM = 12345;
        String WORLD_HOST = "vcm-25470.vm.duke.edu";


//        System.out.print("Entering Your WorldSim Host Address: ");
//        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
//        try {
//            WORLD_HOST= reader.readLine();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        try {
            //Connect to World, Amazon, Establish Server to Client
            Server server = new Server(WORLD_HOST,WORLD_PORTNUM, CLIENT_PORTNUM, AMAZON_HOST, AMAZON_PORTNUM);
            server.connectToAmazon();
            server.startWorldListenerThread(false);
            server.startTruckUpdateThread();
            server.startWorldResendThread();
            server.startAmazonListenerThread();
            server.startAmazonResendThread();
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
