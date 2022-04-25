package edu.duke.ece568.server;

import java.sql.*;
import java.util.ArrayList;

public class PostgreSQLJDBC {

    private final String _truck = "TRUCK";
    private final String _truckId = "TRUCK_ID";
    private final String _x = "X";
    private final String _y = "Y";
    private final String _status = "STATUS";
    private final String _isSentUtruckArrived = "IS_SENTUTRUCKARRIVED";
    private final String _isSentUGoDeliver = "IS_SENTUGODELIVER";
    private final String _shipment = "SHIPMENT";
    private final String _warehouse = "WAREHOUSE";
    private final String _authentication = "AUTHENTICATION";
    private final String _uGoPickup = "UGOPICKUP";
    private final String _uGoDeliver = "UGODELIVER";
    private final String _uQuery = "UQUERY";
    private final String _uShipmentStatusUpdate = "USHIPMENTSTATUSUPDATE";
    private final String _uShippingResponse = "USHIPPINGRESPONSE";
    private final String _uTruckArrivedNotification = "UTRUCKARRIVEDNOTIFICATION";
    private final String _packageId = "PACKAGE_ID";
    private final String _warehouseId = "WAREHOUSE_ID";
    private final String _emailAddress = "EMAIL_ADDRESS";
    private final String _username = "USERNAME";
    private final String _password = "PASSWORD";
    private final String _wSeqNum = "W_SEQNUM";
    private final String _aSeqNum = "A_SEQNUM";
    private final String _shippingRequest = "SHIPPINGREQUEST";
    private final String _shippingId = "SHIPPINGID";
    private final String _aushipmentupdate = "AUSHIPMENTUPDATE";
    private final String _product = "PRODUCT";
    private final String _productId = "PRODUCT_ID";
    private final String _description = "DESCRIPTION";
    private final String _count = "COUNT";
    private final String _trackingNum = "TRACKING_NUM";
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
            "X INT NOT NULL, " +
            "Y INT NOT NULL, " +
            "STATUS INT NOT NULL CHECK (STATUS < 7 AND STATUS > 0), " +
            "IS_SENTUTRUCKARRIVED BOOLEAN NOT NULL," +
            "IS_SENTUGODELIVER BOOLEAN NOT NULL" +
            ");";

    //package_id(long), dest_x(int), dest_y(int), truck_id(long), warehouse_id(long), account_id(int), status(int)
    //status: 7:In Warehouse 8:Delivering 9:Delivered
    //MAIN TRACKING TABLE ON AMAZON REQUEST

    private final String CREATE_SHIPMENT_TABLE = "CREATE TABLE IF NOT EXISTS SHIPMENT(" +
            "PACKAGE_ID BIGINT PRIMARY KEY CHECK (PACKAGE_ID >= 0), " +
            "X BIGINT NOT NULL, " +
            "Y BIGINT NOT NULL, " +
            "TRUCK_ID BIGINT CHECK (TRUCK_ID >= 0), " +
            "WAREHOUSE_ID BIGINT NOT NULL, " +
            "EMAIL_ADDRESS TEXT NOT NULL, " +
            "STATUS INT NOT NULL CHECK (STATUS>6 AND STATUS < 10), " +
            "SHIPPINGID BIGINT NOT NULL CHECK (SHIPPINGID >= 0)," +
            "TRACKING_NUM TEXT NOT NULL," +
            "FOREIGN KEY (WAREHOUSE_ID) REFERENCES WAREHOUSE(WAREHOUSE_ID) ON DELETE SET NULL" +
            ");";

    private final String CREATE_WAREHOUSE_TABLE = "CREATE TABLE IF NOT EXISTS WAREHOUSE(" +
            "WAREHOUSE_ID BIGINT PRIMARY KEY CHECK (WAREHOUSE_ID >= 0 )," +
            "X BIGINT NOT NULL," +
            "Y BIGINT NOT NULL" +
            ");";
    private final String CREATE_AUTHENTICATION_TABLE =
            "CREATE TABLE IF NOT EXISTS AUTHENTICATION(" +
                    "USERNAME TEXT PRIMARY KEY, " +
                    "PASSWORD TEXT NOT NULL" +
                    ");";

