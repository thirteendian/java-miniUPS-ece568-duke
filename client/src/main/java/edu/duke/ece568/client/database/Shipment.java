package edu.duke.ece568.client.database;


import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.ArrayList;

@Entity
@Table(name = "shipment")
public class Shipment {
    @Id@Column(insertable = false,updatable = false)
    private Long package_id;
    @Column(insertable = false,updatable = false)
    private int x;
    @Column(insertable = false,updatable = false)
    private int y;
    @Column(insertable = false,updatable = false)
    private int status;
    @Column(insertable = false,updatable = false)
    private String tracking_num;

}
