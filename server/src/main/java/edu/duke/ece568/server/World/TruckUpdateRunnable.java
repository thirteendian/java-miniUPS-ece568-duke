package edu.duke.ece568.server.World;

import edu.duke.ece568.server.Amazon.AmazonCommand;
import edu.duke.ece568.server.Amazon.AmazonResponse;

import java.net.Socket;

public class TruckUpdateRunnable implements Runnable{
    Socket serverToWorldSocket;
    Socket serverToAmazonSocket;
    AmazonCommand amazonCommand;
    WorldCommand worldCommand;
    AmazonResponse amazonResponse;

    public TruckUpdateRunnable(Socket serverToWorldSocket, Socket serverToAmazonSocket, AmazonCommand amazonCommand, WorldCommand worldCommand, AmazonResponse amazonResponse) {
        this.serverToWorldSocket = serverToWorldSocket;
        this.serverToAmazonSocket = serverToAmazonSocket;
        this.amazonCommand = amazonCommand;
        this.worldCommand = worldCommand;
        this.amazonResponse = amazonResponse;
    }

    @Override
    public void run() {
        while(true){
        }
    }
}
