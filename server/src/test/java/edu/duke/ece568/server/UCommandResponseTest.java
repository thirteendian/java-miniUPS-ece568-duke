package edu.duke.ece568.server;

import edu.duke.ece568.server.protocol.WorldAmazon;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class UCommandResponseTest {
    @Test
    public void test_CommandResponseTest() throws IOException {
        final String WORLD_HOST = "192.168.1.8";
        final Integer WORLD_PORTNUM = 23456;
        final Integer CLIENT_PORTNUM = 34487;
        final Integer ACLIENT_PORTNUM = 34488;
        //=====================================Create World==============================================//
        //Connect to World
        Server server = new Server(WORLD_HOST,WORLD_PORTNUM,CLIENT_PORTNUM);
        Server aServer = new Server(WORLD_HOST,WORLD_PORTNUM,ACLIENT_PORTNUM);
        Command uCommand = new Command(server.getServerToWorldSocket());
        UtilACommand aCommand = new UtilACommand(aServer.getServerToWorldSocket());
        Response uResponse = new Response(server.getServerToWorldSocket());
        UtilAResponse aResponse = new UtilAResponse(aServer.getServerToWorldSocket());
        //Created World
        uCommand.sendUConnect(40L,3);
        uResponse.recvUConnected();
        System.out.print("UPS :");
        System.out.println(uResponse.getResult());
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //Amazon connect to World
        aCommand.sendAConnect(uResponse.getWorldid());
        WorldAmazon.AConnect.Builder aConnected = aResponse.recvFromWorldAConnected();
        System.out.print("Amazon :");
        System.out.println(aConnected.getWorldid());
        //=====================================Send PickUp==============================================//

    }
}
