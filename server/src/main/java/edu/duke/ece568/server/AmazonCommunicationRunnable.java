package edu.duke.ece568.server;

import java.io.IOException;
import java.net.Socket;

public class AmazonCommunicationRunnable implements Runnable{
    private Socket serverToWorldSocket;
    private Command command;
    private Response response;

    public AmazonCommunicationRunnable(Socket serverToWorldSocket) {
        this.serverToWorldSocket = serverToWorldSocket;
        this.command = new Command(this.serverToWorldSocket);
        this.response =new Response(this.serverToWorldSocket);
    }

    @Override
    public void run() {
        //Connect To world
        try {
            this.command.sendUConnect(null,10);
            this.response.recvUConnected();
            System.out.println(response.getWorldid());
            System.out.println(response.getResult());
        } catch (IOException e) {
            e.printStackTrace();
        }
        while(true){

        }
    }
}
