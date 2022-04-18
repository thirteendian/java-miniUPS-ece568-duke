package edu.duke.ece568.server;

import edu.duke.ece568.server.protocol.WorldAmazon;
import edu.duke.ece568.server.protocol.WorldUps;
import edu.duke.ece568.shared.Status;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;

public class UCommandResponseTest {
    @Test
    public void test_CreateWorld() throws IOException {
        final String WORLD_HOST = "192.168.1.8";
        final Integer WORLD_PORTNUM = 12345;
        final Integer WORLD_AMAZON_PORTNUM= 23456;
        final Integer CLIENT_PORTNUM = 34487;
        final Integer ACLIENT_PORTNUM = 34488;
        final String SERVER_HOST = "0.0.0.0";
        final Integer SERVER_PORTNUM = 4444;
        //=====================================Create World==============================================//
        //Connect to World
        Server server = new Server(WORLD_HOST,WORLD_PORTNUM,CLIENT_PORTNUM,SERVER_HOST,SERVER_PORTNUM);
        Server aServer = new Server(WORLD_HOST,WORLD_AMAZON_PORTNUM,ACLIENT_PORTNUM,SERVER_HOST,SERVER_PORTNUM);
        Command uCommand = new Command(server.getServerToWorldSocket());
        UtilACommand aCommand = new UtilACommand(aServer.getServerToWorldSocket());
        Response uResponse = new Response(server.getServerToWorldSocket());
        UtilAResponse aResponse = new UtilAResponse(aServer.getServerToWorldSocket());
        //Check Database has 3 idle truck
        PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC();
        postgreSQLJDBC.dropAllTable();
        postgreSQLJDBC.createAllTable();
        //Created World, Add Truck to Database
        uCommand.sendUConnect(null,3);
        uResponse.recvUConnected();
        System.out.print("UPS :");
        System.out.println(uResponse.getWorldid());
        System.out.println(uResponse.getResult());
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //Amazon connect to World
        aCommand.sendAConnect(uResponse.getWorldid());
        WorldAmazon.AConnected.Builder aConnected = aResponse.recvFromWorldAConnected();
        System.out.print("Amazon :");
        System.out.println(aConnected.getWorldid());


        HashMap hashMap = postgreSQLJDBC.getTruckGroupOfStatus(new Status().tIdel);
        System.out.println(hashMap);
        //UGoPickup
        uCommand.sendUCommandUGoPickUp(1,1,SequenceNumCounter.getInstance().getCurrent_id());
        WorldUps.UResponses.Builder uResponses= uResponse.recvFromWorld();
        System.out.print("UPS :");
        System.out.println(uResponses.getAcksList());

        //Update TruckStatus
        postgreSQLJDBC.updateTruckStatus(1L,2,3,2,null);
        postgreSQLJDBC.updateTruckStatus(2L,2,3,null,null);
        postgreSQLJDBC.updateTruckStatus(3L,2,3,null,null);
        //Check Database has 2 idle truck
        hashMap = postgreSQLJDBC.getTruckGroupOfStatus(new Status().tTraveling);
        System.out.println(hashMap);
        postgreSQLJDBC.updateTruckStatus(1L,5,6,3,true);
        System.out.println(postgreSQLJDBC.getTruckGroupOfStatus(new Status().tArriveWarehouse));
        postgreSQLJDBC.close();

    }
}
