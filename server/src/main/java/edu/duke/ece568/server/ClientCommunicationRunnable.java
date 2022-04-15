package edu.duke.ece568.server;

import java.net.Socket;

public class ClientCommunicationRunnable implements Runnable {
    private Socket clientSocket;
//    private Socket serverToWorldSocket;

    public ClientCommunicationRunnable(Socket clientSocket) {
        this.clientSocket = clientSocket;
//        this.serverToWorldSocket = serverToWorldSocket;
    }

    @Override
    public void run() {

    }
}
