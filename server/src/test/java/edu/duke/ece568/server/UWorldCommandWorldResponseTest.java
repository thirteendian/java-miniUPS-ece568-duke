package edu.duke.ece568.server;

import edu.duke.ece568.server.Amazon.ASeqNumCounter;
import edu.duke.ece568.server.World.WorldCommand;
import edu.duke.ece568.server.World.WorldResponse;
import edu.duke.ece568.server.protocol.WorldAmazon;
import edu.duke.ece568.server.protocol.WorldUps;
import edu.duke.ece568.shared.Status;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class UWorldCommandWorldResponseTest {
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
        WorldCommand uWorldCommand = new WorldCommand(server.getServerToWorldSocket());
        UtilACommand aCommand = new UtilACommand(aServer.getServerToWorldSocket());
        WorldResponse uWorldResponse = new WorldResponse(server.getServerToWorldSocket());
        UtilAResponse aResponse = new UtilAResponse(aServer.getServerToWorldSocket());
        //Check Database has 3 idle truck
        PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC();
        postgreSQLJDBC.dropAllTable();
        postgreSQLJDBC.createAllTable();
        //Created World, Add Truck to Database
        uWorldCommand.sendUConnect(null,3);
        uWorldResponse.recvUConnected();
        System.out.print("UPS :");
        System.out.println(uWorldResponse.getWorldid());
        System.out.println(uWorldResponse.getResult());
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //Amazon connect to World
        aCommand.sendAConnect(uWorldResponse.getWorldid());
        WorldAmazon.AConnected.Builder aConnected = aResponse.recvFromWorldAConnected();
        System.out.print("Amazon :");
        System.out.println(aConnected.getWorldid());


        ArrayList<Long> Array= postgreSQLJDBC.getTruckGroupOfStatus(new Status().tIdel);
        System.out.println(Array);
        //UGoPickup
        uWorldCommand.sendUCommandUGoPickUp(1,1, ASeqNumCounter.getInstance().getCurrSeqNum());
        WorldUps.UResponses.Builder uResponses= uWorldResponse.recvUResponse();
        System.out.print("UPS :");
        System.out.println(uResponses.getAcksList());

        //Update TruckStatus
        postgreSQLJDBC.updateTruckStatus(1L,2,3,2,null);
        postgreSQLJDBC.updateTruckStatus(2L,2,3,null,null);
        postgreSQLJDBC.updateTruckStatus(3L,2,3,null,null);
        //Check Database has 2 idle truck
        Array = postgreSQLJDBC.getTruckGroupOfStatus(new Status().tTraveling);
        System.out.println(Array);
        postgreSQLJDBC.updateTruckStatus(1L,5,6,3,true);
        System.out.println(postgreSQLJDBC.getTruckGroupOfStatus(new Status().tArriveWarehouse));
        postgreSQLJDBC.close();

    }
}
