package edu.duke.ece568.server;

import edu.duke.ece568.server.protocol.UpsAmazon;
import edu.duke.ece568.server.protocol.WorldAmazon;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class ServerTest {
    final String WORLD_HOST = "192.168.1.8";
    final Integer WORLD_PORTNUM = 12345;
    final Integer WORLD_AMAZON_PORTNUM = 23456;

    final Integer CLIENT_PORTNUM = 34487;
    final Integer ACLIENT_PORTNUM = 34488;

    final String AMAZON_HOST = "127.0.0.1";
    final Integer AMAZON_PORTNUM = 11111;

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
    public void test_All() throws IOException {
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

        WorldAmazon.ACommands.Builder aCommandPurchase = WorldAmazon.ACommands.newBuilder();
        WorldAmazon.APurchaseMore.Builder aPurchaseMore = WorldAmazon.APurchaseMore.newBuilder();
        WorldAmazon.AProduct.Builder aProduct = WorldAmazon.AProduct.newBuilder();
        aProduct.setId(440L).setCount(14).setDescription("Descriotion");
        aPurchaseMore.setSeqnum(888888L).setWhnum(1).addThings(aProduct.build());
        aCommandPurchase.addBuy(aPurchaseMore.build());
        aCommand.sendACommand(aCommandPurchase.setSimspeed(500).build());
        System.out.println("[MAIN]     AMAZON BUY");

        while (true) {
            WorldAmazon.AResponses.Builder aResponses = aResponse.recvFromWorld();
            if (aResponses.getArrivedList().size() != 0) {
                System.out.println("[MAIN]      [AMAZON Arrived] aSeqNum: " + aResponses.getArrived(0).getSeqnum() + " ACK: " + aResponses.getAcks(0));
                break;
            }
            aCommand.sendACommand(aCommandPurchase.build());
        }

        WorldAmazon.APack.Builder aPack = WorldAmazon.APack.newBuilder();
        aPack.setWhnum(1).setSeqnum(888889L).setShipid(1L).addThings(aProduct.build());
        WorldAmazon.ACommands.Builder aCommandsPack = WorldAmazon.ACommands.newBuilder().addTopack(aPack.build());
        aCommand.sendACommand(aCommandsPack.setSimspeed(500).build());
        System.out.println("[MAIN]     AMAZON PACK");

        while (true) {
            WorldAmazon.AResponses.Builder aResponses = aResponse.recvFromWorld();
            if (aResponses.getReadyList().size() != 0) {
                System.out.println("[MAIN]      [AMAZON Pack] aSeqNum: " + aResponses.getReady(0).getSeqnum() + " ACK: " + aResponses.getAcks(0));
                break;
            }
            aCommand.sendACommand(aCommandsPack.setSimspeed(500).build());
        }


        //AShippingRequest
        UpsAmazon.AShippingRequest.Builder aShippingRequest = UpsAmazon.AShippingRequest.newBuilder();
        aShippingRequest.setSeqnum(1).setLocation(UpsAmazon.AWareHouseLocation.newBuilder().setX(1).setY(1).setWarehouseid(1).build()).addShipment(UpsAmazon.AShipment.newBuilder().setDestX(2).setDestY(2).setEmailaddress("@duke.edu").setPackageId(1).addProduct(UpsAmazon.Product.newBuilder().setDescription("Descriotion").setCount(10).build()).build());
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


        //Send Load to World
        WorldAmazon.APutOnTruck.Builder aPutOnTruck = WorldAmazon.APutOnTruck.newBuilder().setWhnum(1).setTruckid(1).setShipid(1).setSeqnum(444444L);
        WorldAmazon.ACommands.Builder responsesLoad = WorldAmazon.ACommands.newBuilder().addLoad(aPutOnTruck);
        aCommand.sendACommand(responsesLoad.build());

        while (true) {
            WorldAmazon.AResponses.Builder aResponses = aResponse.recvFromWorld();
            if (aResponses.getLoadedList().size() != 0) {
                System.out.println("[MAIN]      [AMAZON loaded] aSeqNum: " + aResponses.getLoaded(0).getSeqnum());
                break;
            }
        }


        //Send A TruckLoadedNotification
        UpsAmazon.AURequest.Builder auRequestLoaded = UpsAmazon.AURequest.newBuilder();
        auRequestLoaded.addLoaded(UpsAmazon.ATruckLoadedNotification.newBuilder().setTruckId(1).setSeqnum(999999L).build());
        aCommand.sendAURequest(auRequestLoaded.build());


        //Receive ACK =999999L
        for (Long ack : aResponse.recvFromUPS().getAcksList()) {
            System.out.println("[MAIN]      receive [TruckLoadedNotification ACK]: " + ack);
        }

        while (true) {
            UpsAmazon.AUResponse.Builder auResponse = aResponse.recvFromUPS();
            Boolean flag = false;
            if (auResponse.getAcksList().size() != 0) {
                for (Long ack : auResponse.getAcksList()) {
                    if (ack == 999999L) {
                        flag= true;
                        System.out.println("[MAIN]      receive [TruckLoadedNotification]  ACK: " + ack);
                        break;
                    }
                }
            }
            if(flag) break;
        }


        try {
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
    @Test
    public void test_real_server() throws IOException {
        //Please Run This First
        //Then Run the Real Server
        //=====================================Create Socket==================================================//
        //Connect to World Connect to AMAZON
        AmazonServer amazonServer = new AmazonServer(WORLD_HOST, WORLD_AMAZON_PORTNUM);
        amazonServer.acceptConnection();
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

        WorldAmazon.ACommands.Builder aCommandPurchase = WorldAmazon.ACommands.newBuilder();
        WorldAmazon.APurchaseMore.Builder aPurchaseMore = WorldAmazon.APurchaseMore.newBuilder();
        WorldAmazon.AProduct.Builder aProduct = WorldAmazon.AProduct.newBuilder();
        aProduct.setId(440L).setCount(14).setDescription("Descriotion");
        aPurchaseMore.setSeqnum(888888L).setWhnum(1).addThings(aProduct.build());
        aCommandPurchase.addBuy(aPurchaseMore.build());
        aCommand.sendACommand(aCommandPurchase.setSimspeed(500).build());
        System.out.println("[MAIN]     AMAZON BUY");

        while (true) {
            WorldAmazon.AResponses.Builder aResponses = aResponse.recvFromWorld();
            if (aResponses.getArrivedList().size() != 0) {
                System.out.println("[MAIN]      [AMAZON Arrived] aSeqNum: " + aResponses.getArrived(0).getSeqnum() + " ACK: " + aResponses.getAcks(0));
                break;
            }
            aCommand.sendACommand(aCommandPurchase.build());
        }

        WorldAmazon.APack.Builder aPack = WorldAmazon.APack.newBuilder();
        aPack.setWhnum(1).setSeqnum(888889L).setShipid(1L).addThings(aProduct.build());
        WorldAmazon.ACommands.Builder aCommandsPack = WorldAmazon.ACommands.newBuilder().addTopack(aPack.build());
        aCommand.sendACommand(aCommandsPack.setSimspeed(500).build());
        System.out.println("[MAIN]     AMAZON PACK");

        while (true) {
            WorldAmazon.AResponses.Builder aResponses = aResponse.recvFromWorld();
            if (aResponses.getReadyList().size() != 0) {
                System.out.println("[MAIN]      [AMAZON Pack] aSeqNum: " + aResponses.getReady(0).getSeqnum() + " ACK: " + aResponses.getAcks(0));
                break;
            }
            aCommand.sendACommand(aCommandsPack.setSimspeed(500).build());
        }


        //AShippingRequest
        UpsAmazon.AShippingRequest.Builder aShippingRequest = UpsAmazon.AShippingRequest.newBuilder();
        aShippingRequest.setSeqnum(1).setLocation(UpsAmazon.AWareHouseLocation.newBuilder().setX(1).setY(1).setWarehouseid(1).build()).addShipment(UpsAmazon.AShipment.newBuilder().setDestX(2).setDestY(2).setEmailaddress("@duke.edu").setPackageId(1).addProduct(UpsAmazon.Product.newBuilder().setDescription("Descriotion").setCount(10).build()).build());
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


        //Send Load to World
        WorldAmazon.APutOnTruck.Builder aPutOnTruck = WorldAmazon.APutOnTruck.newBuilder().setWhnum(1).setTruckid(1).setShipid(1).setSeqnum(444444L);
        WorldAmazon.ACommands.Builder responsesLoad = WorldAmazon.ACommands.newBuilder().addLoad(aPutOnTruck);
        aCommand.sendACommand(responsesLoad.build());

        while (true) {
            WorldAmazon.AResponses.Builder aResponses = aResponse.recvFromWorld();
            if (aResponses.getLoadedList().size() != 0) {
                System.out.println("[MAIN]      [AMAZON loaded] aSeqNum: " + aResponses.getLoaded(0).getSeqnum());
                break;
            }
        }


        //Send A TruckLoadedNotification
        UpsAmazon.AURequest.Builder auRequestLoaded = UpsAmazon.AURequest.newBuilder();
        auRequestLoaded.addLoaded(UpsAmazon.ATruckLoadedNotification.newBuilder().setTruckId(1).setSeqnum(999999L).build());
        aCommand.sendAURequest(auRequestLoaded.build());


        //Receive ACK =999999L
        for (Long ack : aResponse.recvFromUPS().getAcksList()) {
            System.out.println("[MAIN]      receive [TruckLoadedNotification ACK]: " + ack);
        }

        while (true) {
            UpsAmazon.AUResponse.Builder auResponse = aResponse.recvFromUPS();
            Boolean flag = false;
            if (auResponse.getAcksList().size() != 0) {
                for (Long ack : auResponse.getAcksList()) {
                    if (ack == 999999L) {
                        flag= true;
                        System.out.println("[MAIN]      receive [TruckLoadedNotification]  ACK: " + ack);
                        break;
                    }
                }
            }
            if(flag) break;
        }


        try {
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
