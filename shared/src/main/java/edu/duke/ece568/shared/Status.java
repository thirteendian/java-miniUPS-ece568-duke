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
        if(status == "idle") return tIdel;
        else if(status == "traveling") return tTraveling;
        else if(status == "arrive warehouse") return tArriveWarehouse;
        else if(status == "loading") return tLoading;
        else if(status == "delivering")return  tDelivering;
        else return -1;
    }
}
