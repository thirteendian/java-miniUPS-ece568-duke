package edu.duke.ece568.server;

import edu.duke.ece568.server.protocol.UpsAmazon;
import edu.duke.ece568.server.protocol.WorldAmazon;
import edu.duke.ece568.server.protocol.WorldUps;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class ServerTest {
    final String WORLD_HOST = "192.168.1.8";
    final Integer WORLD_PORTNUM = 12345;
    final Integer WORLD_AMAZON_PORTNUM = 23456;

    final Integer CLIENT_PORTNUM = 34487;
    final Integer ACLIENT_PORTNUM = 34488;

    final String AMAZON_HOST = "127.0.0.1";
    final Integer AMAZON_PORTNUM = 4444;

    @Test
    public void test_CreateWorld() throws IOException {
        //=====================================Create Socket==============================================//
        //Connect to World Connect to AMAZON
        Server server = new Server(WORLD_HOST, WORLD_PORTNUM, CLIENT_PORTNUM, AMAZON_HOST, AMAZON_PORTNUM);
        AmazonServer amazonServer = new AmazonServer(WORLD_HOST, WORLD_AMAZON_PORTNUM);
        server.connectToAmazon();
        amazonServer.acceptConnection();

        server.startWorldListenerThread(false);//Connect World, send to Amazon
        server.startAmazonListenerThread();
        UtilACommand aCommand = new UtilACommand(amazonServer.getServerToWorldSocket(), amazonServer.getServerToUPSSocket());
        UtilAResponse aResponse = new UtilAResponse(amazonServer.getServerToWorldSocket(), amazonServer.getServerToUPSSocket());
        //=====================================AMAZON CONNECTION==============================================//

        aCommand.sendAConnect(aResponse.recvFromUPSWorldID().getWorldId());
        WorldAmazon.AConnected.Builder aConnected = aResponse.recvFromWorldAConnected();
        System.out.print("Amazon :");
        System.out.print(aConnected.getResult());
        System.out.print(" worldID: ");
        System.out.println(aConnected.getWorldid());
    }

    @Test
    public void test_TruckUpdate() throws IOException {
        //=====================================Create Socket==================================================//
        //Connect to World Connect to AMAZON
        Server server = new Server(WORLD_HOST, WORLD_PORTNUM, CLIENT_PORTNUM, AMAZON_HOST, AMAZON_PORTNUM);
        AmazonServer amazonServer = new AmazonServer(WORLD_HOST, WORLD_AMAZON_PORTNUM);
        server.connectToAmazon();
        amazonServer.acceptConnection();

        server.startWorldListenerThread(false);//Connect World, send to Amazon
        server.startAmazonListenerThread();
        server.startTruckUpdateThread();
        server.startWorldResendThread();
        server.startAmazonResendThread();
        UtilACommand aCommand = new UtilACommand(amazonServer.getServerToWorldSocket(), amazonServer.getServerToUPSSocket());
        UtilAResponse aResponse = new UtilAResponse(amazonServer.getServerToWorldSocket(), amazonServer.getServerToUPSSocket());

        //=====================================AMAZON CONNECTION==============================================//
        aCommand.sendAConnect(aResponse.recvFromUPSWorldID().getWorldId());
        WorldAmazon.AConnected.Builder aConnected = aResponse.recvFromWorldAConnected();
        System.out.print("Amazon :");
        System.out.print(aConnected.getResult());
        System.out.print(" worldID: ");
        System.out.println(aConnected.getWorldid());

        //Buy From warehouse 1
        WorldAmazon.ACommands.Builder aCommandBuy = WorldAmazon.ACommands.newBuilder().addBuy(WorldAmazon.APurchaseMore.newBuilder().setSeqnum(888888L).setWhnum(1).addThings(WorldAmazon.AProduct.newBuilder().setId(440).setCount(14).setDescription("Descriotion").build()).build());
        aCommand.sendACommand(aCommandBuy.setSimspeed(200).build());


        while(true){
            WorldAmazon.AResponses.Builder aResponses = aResponse.recvFromWorld();
            if(aResponses.getArrivedList().size()>0){
                System.out.println(aResponses.getArrivedList().get(0));
                break;
            }
        }
//
//
//        //Send Pack to World
//        WorldAmazon.APack.Builder aPack = WorldAmazon.APack.newBuilder().setWhnum(1).setSeqnum(888889L).setShipid(1L).addThings(WorldAmazon.AProduct.newBuilder().setId(440).setDescription("Descriotion").setCount(14).build());
//        WorldAmazon.ACommands.Builder aCommandPack = WorldAmazon.ACommands.newBuilder().addTopack(aPack);
//        aCommand.sendACommand(aCommandPack.setSimspeed(1000).build());
//
//        //Get Pack ACK
//        WorldAmazon.AResponses.Builder aResponses2 = aResponse.recvFromWorld();
//        if (aResponses2.getAcksList().size() != 0) {
//            System.out.println("[MAIN]  ACK: " + aResponses2.getAcks(0));
//        }

        //AShippingRequest
        UpsAmazon.AShippingRequest.Builder aShippingRequest = UpsAmazon.AShippingRequest.newBuilder();
        aShippingRequest.setSeqnum(1).setLocation(UpsAmazon.AWareHouseLocation.newBuilder().setX(1).setY(1).setWarehouseid(1).build()).addShipment(UpsAmazon.AShipment.newBuilder().setDestX(2).setDestY(2).setEmailaddress("@duke.edu").setPackageId(1).addProduct(UpsAmazon.Product.newBuilder().setDescription("Descriotion").setCount(14).build()).build());
        UpsAmazon.AURequest.Builder auRequest = UpsAmazon.AURequest.newBuilder();
        auRequest.addShippingRequest(aShippingRequest);
        aCommand.sendAURequest(auRequest.build());

        //Receive ACK =1
        for (Long ack : aResponse.recvFromUPS().getAcksList()) {
            System.out.println("[MAIN]      [AShippingRequest ACK]: " + ack);
        }

        //RECEIVE from UPS AUResponse: UTruckArrivedNotification
        while (true) {
            UpsAmazon.AUResponse.Builder auResponse = aResponse.recvFromUPS();
            if (auResponse.getArrivedList().size() != 0) {
                System.out.println("[MAIN]      [UTruckArrivedNotification]  truckID: " + auResponse.getArrived(0).getTruckId() + " aSeqNum: " + auResponse.getArrived(0).getSeqnum());
                break;
            }
        }


//        //Send Load to World
//        WorldAmazon.APutOnTruck.Builder aPutOnTruck = WorldAmazon.APutOnTruck.newBuilder().setWhnum(1).setTruckid(1).setShipid(1).setSeqnum(444444L);
//        aCommand.sendACommand(WorldAmazon.ACommands.newBuilder().addLoad(aPutOnTruck).setSimspeed(1000).build());

        //Send A TruckLoadedNotification
        UpsAmazon.AURequest.Builder auRequestLoaded = UpsAmazon.AURequest.newBuilder();
        auRequestLoaded.addLoaded(UpsAmazon.ATruckLoadedNotification.newBuilder().setTruckId(1).setSeqnum(999999L).build());
        aCommand.sendAURequest(auRequestLoaded.build());

        //Receive ACK =999999L
        for (Long ack : aResponse.recvFromUPS().getAcksList()) {
            System.out.println("[MAIN]      [TruckLoadedNotification ACK]: " + ack);
        }

        //TODO:
        //UTruckArrivedNotification made judgement in TruckStatusUpdate to avoid send multiple times
        //UGoDeliver should Made the same judgement as well in TruckStatusUpdate
        //WLR receive UGoDeliver ACK,,?

        try {
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
