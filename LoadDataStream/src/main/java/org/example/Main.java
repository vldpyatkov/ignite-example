package org.example;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteDataStreamer;
import org.apache.ignite.IgniteSystemProperties;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CachePeekMode;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.apache.ignite.cluster.ClusterState;
import org.apache.ignite.internal.util.IgniteUtils;
import org.h2.store.fs.FileUtils;
import org.h2.tools.Csv;

public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length < 1 || !args[0].endsWith(".xml") || !FileUtils.exists(args[0])) {
            System.err.println("The first parameter should be existed xml configuration file.");

            System.exit(1);
        }

        if (args.length < 2 || !args[1].endsWith(".csv") || !FileUtils.exists(args[1])) {
            System.err.println("The second parameter should be existed csv data file.");

            System.exit(1);
        }

        String cfg = args[0].trim();
        String dataFile = args[1].trim();

        System.out.println("Start with parameters [cfg=" + cfg + ", dataFile=" + dataFile + ']');

        String[] colNames = {
                "OBJECT_ID BIGINT",
                "PARAM_ID BIGINT",
                "SERIES_TIME TIMESTAMP",
                "VALUE DOUBLE",
                "PERIOD_ID VARCHAR",
                "ANALYTIC_ID VARCHAR",
                "UNIT_ID BIGINT"
        };

        System.setProperty(IgniteSystemProperties.IGNITE_QUIET, "false");

        System.out.println(
                "Work dir " + IgniteSystemProperties.getString(IgniteSystemProperties.IGNITE_WORK_DIR, IgniteUtils.defaultWorkDirectory()));

        Ignite ignite = Ignition.start(cfg);
        ignite.cluster().state(ClusterState.ACTIVE);

        IgniteCache cache = ignite.getOrCreateCache("default");

        AtomicLong rows = new AtomicLong();

        Csv csvReader = new Csv();
        ignite.cluster().disableWal("default");

        long startLoad = System.currentTimeMillis();

        try {
            csvReader.read(dataFile, colNames, "UTF-8");

            try (IgniteDataStreamer ds = ignite.dataStreamer("default")) {
                ds.keepBinary(true);

                Thread[] threads = new Thread[Runtime.getRuntime().availableProcessors()];

                for (int p = 0; p < threads.length; p++) {
                    threads[p] = new Thread(() -> {
                        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        String[] row;

                        while ((row = syncRed(csvReader)) != null) {
                            try {
                                ds.addData(
                                        ignite.binary().builder("TimedataKeyPart")
                                                .setField("OBJECT_ID", Long.valueOf(row[0]))
                                                .setField("PARAM_ID", Long.valueOf(row[1]))
                                                .setField("SERIES_TIME", format.parse(row[2]))
                                                .setField("PERIOD_ID", row[4])
                                                .setField("ANALYTIC_ID", row[5])
                                                .build(),
                                        ignite.binary().builder("TimedataValPart")
                                                .setField("VALUE", Double.valueOf(row[3]))
                                                .setField("UNIT_ID", Long.valueOf(row[6]))
                                                .build()
                                );
                            } catch (ParseException e) {
                                throw new RuntimeException(e);
                            }

                            rows.incrementAndGet();
                        }
                    });

                    threads[p].start();
                }

                for (int p = 0; p < threads.length; p++) {
                    threads[p].join();
                }
            }
        } finally {
            csvReader.close();
            ignite.cluster().enableWal("default");
        }

        cache.query(new SqlFieldsQuery("CREATE TABLE DEFAULT.TIMEDATA ("
                + "OBJECT_ID LONG,"
                + "PARAM_ID LONG,"
                + "SERIES_TIME TIMESTAMP,"
                + "VALUE DOUBLE,"
                + "PERIOD_ID VARCHAR,"
                + "ANALYTIC_ID VARCHAR,"
                + "UNIT_ID LONG,"
                + "CONSTRAINT timedata_pk PRIMARY KEY (OBJECT_ID,PARAM_ID,SERIES_TIME,PERIOD_ID,ANALYTIC_ID)"
                + ") WITH \"CACHE_NAME=default,PARALLELISM=" + Runtime.getRuntime().availableProcessors() / 2
                + ",KEY_TYPE=TimedataKeyPart,VALUE_TYPE=TimedataValPart\"")).getAll();

        List res = cache.query(new SqlFieldsQuery("SELECT COUNT(*) FROM TIMEDATA")).getAll();

        System.out.println("Rows loaded " + rows.get() + " time " + (System.currentTimeMillis() - startLoad));
        System.out.println("Rows in table " + res.get(0));
        System.out.println("Key-value cache size " + cache.sizeLong(CachePeekMode.PRIMARY));

        System.out.println("Caches " + ignite.cacheNames().stream().collect(Collectors.joining(", ")));
    }

    public static synchronized String[] syncRed(Csv csvReader) {
        try {
            return (String[]) csvReader.readRow();
        } catch (Exception e) {
            System.err.println("Read exception [err=" + e.getMessage() + ']');

            return null;
        }
    }
}