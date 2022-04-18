package edu.duke.ece568.server;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

public class PostgreSQLJDBC {

    /*
    HOW TO USE POSTGRESQL
    1. CONNECT TO DATABASE USER
    psql ece568
    2. VIEW ALL TABLE
     \dt
    3.
     */

    //truck_id(long), status(int) is_sentutruckarrived(boolean)
    //status: 1:Idle 2:Traveling 3:ArriveWareHouse 4:Loading 5:Loaded 6:Delivering
    //MAIN TRACKING TABLE ON WORLD TRUCK
    private final String CREATE_TRUCK_TABLE = "CREATE TABLE IF NOT EXISTS TRUCK(" +
            "TRUCK_ID BIGINT PRIMARY KEY CHECK (TRUCK_ID >= 0), " +
            "TRUCK_X INT NOT NULL, " +
            "TRUCK_Y INT NOT NULL, " +
            "STATUS INT NOT NULL CHECK (STATUS < 7 AND STATUS > 0), " +
            "IS_SENTUTRUCKARRIVED BOOLEAN NOT NULL" +
            ");";
    //package_id(long), dest_x(int), dest_y(int), truck_id(long), warehouse_id(long), account_id(int), status(int)
    //status: 7:In Warehouse 8:Delivering 9:Delivered
    //MAIN TRACKING TABLE ON AMAZON REQUEST

    private final String CREATE_SHIPMENT_TABLE = "CREATE TABLE IF NOT EXISTS SHIPMENT(" +
            "PACKAGE_ID BIGINT PRIMARY KEY CHECK (PACKAGE_ID >= 0), " +
            "DEST_X INT NOT NULL, " +
            "DEST_Y INT NOT NULL, " +
            "TRUCK_ID BIGINT CHECK (TRUCK_ID >= 0), " +
            "WAREHOUSE_ID BIGINT NOT NULL, " +
            "EMAIL_ADDRESS TEXT NOT NULL, " +
            "STATUS INT NOT NULL CHECK (STATUS>6 AND STATUS < 10), " +
            "FOREIGN KEY (TRUCK_ID) REFERENCES TRUCK(TRUCK_ID) ON DELETE SET NULL," +
            "FOREIGN KEY (WAREHOUSE_ID) REFERENCES WAREHOUSE(WAREHOUSE_ID) ON DELETE SET NULL" +
            ");";
    private final String CREATE_WAREHOUSE_TABLE = "CREATE TABLE IF NOT EXISTS WAREHOUSE(" +
            "WAREHOUSE_ID BIGINT PRIMARY KEY CHECK (WAREHOUSE_ID >= 0 )," +
            "WAREHOUSE_X BIGINT NOT NULL," +
            "WAREHOUSE_Y BIGINT NOT NULL" +
            ");";
    private final String CREATE_AUTHENTICATION_TABLE = "CREATE EXTENSION IF NOT EXISTS pgcrypto;" +
            "CREATE TABLE IF NOT EXISTS AUTHENTICATION(" +
            "USERNAME TEXT PRIMARY KEY, " +
            "PASSWORD TEXT NOT NULL" +
            ");";
    //SELF USE TABLE TO WORLD RESEND
    private final String CREATE_UGOPICKUP_TABLE = "CREATE TABLE IF NOT EXISTS UGOPICKUP(" +
            "W_SEQNUM BIGINT PRIMARY KEY CHECK(W_SEQNUM >=0)," +
            "TRUCK_ID BIGINT NOT NULL" +
            ");";
    private final String CREATE_QUERY_TABLE = "CREATE TABLE IF NOT EXISTS UQUERY(" +
            "W_SEQNUM BIGINT PRIMARY KEY CHECK(W_SEQNUM >=0)," +
            "TRUCK_ID BIGINT NOT NULL" +
            ");";
    //SELF USE TABLE TO AMAZON RESEND
    private final String CREATE_USHIPMENTRESPONSE_TABLE = "CREATE TABLE IF NOT EXISTS USHIPMENTRESPONSE(" +
            "A_SEQNUM BIGINT PRIMARY KEY CHECK(A_SEQNUM >=0)," +
            "PACKAGE_ID BIGINT NOT NULL," +
            "FOREIGN KEY (PACKAGE_ID) REFERENCES SHIPMENT(PACKAGE_ID) ON DELETE CASCADE" +
            ");";
    private final String CREATE_USHIPPINGRESPONSE = "CREATE TABLE IF NOT EXISTS USHIPPINGRESPONSE(" +
            "A_SEQNUM BIGINT PRIMARY KEY CHECK(A_SEQNUM >=0)," +
            "TRUCK_ID BIGINT NOT NULL" +
            ");";
    private final String CREATE_UTRUCKARRIVEDNOTIFICATION = "CREATE TABLE IF NOT EXISTS UTRUCKARRIVEDNOTIFICATION(" +
            "A_SEQNUM BIGINT PRIMARY KEY CHECK(A_SEQNUM >=0)," +
            "TRUCK_ID BIGINT NOT NULL" +
            ");";
    //DROP ALL TABLE
    private final String DROP_ALL_TABLE = "DROP TABLE IF EXISTS TRUCK CASCADE; " +
            "DROP TABLE IF EXISTS SHIPMENT CASCADE; " +
            "DROP TABLE IF EXISTS WAREHOUSE CASCADE; " +
            "DROP TABLE IF EXISTS AUTHENTICATION; " +
            "DROP TABLE IF EXISTS UGOPICKUP; " +
            "DROP TABLE IF EXISTS UQUERY; " +
            "DROP TABLE IF EXISTS USHIPMENTRESPONSE; " +
            "DROP TABLE IF EXISTS USHIPPINGRESPONSE; " +
            "DROP TABLE IF EXISTS UTRUCKARRIVEDNOTIFICATION; ";
    private Connection c;
    private DatabaseMetaData databaseMetaData;

