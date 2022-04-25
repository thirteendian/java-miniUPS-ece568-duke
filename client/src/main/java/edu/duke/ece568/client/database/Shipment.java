package edu.duke.ece568.client.database;


import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "shipment")
public class Shipment {
    @Id@Column(insertable = false,updatable = false)
    private Long package_id;
    @Column
    private int x;
    @Column
    private int y;
    @Column(insertable = false,updatable = false)
    private int status;
    @Column(insertable = false,updatable = false)
    private String tracking_num;

    public Long getPackage_id() {
        return package_id;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getStatus() {
        return status;
    }

    public String getTracking_num() {
        return tracking_num;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Shipment shipment = (Shipment) o;
        return x == shipment.x && y == shipment.y && status == shipment.status && Objects.equals(package_id, shipment.package_id) && Objects.equals(tracking_num, shipment.tracking_num);
    }

    @Override
    public int hashCode() {
        return Objects.hash(package_id, x, y, status, tracking_num);
    }

    @Override
    public String toString() {
        return "Shipment{" +
                "package_id=" + package_id +
                ", x=" + x +
                ", y=" + y +
                ", status=" + status +
                ", tracking_num='" + tracking_num + '\'' +
                '}';
    }
}
