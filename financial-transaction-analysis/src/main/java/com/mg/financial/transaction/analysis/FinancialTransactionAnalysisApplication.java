package com.mg.financial.transaction.analysis;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.function.ForeachFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.kafka010.ConsumerStrategies;
import org.apache.spark.streaming.kafka010.KafkaUtils;
import org.apache.spark.streaming.kafka010.LocationStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
public class FinancialTransactionAnalysisApplication {

    @Autowired
    private DataSource dataSource;

    @Value("${spring.datasource.url}")
    private String url;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    public static void main(String[] args) {
        SpringApplication.run(FinancialTransactionAnalysisApplication.class, args);
    }

    @PostConstruct
    public void init() throws InterruptedException, SQLException {
        createTables();

        initStreaming(url, username, password);
    }

    private static void initStreaming(String url, String username, String password) throws InterruptedException, SQLException {
        SparkConf sparkConf = new SparkConf().setAppName("TransactionAnalysis").setMaster("local[*]");
        JavaStreamingContext streamingContext = new JavaStreamingContext(sparkConf, Durations.seconds(10));

        Map<String, Object> kafkaParams = new HashMap<>();
        kafkaParams.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        kafkaParams.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        kafkaParams.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        kafkaParams.put(ConsumerConfig.GROUP_ID_CONFIG, "transaction-analysis-group");
        kafkaParams.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        kafkaParams.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);

        JavaInputDStream<ConsumerRecord<String, String>> messages = KafkaUtils.createDirectStream(
                streamingContext,
                LocationStrategies.PreferConsistent(),
                ConsumerStrategies.Subscribe(Collections.singletonList("transactions"), kafkaParams)
        );

        messages.foreachRDD(rdd -> {
            if (!rdd.isEmpty()) {
                SparkSession sparkSession = SparkSession.builder().config(rdd.context().getConf()).getOrCreate();

                // Define schema
                StructType schema = DataTypes.createStructType(new StructField[]{
                        DataTypes.createStructField("transactionId", DataTypes.StringType, false),
                        DataTypes.createStructField("fromAccountId", DataTypes.StringType, false),
                        DataTypes.createStructField("toAccountId", DataTypes.StringType, false),
                        DataTypes.createStructField("amount", DataTypes.createDecimalType(10, 2), false),
                        DataTypes.createStructField("transactionDate", DataTypes.TimestampType, false),
                        DataTypes.createStructField("transactionType", DataTypes.StringType, false)
                });

                // Parse JSON messages and create DataFrame
                Dataset<Row> transactions = sparkSession.read().schema(schema).json(rdd.map(ConsumerRecord::value));

                // Register DataFrame as a temporary view
                transactions.createOrReplaceTempView("transactions");

                // Insert transactions into H2 database
                transactions.foreach((ForeachFunction<Row>) row -> insertTransaction(row, url, username, password));

                // Calculate total transaction amount in the last 1 minute
                Dataset<Row> totalAmount = sparkSession.sql(
                        "SELECT SUM(amount) AS total_amount FROM transactions WHERE transactionDate > current_timestamp() - interval 1 minute"
                );

                totalAmount.show();

                // Insert total amount into H2 database
                totalAmount.foreach((ForeachFunction<Row>) row -> insertTotalAmount(row.getDecimal(0), url, username, password));

                // Detect suspicious transactions
                Dataset<Row> suspiciousTransactions = sparkSession.sql(
                        "SELECT * FROM transactions WHERE amount > 10000"
                );

                suspiciousTransactions.show();
            }
        });

        streamingContext.start();
        streamingContext.awaitTermination();
    }

    // Create H2 Database tables
    private void createTables() {
        String createTransactionsTable = "CREATE TABLE IF NOT EXISTS transactions (" +
                "transactionId VARCHAR(255) PRIMARY KEY," +
                "fromAccountId VARCHAR(255)," +
                "toAccountId VARCHAR(255)," +
                "amount DECIMAL(10, 2)," +
                "transactionDate TIMESTAMP," +
                "transactionType VARCHAR(255)" +
                ");";
        String createSummaryTable = "CREATE TABLE IF NOT EXISTS transaction_summary (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "total_amount DECIMAL(19, 2)," +
                "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ");";

        try (Connection conn = dataSource.getConnection()) {
            createTransactionTable(createTransactionsTable, conn);

            createTransactionSummaryTable(createSummaryTable, conn);
        } catch (SQLException e) {
            System.err.println("Error connecting to the database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void createTransactionSummaryTable(String createSummaryTable, Connection conn) {
        try (PreparedStatement stmt2 = conn.prepareStatement(createSummaryTable)) {
            if (!stmt2.execute()) {
                System.out.println("Transaction summary table created successfully or already exists.");
            } else {
                System.out.println("Unexpected result while creating Transaction summary table.");
            }
        } catch (SQLException e) {
            System.err.println("Error creating Transaction summary table: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void createTransactionTable(String createTransactionsTable, Connection conn) {
        try (PreparedStatement stmt1 = conn.prepareStatement(createTransactionsTable)) {
            if (!stmt1.execute()) {
                System.out.println("Transactions table created successfully or already exists.");
            } else {
                System.out.println("Unexpected result while creating Transactions table.");
            }
        } catch (SQLException e) {
            System.err.println("Error creating Transactions table: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Insert transaction into H2 database
    private static void insertTransaction(Row row, String url, String username, String password) {
        try (Connection conn = DriverManager.getConnection(url, username, password);
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO TRANSACTIONS (transactionId, fromAccountId, toAccountId, amount, transactionDate, transactionType) VALUES (?, ?, ?, ?, ?, ?)"
             )) {
            stmt.setString(1, row.getString(0));
            stmt.setString(2, row.getString(1));
            stmt.setString(3, row.getString(2));
            stmt.setBigDecimal(4, row.getDecimal(3));
            stmt.setTimestamp(5, row.getTimestamp(4));
            stmt.setString(6, row.getString(5));
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Insert total amount into H2 database
    private static void insertTotalAmount(BigDecimal totalAmount, String url, String username, String password) {
        try (Connection conn = DriverManager.getConnection(url,username, password);
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO transaction_summary (total_amount) VALUES (?)"
             )) {
            stmt.setBigDecimal(1, totalAmount);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
