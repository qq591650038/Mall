import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Generates a resumable load-test dataset without deleting existing data.
 * Run with: java -cp <mysql-connector.jar>;. GenerateLargeDataset --run
 */
public final class GenerateLargeDataset {
    private static final String URL = "jdbc:mysql://localhost:3306/mall?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false&rewriteBatchedStatements=true";
    private static final String USER = "root";
    private static final String PASSWORD = "123456";
    private static final int PRODUCT_COUNT = 5_000;
    private static final int USER_COUNT = 500_000;
    private static final int USER_BATCH = 1_000;
    private static final int MIN_RELATIONS_PER_USER = 20;
    private static final int MAX_RELATIONS_PER_USER = 30;
    private static final String USER_PREFIX = "load_user_";
    private static final String PRODUCT_PREFIX = "load_product_";
    private static final String PASSWORD_HASH = "$2a$10$w5o3A0N4e1MXCqLhMcBWzeZSqqPgv8s1jUfP1qR4f1lSjTgIOMYwq";
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private record Product(long id, long skuId, String name, BigDecimal price) { }
    private record OrderData(long userId, int userOrdinal, LocalDateTime createdAt, BigDecimal amount, int status) { }

    public static void main(String[] args) throws Exception {
        if (args.length == 1 && "--inspect".equals(args[0])) {
            try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD)) {
                printColumns(connection);
            }
            return;
        }
        if (args.length == 1 && "--status".equals(args[0])) {
            try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD)) {
                System.out.printf("users=%d, products=%d, orders=%d, orderItems=%d, carts=%d, favorites=%d, browseHistory=%d%n",
                        countGeneratedUsers(connection),
                        count(connection, "SELECT COUNT(*) FROM product WHERE name LIKE '" + PRODUCT_PREFIX + "%'"),
                        count(connection, "SELECT COUNT(*) FROM `order` WHERE order_no LIKE 'LOAD%'"),
                        count(connection, "SELECT COUNT(*) FROM order_item oi JOIN `order` o ON o.id=oi.order_id WHERE o.order_no LIKE 'LOAD%'"),
                        count(connection, "SELECT COUNT(*) FROM cart c JOIN `user` u ON u.id=c.user_id WHERE u.username LIKE '" + USER_PREFIX + "%'"),
                        count(connection, "SELECT COUNT(*) FROM favorite f JOIN `user` u ON u.id=f.user_id WHERE u.username LIKE '" + USER_PREFIX + "%'"),
                        count(connection, "SELECT COUNT(*) FROM browse_history b JOIN `user` u ON u.id=b.user_id WHERE u.username LIKE '" + USER_PREFIX + "%'"));
            }
            return;
        }
        if (args.length == 1 && "--smoke".equals(args[0])) {
            try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD)) {
                connection.setAutoCommit(false);
                List<Product> products = ensureProducts(connection);
                generateUsersAndData(connection, products, 0, 1);
                connection.rollback();
                System.out.println("Smoke test passed; transaction rolled back.");
            }
            return;
        }
        if (args.length != 1 || !"--run".equals(args[0])) {
            System.err.println("Refusing to modify the database. Re-run with --run after reviewing this script.");
            return;
        }
        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD)) {
            connection.setAutoCommit(false);
            verifySchema(connection);
            List<Product> products = ensureProducts(connection);
            int completedUsers = countGeneratedUsers(connection);
            System.out.printf("Products ready: %d. Resuming user generation at %d/%d.%n", products.size(), completedUsers, USER_COUNT);
            for (int start = completedUsers; start < USER_COUNT; start += USER_BATCH) {
                int end = Math.min(start + USER_BATCH, USER_COUNT);
                generateUsersAndData(connection, products, start, end);
                System.out.printf("Committed users %d-%d of %d at %s%n", start + 1, end, USER_COUNT, LocalDateTime.now().format(TIME));
            }
        }
    }

    private static void verifySchema(Connection connection) throws SQLException {
        String[] tables = {"user", "product", "product_sku", "order", "order_item", "cart", "favorite", "browse_history"};
        DatabaseMetaData meta = connection.getMetaData();
        for (String table : tables) {
            try (ResultSet result = meta.getTables(connection.getCatalog(), null, table, new String[]{"TABLE"})) {
                if (!result.next()) throw new SQLException("Required table is missing: " + table);
            }
        }
    }

    private static void printColumns(Connection connection) throws SQLException {
        DatabaseMetaData meta = connection.getMetaData();
        for (String table : new String[]{"cart", "favorite", "browse_history", "order", "order_item", "user"}) {
            List<String> columns = new ArrayList<>();
            try (ResultSet rows = meta.getColumns(connection.getCatalog(), null, table, null)) {
                while (rows.next()) columns.add(rows.getString("COLUMN_NAME"));
            }
            System.out.println(table + ": " + String.join(", ", columns));
        }
    }

    private static List<Product> ensureProducts(Connection connection) throws SQLException {
        int existing = count(connection, "SELECT COUNT(*) FROM product WHERE name LIKE '" + PRODUCT_PREFIX + "%'");
        if (existing == 0) {
            try (PreparedStatement product = connection.prepareStatement(
                    "INSERT INTO product(category_id,brand_id,name,subtitle,main_image,price,original_price,total_stock,sales,status,is_recommend,description,create_time,update_time,deleted) VALUES (1,NULL,?,?,?,?,?,?,0,1,0,?,?,?,0)",
                    Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement sku = connection.prepareStatement(
                    "INSERT INTO product_sku(product_id,sku_code,spec_info,price,original_price,stock,image,status,create_time,update_time,deleted) VALUES (?,?,?,?,?,?,?,1,?,?,0)")) {
                for (int index = 1; index <= PRODUCT_COUNT; index++) {
                    BigDecimal price = BigDecimal.valueOf(19 + index % 800).add(BigDecimal.valueOf((index % 100) / 100.0)).setScale(2);
                    product.setString(1, PRODUCT_PREFIX + String.format("%05d", index));
                    product.setString(2, "Load test product " + index);
                    product.setString(3, "/uploads/load-products/" + index + ".jpg");
                    product.setBigDecimal(4, price);
                    product.setBigDecimal(5, price.add(BigDecimal.valueOf(20)));
                    product.setInt(6, 100_000);
                    product.setString(7, "Synthetic product for load testing");
                    product.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
                    product.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));
                    product.executeUpdate();
                    try (ResultSet keys = product.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Product key was not generated");
                        long productId = keys.getLong(1);
                        sku.setLong(1, productId);
                        sku.setString(2, "LOAD-SKU-" + String.format("%05d", index));
                        sku.setString(3, "{\"source\":\"load-test\"}");
                        sku.setBigDecimal(4, price);
                        sku.setBigDecimal(5, price.add(BigDecimal.valueOf(20)));
                        sku.setInt(6, 100_000);
                        sku.setString(7, "/uploads/load-products/" + index + ".jpg");
                        sku.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
                        sku.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));
                        sku.addBatch();
                    }
                    if (index % 500 == 0) sku.executeBatch();
                }
                sku.executeBatch();
                connection.commit();
            } catch (SQLException error) {
                connection.rollback();
                throw error;
            }
        }
        List<Product> products = new ArrayList<>(PRODUCT_COUNT);
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT p.id, s.id, p.name, p.price FROM product p JOIN product_sku s ON s.product_id=p.id WHERE p.name LIKE ? ORDER BY p.id")) {
            statement.setString(1, PRODUCT_PREFIX + "%");
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) products.add(new Product(rows.getLong(1), rows.getLong(2), rows.getString(3), rows.getBigDecimal(4)));
            }
        }
        if (products.size() != PRODUCT_COUNT) throw new SQLException("Expected " + PRODUCT_COUNT + " generated products but found " + products.size());
        return products;
    }

    private static void generateUsersAndData(Connection connection, List<Product> products, int start, int end) throws SQLException {
        List<Long> userIds = insertUsers(connection, start, end);
        Random random = new Random(7_290_000L + start);
        try (PreparedStatement cart = connection.prepareStatement("INSERT INTO cart(user_id,product_id,sku_id,quantity,selected,create_time,update_time,deleted) VALUES (?,?,?,?,1,?,?,0)");
             PreparedStatement favorite = connection.prepareStatement("INSERT INTO favorite(user_id,product_id,original_price,price_alert,stock_alert,last_price,last_stock,create_time) VALUES (?,?,?,1,0,?,?,?)");
             PreparedStatement browse = connection.prepareStatement("INSERT INTO browse_history(user_id,product_id,browse_time,create_time) VALUES (?,?,?,?)")) {
            for (int offset = 0; offset < userIds.size(); offset++) {
                long userId = userIds.get(offset);
                LocalDateTime now = LocalDateTime.now().minusDays(random.nextInt(7));
                int cartCount = randomCount(random);
                int favoriteCount = randomCount(random);
                int browseCount = randomCount(random);
                Set<Integer> picks = uniqueProductIndexes(random, cartCount + favoriteCount + browseCount);
                Iterator<Integer> iterator = picks.iterator();
                for (int i = 0; i < cartCount; i++) {
                    Product product = products.get(iterator.next());
                    cart.setLong(1, userId); cart.setLong(2, product.id); cart.setLong(3, product.skuId); cart.setInt(4, 1 + random.nextInt(3)); cart.setTimestamp(5, Timestamp.valueOf(now)); cart.setTimestamp(6, Timestamp.valueOf(now)); cart.addBatch();
                }
                for (int i = 0; i < favoriteCount; i++) {
                    Product saved = products.get(iterator.next());
                    favorite.setLong(1, userId); favorite.setLong(2, saved.id); favorite.setBigDecimal(3, saved.price); favorite.setBigDecimal(4, saved.price); favorite.setInt(5, 100_000); favorite.setTimestamp(6, Timestamp.valueOf(now)); favorite.addBatch();
                }
                for (int i = 0; i < browseCount; i++) {
                    Product viewed = products.get(iterator.next());
                    browse.setLong(1, userId); browse.setLong(2, viewed.id); browse.setTimestamp(3, Timestamp.valueOf(now)); browse.setTimestamp(4, Timestamp.valueOf(now)); browse.addBatch();
                }
            }
            cart.executeBatch(); favorite.executeBatch(); browse.executeBatch();
        }
        insertOrdersAndItems(connection, products, userIds, start, random);
        connection.commit();
    }

    private static List<Long> insertUsers(Connection connection, int start, int end) throws SQLException {
        List<Long> ids = new ArrayList<>(end - start);
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO `user`(username,phone,email,password,salt,avatar,nickname,gender,status,create_time,update_time,deleted,member_level_id) VALUES (?,?,?,?,?,?,?,?,1,?,?,0,1)", Statement.RETURN_GENERATED_KEYS)) {
            for (int index = start; index < end; index++) {
                String suffix = String.format("%06d", index + 1);
                Timestamp now = Timestamp.valueOf(LocalDateTime.now());
                statement.setString(1, USER_PREFIX + suffix);
                statement.setString(2, "18" + String.format("%09d", index + 1));
                statement.setString(3, "load" + suffix + "@example.test");
                statement.setString(4, PASSWORD_HASH); statement.setString(5, "load-test"); statement.setString(6, null);
                statement.setString(7, "Load User " + suffix); statement.setInt(8, index % 3); statement.setTimestamp(9, now); statement.setTimestamp(10, now); statement.addBatch();
            }
            statement.executeBatch();
            try (ResultSet keys = statement.getGeneratedKeys()) { while (keys.next()) ids.add(keys.getLong(1)); }
        }
        if (ids.size() != end - start) throw new SQLException("User key count mismatch. Expected " + (end - start) + ", got " + ids.size());
        return ids;
    }

    private static void insertOrdersAndItems(Connection connection, List<Product> products, List<Long> userIds, int start, Random random) throws SQLException {
        List<OrderData> orders = new ArrayList<>();
        LocalDateTime base = LocalDateTime.now().minusDays(7).withHour(9).withMinute(0).withSecond(0).withNano(0);
        for (int i = 0; i < userIds.size(); i++) {
            for (int day = 0; day < 7; day++) for (int count = 0, daily = 2 + random.nextInt(4); count < daily; count++) {
                BigDecimal amount = BigDecimal.valueOf(50 + random.nextInt(900)).setScale(2);
                orders.add(new OrderData(userIds.get(i), start + i + 1, base.plusDays(day).plusMinutes(random.nextInt(600)), amount, random.nextBoolean() ? 3 : 1));
            }
        }
        try (PreparedStatement order = connection.prepareStatement(
                "INSERT INTO `order`(order_no,user_id,total_amount,discount_amount,freight_amount,pay_amount,pay_status,pay_time,order_status,ship_time,receive_time,address_snapshot,remark,order_source,order_type,create_time,update_time,deleted) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
             PreparedStatement item = connection.prepareStatement("INSERT INTO order_item(order_id,product_id,sku_id,product_name,sku_info,product_image,price,quantity,subtotal,create_time) VALUES (?,?,?,?,?,?,?,?,?,?)")) {
            int sequence = 0;
            for (OrderData data : orders) {
                Timestamp time = Timestamp.valueOf(data.createdAt);
                order.setString(1, "LOAD" + String.format("%06d", data.userOrdinal) + String.format("%02d", sequence++)); order.setLong(2, data.userId);
                order.setBigDecimal(3, data.amount); order.setBigDecimal(4, BigDecimal.ZERO); order.setBigDecimal(5, BigDecimal.ZERO); order.setBigDecimal(6, data.amount);
                order.setInt(7, 1); order.setTimestamp(8, time); order.setInt(9, data.status); order.setTimestamp(10, Timestamp.valueOf(data.createdAt.plusHours(4))); order.setTimestamp(11, data.status == 3 ? Timestamp.valueOf(data.createdAt.plusDays(2)) : null);
                order.setString(12, "{\"receiverName\":\"Load User\",\"receiverPhone\":\"18000000000\",\"province\":\"Load\",\"city\":\"Test\",\"district\":\"Data\",\"detailAddress\":\"Synthetic address\"}"); order.setString(13, "Generated load-test order"); order.setString(14, "NORMAL"); order.setString(15, "PHYSICAL"); order.setTimestamp(16, time); order.setTimestamp(17, time); order.setInt(18, 0); order.addBatch();
            }
            order.executeBatch();
            List<Long> orderIds = new ArrayList<>(orders.size()); try (ResultSet keys = order.getGeneratedKeys()) { while (keys.next()) orderIds.add(keys.getLong(1)); }
            if (orderIds.size() != orders.size()) throw new SQLException("Order key count mismatch");
            for (long orderId : orderIds) {
                int items = 1 + random.nextInt(3); Set<Integer> picks = uniqueProductIndexes(random, items);
                for (int pick : picks) { Product product = products.get(pick); int quantity = 1 + random.nextInt(3); BigDecimal subtotal = product.price.multiply(BigDecimal.valueOf(quantity));
                    item.setLong(1, orderId); item.setLong(2, product.id); item.setLong(3, product.skuId); item.setString(4, product.name); item.setString(5, "{\"source\":\"load-test\"}"); item.setString(6, "/uploads/load-products/" + product.id + ".jpg"); item.setBigDecimal(7, product.price); item.setInt(8, quantity); item.setBigDecimal(9, subtotal); item.setTimestamp(10, Timestamp.valueOf(LocalDateTime.now())); item.addBatch(); }
            }
            item.executeBatch();
            String orderIdList = orderIds.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
            try (PreparedStatement totals = connection.prepareStatement(
                    "UPDATE `order` o JOIN (SELECT order_id, SUM(subtotal) AS total FROM order_item WHERE order_id IN (" + orderIdList + ") GROUP BY order_id) i ON i.order_id = o.id SET o.total_amount = i.total, o.pay_amount = i.total")) {
                totals.executeUpdate();
            }
        }
    }

    private static int randomCount(Random random) { return MIN_RELATIONS_PER_USER + random.nextInt(MAX_RELATIONS_PER_USER - MIN_RELATIONS_PER_USER + 1); }
    private static Set<Integer> uniqueProductIndexes(Random random, int count) { Set<Integer> values = new LinkedHashSet<>(); while (values.size() < count) values.add(random.nextInt(PRODUCT_COUNT)); return values; }
    private static int countGeneratedUsers(Connection connection) throws SQLException { return count(connection, "SELECT COUNT(*) FROM `user` WHERE username LIKE '" + USER_PREFIX + "%'"); }
    private static int count(Connection connection, String sql) throws SQLException { try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery(sql)) { rows.next(); return rows.getInt(1); } }
}
