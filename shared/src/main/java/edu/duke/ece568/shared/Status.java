package edu.duke.ece568.shared;

public class Status {
    //Truck Status
    public final Integer tIdel = 1;
    public final Integer tTraveling = 2;
    public final Integer tArriveWarehouse = 3;
    public final Integer tLoading = 4;
    public final Integer tLoaded = 5;
    public final Integer tDelivering = 6;
    //Package Status
    public final Integer pInWarehouse = 7;
    public final Integer pDelivering = 8;
    public final Integer pDelivered = 9;
    public Integer getStatus(String status){
        if(status.equals("IDLE")) return tIdel;
        else if(status.equals("TRAVELLING")) return tTraveling;
        else if(status.equals("ARRIVE WAREHOUSE")) return tArriveWarehouse;
        else if(status.equals("LOADING")) return tLoading;
        else if(status.equals("DELIVERING"))return  tDelivering;
        else return -1;
    }
    public String getStatus(Integer status){
        if(status.equals(1))return  "IDLE";
        else if (status.equals(2))return  "TRAVELLING";
        else if (status.equals(3))return  "ARRIVE WAREHOUSE";
        else if (status.equals(4))return  "LOADING";
        else if (status.equals(5))return  "LOADED";
        else if (status.equals(6))return  "DELIVERING";
        else if (status.equals(7))return  "IN WAREHOUSE";
        else if (status.equals(8))return  "DELIVERING";
        else if (status.equals(9))return  "DELIVERED";
        else  return "ERROR";
    }

}