    public PostgreSQLJDBC() {
        try {
            Class.forName("org.postgresql.Driver");
            this.c = DriverManager.getConnection("jdbc:postgresql://localhost:5432/ece568", "ece568", "ece568");
            this.databaseMetaData = c.getMetaData();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Execute SQL Command in query
     *
     * @param query command
     */
    private void executeStatement(String query) {
        try {
            Statement statement = this.c.createStatement();
            statement.executeUpdate(query);
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Check if key of value id exist in table
     *
     * @param table The table to check
     * @param key   The entry to check
     * @param id    The entry value
     * @return boolean denotes whether exist or not
     */
    private boolean isPrimaryKeyExist(String table, String key, Long id) {
        String query = "SELECT * FROM " + table + " WHERE " + key + " = " + id + "; ";
        Statement statement = null;
        try {
            statement = this.c.createStatement();
            ResultSet resultSet = statement.executeQuery(query);
            if (resultSet.next() == false) {
                System.out.println(key + ": " + id + "does not exist!");
                resultSet.close();
                statement.close();
                return false;
            } else return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Get Long Entry Value From Table Key = ID Item
     *
     * @param table The SQL Table
     * @param key   The entry key
     * @param id    The key value
     * @param entry the data entry name
     * @return -1 if wrong
     */
    private Long getLongFromTable(String table, String key, Long id, String entry) {
        String query = "SELECT " + entry + " FROM " + table + " WHERE " + key + " = " + id + ";";
        Long ans = -1L;
        try {
            Statement statement = c.createStatement();
            ResultSet resultSet = statement.executeQuery(query);
            if (resultSet.next()) {
                ans = resultSet.getLong(entry);
            } else {
                System.out.println(key + ": id " + entry + " does not exist!");
            }
            resultSet.close();
            statement.close();
            return ans;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ans;
    }

    /**
     * Get Int Entry Value From Table Key = ID Item
     *
     * @param table The SQL Table
     * @param key   The entry key
     * @param id    The key value
     * @param entry the data entry name
     * @return -1 if wrong
     */
    private Integer getIntFromTable(String table, String key, Long id, String entry) {
        String query = "SELECT " + entry + " FROM " + table + " WHERE " + key + " = " + id + ";";
        Integer ans = -1;
        try {
            Statement statement = c.createStatement();
            ResultSet resultSet = statement.executeQuery(query);
            if (resultSet.next()) {
                ans = resultSet.getInt(entry);
            } else {
                System.out.println(key + ": id " + entry + " does not exist!");
            }
            resultSet.close();
            statement.close();
            return ans;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ans;
    }

    public void createAllTable() {
        executeStatement(this.CREATE_TRUCK_TABLE);
        executeStatement(this.CREATE_WAREHOUSE_TABLE);
        executeStatement(this.CREATE_SHIPMENT_TABLE);
        executeStatement(this.CREATE_QUERY_TABLE);
        executeStatement(this.CREATE_USHIPMENTRESPONSE_TABLE);
        executeStatement(this.CREATE_AUTHENTICATION_TABLE);
        executeStatement(this.CREATE_UGOPICKUP_TABLE);
        executeStatement(this.CREATE_USHIPPINGRESPONSE);
        executeStatement(this.CREATE_UTRUCKARRIVEDNOTIFICATION);
    }

    public void dropAllTable() {
        executeStatement(this.DROP_ALL_TABLE);
    }

    public Boolean isTableExist(String tableName) throws SQLException {
        ResultSet resultSet = this.databaseMetaData.getTables(null, null, tableName, new String[]{"TABLE"});
        return resultSet.next();
    }

    /******************************************************************************************************************
     * TRUCK METHOD
     ******************************************************************************************************************/
    public void addTruck(Long truckID, Integer truckX, Integer truckY, Integer status, Boolean isSentUTruckArrived) {
        String query = "INSERT INTO TRUCK(TRUCK_ID, TRUCK_X, TRUCK_Y, STATUS, IS_SENTUTRUCKARRIVED) VALUES (" +
                truckID + "," +
                truckX + "," +
                truckY + "," +
                status + "," +
                isSentUTruckArrived +
                ");";
        executeStatement(query);
    }

    public void updateTruckStatus(Long truckID, Integer setTruckX, Integer setTruckY, Integer setStatus, Boolean setIsSentUTruckArrived) {
        if (truckID == null) {
            System.out.println("TruckId should not be NULL!");
            return;
        }
        if (!isPrimaryKeyExist("TRUCK", "TRUCK_ID", truckID)) return;
        String query;
        if (setTruckX != null && setTruckY != null) {
            query = "UPDATE TRUCK SET TRUCK_X = " + setTruckX + "," + "TRUCK_Y = " + setTruckY + " WHERE TRUCK_ID = " + truckID + ";";
            executeStatement(query);
        }
        if (setStatus != null) {
            query = "UPDATE TRUCK SET STATUS = " + setStatus + " WHERE TRUCK_ID = " + truckID + ";";
            executeStatement(query);
        }
        if (setIsSentUTruckArrived != null) {
            query = "UPDATE TRUCK SET IS_SENTUTRUCKARRIVED = " + setIsSentUTruckArrived + " WHERE TRUCK_ID = " + truckID + ";";
            executeStatement(query);
        }
    }
    public Integer getTruckX(Long truckID){
        return getIntFromTable("TRUCK", "TRUCK_ID", truckID, "TRUCK_X");
    }
    public Integer getTruckY(Long truckID){
        return getIntFromTable("TRUCK", "TRUCK_ID", truckID, "TRUCK_Y");
    }
    public Integer getTruckStatus(Long truckID){
        return getIntFromTable("TRUCK", "TRUCK_ID", truckID, "STATUS");
    }

    public HashMap<Long, ArrayList<Integer>> getTruckGroupOfStatus(Integer status) {
        HashMap<Long, ArrayList<Integer>> truckHashMap = new HashMap<>();
        String query = "SELECT * FROM TRUCK WHERE STATUS=" + status+";";
        try {
            Statement stmt = c.createStatement();
            ResultSet resultSet = stmt.executeQuery(query);
            while (resultSet.next()) {
                ArrayList<Integer> pos = new ArrayList<>();
                pos.add(resultSet.getInt("TRUCK_X"));
                pos.add(resultSet.getInt("TRUCK_Y"));
                truckHashMap.put(resultSet.getLong("TRUCK_ID"), pos);
            }
            resultSet.close();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return truckHashMap;
    }



    /******************************************************************************************************************
     * SHIPMENT METHOD
     ******************************************************************************************************************/
    public void addShipment(Long packageID, Integer X, Integer Y, Long truckID, Long warehouseID, String emailAddress, Integer status) {
        String query = "INSERT INTO SHIPMENT(PACKAGE_ID, DEST_X, DEST_Y, TRUCK_ID, WAREHOUSE_ID,EMAIL_ADDRESS,STATUS) VALUES (" +
                packageID + "," +
                X + "," +
                Y + "," +
                truckID + "," +
                warehouseID + "," +
                packageID + "," +
                emailAddress + "," +
                status +
                ");";
        executeStatement(query);
    }

    public void updateShipmentStatus(Long packageID, Integer status) {
        if (packageID == null) {
            System.out.println("PackageID should not be NULL!");
            return;
        }
        String query;
        query = "SELECT * FROM SHIPMENT WHERE PACKAGE_ID =" + packageID + "; ";
        try {
            Statement statement = c.createStatement();
            ResultSet resultSet = statement.executeQuery(query);
            if (resultSet.next() == false) {
                System.out.println("Shipment id does not exist!");
                statement.close();
                resultSet.close();
                return;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        query = "UPDATE SHIPMENT SET STATUS = " + status + " WHERE PACKAGE_ID = " + packageID + ";";
    }


    public void close() {
        try {
            this.c.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