    //SELF USE TABLE TO WORLD RESEND
    private final String CREATE_UGOPICKUP_TABLE = "CREATE TABLE IF NOT EXISTS UGOPICKUP(" +
            "W_SEQNUM BIGINT PRIMARY KEY CHECK(W_SEQNUM >=0)," +
            "TRUCK_ID BIGINT NOT NULL," +
            "A_SEQNUM BIGINT NOT NULL," +
            "WAREHOUSE_ID BIGINT NOT NULL" +
            ");";
    private final String CREATE_QUERY_TABLE = "CREATE TABLE IF NOT EXISTS UQUERY(" +
            "W_SEQNUM BIGINT PRIMARY KEY CHECK(W_SEQNUM >=0)," +
            "TRUCK_ID BIGINT NOT NULL" +
            ");";
    private final String CREATE_UGODELIVER_TABLE = "CREATE TABLE IF NOT EXISTS UGODELIVER(" +
            "W_SEQNUM BIGINT PRIMARY KEY CHECK(W_SEQNUM >=0)," +
            "TRUCK_ID BIGINT NOT NULL" +
            ");";
    //SELF USE TABLE TO AMAZON RESEND
    private final String CREATE_USHIPMENTSTATUSUPDATE_TABLE = "CREATE TABLE IF NOT EXISTS USHIPMENTSTATUSUPDATE(" +
            "A_SEQNUM BIGINT PRIMARY KEY CHECK(A_SEQNUM >=0)" +
            ");";
    private final String CREATE_USHIPPINGRESPONSE = "CREATE TABLE IF NOT EXISTS USHIPPINGRESPONSE(" +
            "A_SEQNUM BIGINT PRIMARY KEY CHECK(A_SEQNUM >=0)," +
            "TRUCK_ID BIGINT NOT NULL" +
            ");";
    private final String CREATE_UTRUCKARRIVEDNOTIFICATION = "CREATE TABLE IF NOT EXISTS UTRUCKARRIVEDNOTIFICATION(" +
            "A_SEQNUM BIGINT PRIMARY KEY CHECK(A_SEQNUM >=0)," +
            "TRUCK_ID BIGINT NOT NULL" +
            ");";

    private final String CREATE_SHIPPING_REQUEST_TABLE = "CREATE TABLE IF NOT EXISTS SHIPPINGREQUEST(" +
            "SHIPPINGID BIGINT PRIMARY KEY CHECK(SHIPPINGID >=0)," +
            "A_SEQNUM BIGINT NOT NULL" +
            ");";
    private final String CREATE_AUSHIPMENTUPDATE = "CREATE TABLE IF NOT EXISTS AUSHIPMENTUPDATE(" +
            "PACKAGE_ID BIGINT PRIMARY KEY CHECK (PACKAGE_ID >=0)," +
            "STATUS INT NOT NULL , " +
            "A_SEQNUM BIGINT NOT NULL "+
            ");";
    private final String CREATE_PRODUCT  = "CREATE TABLE IF NOT EXISTS PRODUCT(" +
            "PRODUCT_ID BIGSERIAL PRIMARY KEY," +
            "PACKAGE_ID BIGINT NOT NULL," +
            "DESCRIPTION TEXT NOT NULL," +
            "COUNT BIGINT NOT NULL" +
            ");";

    //DROP ALL TABLE
    private final String DROP_ALL_TABLE = "DROP TABLE IF EXISTS TRUCK CASCADE; " +
            "DROP TABLE IF EXISTS SHIPMENT CASCADE; " +
            "DROP TABLE IF EXISTS WAREHOUSE CASCADE; " +
            "DROP TABLE IF EXISTS AUTHENTICATION; " +
            "DROP TABLE IF EXISTS UGOPICKUP; " +
            "DROP TABLE IF EXISTS UGODELIVER;" +
            "DROP TABLE IF EXISTS UQUERY; " +
            "DROP TABLE IF EXISTS USHIPMENTSTATUSUPDATE; " +
            "DROP TABLE IF EXISTS USHIPPINGRESPONSE; " +
            "DROP TABLE IF EXISTS UTRUCKARRIVEDNOTIFICATION; " +
            "DROP TABLE IF EXISTS SHIPPINGREQUEST;" +
            "DROP TABLE IF EXISTS AUSHIPMENTUPDATE;" +
            "DROP TABLE IF EXISTS PRODUCT;";
    private Connection c;
    private DatabaseMetaData databaseMetaData;

