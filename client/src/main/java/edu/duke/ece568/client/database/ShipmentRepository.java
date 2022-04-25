package edu.duke.ece568.client.database;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    @Query("select s from Shipment s where s.tracking_num = :tracking_num")
    Shipment findPackageByTrackingNum(@Param("tracking_num") String tracking_num);

    @Transactional
    @Modifying
    @Query("update Shipment s set s.x= :x_dest where s.package_id = :id")
    void setShipment_x(@Param("x_dest") Integer x_dest, @Param("id") long id);

    @Transactional
    @Modifying
    @Query("update Shipment s set s.y= :y_dest where s.package_id = :id")
    void setShipment_y(@Param("y_dest") Integer y_dest, @Param("id") long id);
}