    public PostgreSQLJDBC() {
        try {
            Class.forName("org.postgresql.Driver");
            this.c = DriverManager.getConnection("jdbc:postgresql://localhost:5432/ece568", "ece568", "ece568");
            this.c.setAutoCommit(false);
            this.databaseMetaData = c.getMetaData();
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Drop All Table That Exist
     * Note that this method should be called only once when start server
     */
    public void dropAllTable() {
        executeStatement(this.DROP_ALL_TABLE);
        try {
            this.c.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Create All Table That Needed
     * Note that this method should be called only once when start server
     */
    public void createAllTable() {
        executeStatement(this.CREATE_TRUCK_TABLE);
        executeStatement(this.CREATE_WAREHOUSE_TABLE);
        executeStatement(this.CREATE_SHIPMENT_TABLE);
        executeStatement(this.CREATE_QUERY_TABLE);
        executeStatement(this.CREATE_USHIPMENTSTATUSUPDATE_TABLE);
        executeStatement(this.CREATE_AUTHENTICATION_TABLE);
        executeStatement(this.CREATE_UGOPICKUP_TABLE);
        executeStatement(this.CREATE_UGODELIVER_TABLE);
        executeStatement(this.CREATE_USHIPPINGRESPONSE);
        executeStatement(this.CREATE_UTRUCKARRIVEDNOTIFICATION);
        executeStatement(this.CREATE_SHIPPING_REQUEST_TABLE);
        executeStatement(this.CREATE_AUSHIPMENTUPDATE);
        executeStatement(this.CREATE_PRODUCT);
        try {
            this.c.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Execute SQL Command in query
     * Note that COMMIT IS NEEDED AFTER THIS METHOD
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
//                System.out.println(key + ": " + id + "does not exist!");
                resultSet.close();
                statement.close();
                return false;
            } else return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean isPrimaryKeyExist(String table, String key, String id) {
        String query = "SELECT * FROM " + table + " WHERE " + key + " = " + id + "; ";
        Statement statement = null;
        try {
            statement = this.c.createStatement();
            ResultSet resultSet = statement.executeQuery(query);
            if (resultSet.next() == false) {
//                System.out.println(key + ": " + id + "does not exist!");
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
        Long ans = null;
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
     * Get Boolean Entry Value From Table Key = ID Item
     *
     * @param table The SQL Table
     * @param key   The entry key
     * @param id    The key value
     * @param entry the data entry name
     * @return -1 if wrong
     */
    private Boolean getBooleanFromTable(String table, String key, Long id, String entry) {
        String query = "SELECT " + entry + " FROM " + table + " WHERE " + key + " = " + id + ";";
        Boolean ans = false;
        try {
            Statement statement = c.createStatement();
            ResultSet resultSet = statement.executeQuery(query);
            if (resultSet.next()) {
                ans = resultSet.getBoolean(entry);
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
        Integer ans = null;
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

    /**
     * Get Int Entry Value From Table Key = ID Item
     *
     * @param table The SQL Table
     * @param key   The entry key
     * @param id    The key value
     * @param entry the data entry name
     * @return -1 if wrong
     */
    private String getStringFromTable(String table, String key, Long id, String entry) {
        String query = "SELECT " + entry + " FROM " + table + " WHERE " + key + " = " + id + ";";
        String ans = null;
        try {
            Statement statement = c.createStatement();
            ResultSet resultSet = statement.executeQuery(query);
            if (resultSet.next()) {
                ans = resultSet.getString(entry);
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

    private String getStringFromTable(String table, String key, String id, String entry) {
        String query = "SELECT " + entry + " FROM " + table + " WHERE " + key + " = " + id + ";";
        String ans = null;
        try {
            Statement statement = c.createStatement();
            ResultSet resultSet = statement.executeQuery(query);
            if (resultSet.next()) {
                ans = resultSet.getString(entry);
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
     * Update Table Entry of Key=id with newEntryValue
     * Note that COMMIT IS NEEDED AFTER THIS METHOD
     *
     * @param table         The table to update
     * @param key           The Primary Key
     * @param id            The Primary Key Value
     * @param entry         The Entry to Update
     * @param newEntryValue The New Entry Value
     */
    private void updateEntry(String table, String key, Long id, String entry, Integer newEntryValue) {
        String query = "Update " + table + " SET " + entry + " = " + newEntryValue + " WHERE " + key + " = " + id + ";";
        executeStatement(query);
    }

    private void updateEntry(String table, String key, Long id, String entry, String newEntryValue) {
        String query = "Update " + table + " SET " + entry + " = \"" + newEntryValue + "\" WHERE " + key + " = " + id + ";";
        executeStatement(query);
    }

    private void updateEntry(String table, String key, Long id, String entry, Long newEntryValue) {
        String query = "Update " + table + " SET " + entry + " = " + newEntryValue + " WHERE " + key + " = " + id + ";";
        executeStatement(query);
    }

    private void updateEntry(String table, String key, Long id, String entry, Boolean newEntryValue) {
        String query = "Update " + table + " SET " + entry + " = " + newEntryValue + " WHERE " + key + " = " + id + ";";
        executeStatement(query);
    }

    /**
     * Delete Whole entry of table when key=id
     * Note: COMMIT IS NEEDED AFTER THIS METHOD
     *
     * @param table The table
     * @param key   Primary Key
     * @param id    Primary Key value
     */
    private void deleteEntry(String table, String key, Long id) {
        String query = "DELETE FROM " + table + " WHERE " + key + " = " + id + ";";
        executeStatement(query);
    }

    /**
     * Add Entry for resent usage
     * NOte: COMMIT IS NEEDED AFTER THIS METHOD
     *
     * @param table      the table
     * @param key        primary key
     * @param seq        primary key value(should be sequence number)
     * @param entry      entry name
     * @param entryValue entry value
     */
    private void addResentEntry(String table, String key, Long seq, String entry, Long entryValue) {
        String query = "INSERT INTO " + table + "(" + key + "," + entry + ") VALUES (" + seq + "," + entryValue + ");";
        executeStatement(query);
    }

    /**
     * Get Table Size
     *
     * @param table
     * @return
     */
    private Integer getTableSize(String table) {
        Integer size = 0;
        String query = "SELECT COUNT(*) AS total FROM " + table;
        try {
            Statement statement = c.createStatement();
            ResultSet resultSet = statement.executeQuery(query);
            if (resultSet.next()) {
                size = resultSet.getInt("total");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return size;
    }

    /******************************************************************************************************************
     * TRUCK METHOD
     ******************************************************************************************************************/

    public void addTruck(Long truckID, Integer truckX, Integer truckY, Integer status, Boolean isSentUTruckArrived, Boolean isSentUGoDeliver) {
        String query = "INSERT INTO " + _truck + "(" + _truckId + ", " + _x + ", " + _y + ", " + _status + ", " + _isSentUtruckArrived + ", "+ _isSentUGoDeliver+") VALUES (" +
                truckID + "," +
                truckX + "," +
                truckY + "," +
                status + "," +
                isSentUTruckArrived +","+
                isSentUGoDeliver+
                ");";
        executeStatement(query);
        try {
            this.c.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateTruckStatus(Long truckID, Integer setTruckX, Integer setTruckY, Integer setStatus, Boolean setIsSentUTruckArrived, Boolean setIsSentUGoDeliver) {
        if (!isPrimaryKeyExist(_truck, _truckId, truckID)) return;
        String query;
        if (setTruckX != null && setTruckY != null) {
            updateEntry(_truck, _truckId, truckID, _x, setTruckX);
            updateEntry(_truck, _truckId, truckID, _y, setTruckY);
        }
        if (setStatus != null) {
            updateEntry(_truck, _truckId, truckID, _status, setStatus);
        }
        if (setIsSentUTruckArrived != null) {
            updateEntry(_truck, _truckId, truckID, _isSentUtruckArrived, setIsSentUTruckArrived);
        }
        if (setIsSentUGoDeliver != null){
            updateEntry(_truck,_truckId,truckID, _isSentUGoDeliver, setIsSentUGoDeliver);
        }

        try {
            this.c.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Integer getTruckX(Long truckID) {
        return getIntFromTable(_truck, _truckId, truckID, _x);
    }

    public Integer getTruckY(Long truckID) {
        return getIntFromTable(_truck, _truckId, truckID, _y);
    }

    public Integer getTruckStatus(Long truckID) {
        return getIntFromTable(_truck, _truckId, truckID, _status);
    }

    public Boolean getTruckIsSentUTruckArrived(Long truckID){
        return getBooleanFromTable(_truck,_truckId,truckID,_isSentUtruckArrived);
    }
    public Boolean getTruckIsSentUGoDeliver(Long truckID){
        return getBooleanFromTable(_truck,_truckId,truckID,_isSentUGoDeliver);
    }

    public ArrayList<Long> getTruckGroupOfStatus(Integer status) {
        String query = "SELECT * FROM " + _truck + " WHERE " + _status + "=" + status + ";";
        ArrayList<Long> idArrayList = new ArrayList<>();
        try {
            Statement stmt = c.createStatement();
            ResultSet resultSet = stmt.executeQuery(query);
            while (resultSet.next()) {
                idArrayList.add(resultSet.getLong(_truckId));
            }
            resultSet.close();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return idArrayList;
    }

    public ArrayList<Long> getAllTruckID(){
        String query = "SELECT * FROM " + _truck + ";";
        ArrayList<Long> idArrayList = new ArrayList<>();
        try {
            Statement stmt = c.createStatement();
            ResultSet resultSet = stmt.executeQuery(query);
            while (resultSet.next()) {
                idArrayList.add(resultSet.getLong(_truckId));
            }
            resultSet.close();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return idArrayList;
    }
    /******************************************************************************************************************
     * SHIPMENT METHOD
     ******************************************************************************************************************/

    public void addShipment(Long packageID, Long X, Long Y, Long warehouseID, String emailAddress, Integer status, Long shippingID, String trackingNum) {
        String query = "INSERT INTO " + _shipment + "(" + _packageId + ", " + _x + ", " + _y + ", " + _truckId + ", " +
                _warehouseId + "," + _emailAddress + "," + _status + "," + _shippingId +"," +_trackingNum+") VALUES (" +
                packageID + "," +
                X + "," +
                Y + "," +
                "NULL, " +
                warehouseID + ", \'" +
                emailAddress + "\' ," +
                status + "," +
                shippingID +", \'" +
                trackingNum+
                "\' );";
        executeStatement(query);
        try {
            c.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public void updateShipmentStatus(Long packageID, Integer status) {
        if (!isPrimaryKeyExist(_shipment, _packageId, packageID)) return;
        updateEntry(_shipment, _packageId, packageID, _status, status);
        try {
            this.c.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateShipmentTruckID(Long packageID, Long truckID) {
        if (isPrimaryKeyExist(_shipment, _packageId, packageID)) {
            updateEntry(_shipment, _packageId, packageID, _truckId, truckID);
            try {
                c.commit();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Used for TruckStatusThread Dispatch Truck to the same Shipment Warehouse location
     * @param shippingID shippingID related to One Shipping Request
     * @return ArrayList of All PackageID that has the same ShippingID
     */
    public ArrayList<Long> getShipmentPackageIDWithShippingID(Long shippingID) {
        String query = "SELECT * FROM " + _shipment + " WHERE " + _shippingId + " = " + shippingID + ";";
        ArrayList<Long> ans = new ArrayList<>();
        try {
            Statement statement = c.createStatement();
            ResultSet resultSet = statement.executeQuery(query);
            while (resultSet.next()) {
                ans.add(resultSet.getLong(_packageId));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ans;
    }

    /**
     * Used for WorldListener to Construct UShippingResponse with PackageID of the same Truck
     *
     * @param truckID The Truck that will carry those shipments
     * @return ArrayList of All PackageID that has assigned to the same TruckID
     */
    public ArrayList<Long> getShipmentPackageIDWithTruckID(Long truckID) {
        String query = "SELECT * FROM " + _shipment + " WHERE " + _truckId + " = " + truckID + ";";
        ArrayList<Long> ans = new ArrayList<>();
        try {
            Statement statement = c.createStatement();
            ResultSet resultSet = statement.executeQuery(query);
            while (resultSet.next()) {
                ans.add(resultSet.getLong(_packageId));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ans;
    }

    public Long getShipmentDestX(Long packageID) {
        return getLongFromTable(_shipment, _packageId, packageID, _x);
    }

    public Long getShipmentDestY(Long packageID) {
        return getLongFromTable(_shipment, _packageId, packageID, _y);
    }

    public Long getShipmentTruckID(Long packageID) {
        return getLongFromTable(_shipment, _packageId, packageID, _truckId);
    }

    public Long getShipmentWarehouseID(Long packageID) {
        return getLongFromTable(_shipment, _packageId, packageID, _warehouseId);
    }

    public String getShipmentEmailAddress(Long packageID) {
        return getStringFromTable(_shipment, _packageId, packageID, _emailAddress);
    }

    public Integer getShipmentStatus(Long packageID) {
        return getIntFromTable(_shipment, _packageId, packageID, _status);
    }

    public String getShipmentTrackingNum(Long packageID){
        return getStringFromTable(_shipment,_packageId, packageID, _trackingNum);
    }

    public Integer getShipmentTableSize() {
        return getTableSize(_shipment);
    }

    public Boolean isShipmentPackageIDExist(Long packageID){
        return isPrimaryKeyExist(_shipment,_packageId,packageID);
    }
    public void deleteShipment(Long packageID) {
        deleteEntry(_shipment, _packageId, packageID);
        try {
            c.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /******************************************************************************************************************
     * WAREHOUSE METHOD
     ******************************************************************************************************************/
    public void addWarehouse(Long wareHouseID, Long warehouseX, Long warehouseY) {
        String query = "INSERT INTO " + _warehouse + "(" + _warehouseId + "," + _x + ", " + _y + ") VALUES (" +
                wareHouseID + "," +
                warehouseX + "," +
                warehouseY +
                ");";
        executeStatement(query);
        try {
            c.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateWarehouse(Long warehouseID, Long warehouseX, Long warehouseY) {
        if (!isPrimaryKeyExist(_warehouse, _warehouseId, warehouseID)) return;
        updateEntry(_warehouse, _warehouseId, warehouseID, _x, warehouseX);
        updateEntry(_warehouse, _warehouseId, warehouseID, _y, warehouseY);
        try {
            c.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Boolean isWarehouseExist(Long warehouseID){
        return isPrimaryKeyExist(_warehouse,_warehouseId,warehouseID);
    }

    public Long getWarehouseX(Long warehouseID) {
        return getLongFromTable(_warehouse, _warehouseId, warehouseID, _x);
    }

    public Long getWarehouseY(Long warehouseID) {
        return getLongFromTable(_warehouse, _warehouseId, warehouseID, _y);
    }

    /******************************************************************************************************************
     * AUTHENTICATION METHOD
     ******************************************************************************************************************/
    public void addAuthentication(String username, String password) {
        String query = "INSERT INTO " + _authentication + "(" + _username + ", " + _password + ") VALUES (" +
                username + ", " +
                password +
                ");";
        executeStatement(query);
        try {
            c.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Boolean isPasswordCorrect(String username, String password) {
        if (isPrimaryKeyExist(_authentication, _username, username)) {
            if (getStringFromTable(_authentication, _username, username, _password) == password) {
                return true;
            }
        }
        return false;
    }

    public void updateAuthentication(String username, String password) {
        if (!isPrimaryKeyExist(_authentication, _username, username)) return;
        try {
            c.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /******************************************************************************************************************
     * UGOPICKUP METHOD
     ******************************************************************************************************************/

    public void addUGoPickup(Long seqnum, Long truckID, Long AseqNum, Long wareHouseID) {
        String query = "INSERT INTO " + _uGoPickup + " ( " + _wSeqNum + "," + _truckId + "," + _aSeqNum + ","+ _warehouseId+
                " ) VALUES ( " + seqnum + "," + truckID + "," + AseqNum + ","+ wareHouseID+" );";
        executeStatement(query);
        try {
            c.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Long getUGoPickupWareHouseID(Long seqnum){
        return getLongFromTable(_uGoPickup,_wSeqNum,seqnum,_warehouseId);
    }
    public Long getUGOPickupTruckID(Long seqnum) {
        return getLongFromTable(_uGoPickup, _wSeqNum, seqnum, _truckId);
    }

    public Long getUGOPickupASeqNum(Long seqnum) {
        return getLongFromTable(_uGoPickup, _wSeqNum, seqnum, _aSeqNum);
    }

    public Boolean isUGoPickupContainsWseq(Long wSeqNum) {
        if (isPrimaryKeyExist(_uGoPickup, _wSeqNum, wSeqNum)) {
            return true;
        }
        return false;
    }

    public void deleteUGoPickUp(Long seqnum) {
        if (isPrimaryKeyExist(_uGoPickup, _wSeqNum, seqnum)) {
            deleteEntry(_uGoPickup, _wSeqNum, seqnum);
            try {
                c.commit();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public ArrayList<Long> getUGoPickUpWSeqNums(){
        ArrayList<Long> wSeqnums = new ArrayList<>();
        String query = "SELECT * FROM "+ _uGoPickup+";";
        try {
            Statement statement = c.createStatement();
            ResultSet resultSet = statement.executeQuery(query);
            while(resultSet.next()){
                wSeqnums.add(resultSet.getLong(_wSeqNum));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return wSeqnums;
    }

    public Integer getUGoPickupTableSize() {
        return getTableSize(_uGoPickup);
    }

    /******************************************************************************************************************
     * QUERY METHOD
     ******************************************************************************************************************/
    public void addQuery(Long seqnum, Long truckID) {
        addResentEntry(_uQuery, _wSeqNum, seqnum, _truckId, truckID);
        try {
            c.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Long getQueryTruckID(Long seqnum) {
        return getLongFromTable(_uQuery, _wSeqNum, seqnum, _truckId);
    }

    public void deleteQuery(Long seqnum) {
        if (isPrimaryKeyExist(_uQuery, _wSeqNum, seqnum)) {
            deleteEntry(_uQuery, _wSeqNum, seqnum);
            try {
                c.commit();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }


    public Integer getQueryTableSize() {
        return getTableSize(_uQuery);
    }

    /******************************************************************************************************************
     * UGoDELIVERY METHOD
     ******************************************************************************************************************/
    public void addUGoDeliver(Long seqnum, Long truckID) {
        String query = "INSERT INTO " + _uGoDeliver + "(" + _wSeqNum + "," + _truckId + ") VALUES (" +
                seqnum + "," +
                truckID +
                ");";
        executeStatement(query);
        try {
            c.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<Long> getUGoDeliverWSeqNums(){
        ArrayList<Long> wSeqnums = new ArrayList<>();
        String query = "SELECT * FROM "+ _uGoDeliver+";";
        try {
            Statement statement = c.createStatement();
            ResultSet resultSet = statement.executeQuery(query);
            while(resultSet.next()){
                wSeqnums.add(resultSet.getLong(_wSeqNum));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return wSeqnums;
    }

    public Long getUGoDeliverTruckID(Long seqnum) {
        return getLongFromTable(_uGoDeliver, _wSeqNum, seqnum, _truckId);
    }

    public Boolean isUGoDeliverContainsWseq(Long wSeqNum) {
        if (isPrimaryKeyExist(_uGoDeliver, _wSeqNum, wSeqNum)) {
            return true;
        }
        return false;
    }

    public void deleteUGoDeliver(Long seqnum) {
        if (isPrimaryKeyExist(_uGoDeliver, _wSeqNum, seqnum)) {
            deleteEntry(_uGoDeliver, _wSeqNum, seqnum);
            try {
                c.commit();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public Integer getUGoDeliverTableSize() {
        return getTableSize(_uGoDeliver);
    }

    /******************************************************************************************************************
     * UShipmentStatusUpdate METHOD
     ******************************************************************************************************************/
    public void addUShipmentStatusUpdate(Long seqnum, Long packageID, Long truckID) {
        String query = "INSERT INTO " + _uShipmentStatusUpdate + "(" + _aSeqNum + ") VALUES (" +
                seqnum +
                ");";
        executeStatement(query);
        try {
            c.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<Long> getAllUShipmentStatusUpdate() {
        String query = "SELECT * FROM " + _uShipmentStatusUpdate + ";";
        ArrayList<Long> result = new ArrayList<>();
        try {
            Statement statement = this.c.createStatement();
            ResultSet resultSet = statement.executeQuery(query);
            while (resultSet.next()) {
                result.add(resultSet.getLong(_aSeqNum));
            }
            resultSet.close();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    public void deleteUShipmentStatusUpdate(Long seqnum) {
        if (isPrimaryKeyExist(_uShipmentStatusUpdate, _aSeqNum, seqnum)) {
            deleteEntry(_uShipmentStatusUpdate, _aSeqNum, seqnum);
            try {
                c.commit();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public Integer getUShipmentStatusUpdateTableSize() {
        return getTableSize(_uShipmentStatusUpdate);
    }

    /******************************************************************************************************************
     * AUShipmentStatuisUpdate METHOD
     ******************************************************************************************************************/
    public void addAUShipmentUpdate(Long packageID, Integer status, Long auShipmentStatusUpdateSeqNum) {
        String query = "INSERT INTO " + _aushipmentupdate + "(" + _packageId+","+_status +","+_aSeqNum+ ") VALUES (" +
                packageID+"," +
                status+","+
                auShipmentStatusUpdateSeqNum+
                ");";
        executeStatement(query);
        try {
            c.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public Integer getAUShipmentUpdateStatus(Long packageID){
        return getIntFromTable(_aushipmentupdate,_packageId,packageID,_status);
    }

    public ArrayList<Long> getAllAUShipmentUpdate() {
        String query = "SELECT * FROM " + _aushipmentupdate + ";";
        ArrayList<Long> result = new ArrayList<>();
        try {
            Statement statement = this.c.createStatement();
            ResultSet resultSet = statement.executeQuery(query);
            while (resultSet.next()) {
                result.add(resultSet.getLong(_packageId));
            }
            resultSet.close();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    /******************************************************************************************************************
     * USHIPPINGRESPONSE METHOD
     ******************************************************************************************************************/
    public void addUShippingResponse(Long seqnum, Long truckID) {
        addResentEntry(_uShippingResponse, _aSeqNum, seqnum, _truckId, truckID);
        try {
            c.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Long getUShippingResponseTruckID(Long seqnum) {
        return getLongFromTable(_uShippingResponse, _aSeqNum, seqnum, _truckId);
    }

    public void deleteUShippingResponse(Long seqnum) {
        if (isPrimaryKeyExist(_uShippingResponse, _aSeqNum, seqnum)) {
            deleteEntry(_uShippingResponse, _aSeqNum, seqnum);
            try {
                c.commit();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public ArrayList<Long> getAllUShippingResponse() {
        String query = "SELECT * FROM " + _uShippingResponse + ";";
        ArrayList<Long> result = new ArrayList<>();
        try {
            Statement statement = this.c.createStatement();
            ResultSet resultSet = statement.executeQuery(query);
            while (resultSet.next()) {
                result.add(resultSet.getLong(_aSeqNum));
            }
            resultSet.close();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    public Integer getUShippingResponseTableSize() {
        return getTableSize(_uShipmentStatusUpdate);
    }

    /******************************************************************************************************************
     * UTRUCKARRIVED METHOD
     ******************************************************************************************************************/
    public void addUTruckArrivedNotification(Long seqnum, Long truckID) {
        addResentEntry(_uTruckArrivedNotification, _aSeqNum, seqnum, _truckId, truckID);
        try {
            c.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Long getUTruckArrivedNotificationTruckID(Long seqnum) {
        return getLongFromTable(_uTruckArrivedNotification, _aSeqNum, seqnum, _truckId);
    }

    public void deleteUTruckArrivedNotification(Long seqnum) {
        if (isPrimaryKeyExist(_uTruckArrivedNotification, _aSeqNum, seqnum)) {
            deleteEntry(_uTruckArrivedNotification, _aSeqNum, seqnum);
            try {
                c.commit();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public ArrayList<Long> getAllUTruckArrivedNotification() {
        String query = "SELECT * FROM " + _uTruckArrivedNotification + ";";
        ArrayList<Long> result = new ArrayList<>();
        try {
            Statement statement = this.c.createStatement();
            ResultSet resultSet = statement.executeQuery(query);
            while (resultSet.next()) {
                result.add(resultSet.getLong(_aSeqNum));
            }
            resultSet.close();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }
    public Integer getUTruckArrivedNotificationTableSize() {
        return getTableSize(_uTruckArrivedNotification);
    }

    /******************************************************************************************************************
     * SHIPPINGREQUEST METHOD
     ******************************************************************************************************************/
    public void addShippingRequest(Long ShippingID, Long aSeqnum) {
        String query = "INSERT INTO " + _shippingRequest + "(" + _shippingId + "," + _aSeqNum + ") VALUES (" + ShippingID + "," + aSeqnum + ");";
        executeStatement(query);
        try {
            c.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<Long> getNumOfShippingRequestByOrder(Integer num) {
        String query = "SELECT * FROM " + _shippingRequest + " ORDER BY " + _shippingId + " ASC LIMIT " + num + ";";
        ArrayList<Long> ans = new ArrayList<>();
        try {
            Statement statement = this.c.createStatement();
            ResultSet resultSet = statement.executeQuery(query);

            while (resultSet.next()) {
                ans.add(resultSet.getLong(_shippingId));
            }
            resultSet.close();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ans;
    }

    public Long getShippingRequestASeqNum(Long shippingID) {
        return getLongFromTable(_shippingRequest, _shippingId, shippingID, _aSeqNum);
    }

    public Boolean isShippingRequestExist(Long seqNum){
        String query = "SELECT * FROM " + _shippingRequest + " WHERE " + _aSeqNum + " = "+ seqNum + ";";
        try {
            Statement statement = this.c.createStatement();
            ResultSet resultSet = statement.executeQuery(query);
            if(resultSet.next()){
                return true;
            }
            resultSet.close();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void deleteShippingRequest(Long shippingID) {
        if (isPrimaryKeyExist(_shippingRequest, _shippingId, shippingID)) {
            deleteEntry(_shippingRequest, _shippingId, shippingID);
            try {
                c.commit();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /******************************************************************************************************************
     * PRODUCT METHOD
     ******************************************************************************************************************/
    public void addProduct(Long packageId, String description, Long count) {
        String query = "INSERT INTO " + _product + "(" + _packageId + "," + _description + ","+_count + ") VALUES (" +
                packageId + ", \'" +
                description + "\' ," +
                count +
                ");";
        executeStatement(query);
        try {
            c.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public  ArrayList<ProductInfo> getProductsOf(Long packageID){
        ArrayList<ProductInfo> productInfoList = new ArrayList<>();
        String query = "SELECT * FROM " + _product +" WHERE "+ _packageId + " = " +packageID+";";
        try {
            Statement statement = this.c.createStatement();
            ResultSet resultSet = statement.executeQuery(query);
            while(resultSet.next()){
                ProductInfo productInfo = new ProductInfo(resultSet.getString(_description),resultSet.getLong(_count));
                productInfoList.add(productInfo);
            }
            resultSet.close();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return productInfoList;
    }

    public void close() {
        try {
            this.c.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
