package org.example;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Formatter;
import java.util.Locale;
import java.util.Random;

public class Generator {

    public static void main(String[] args) throws Exception {
        Random rand = new Random();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter("C:\\temp\\data_large.csv", true))) {
            for (int i = 0; i < 5_000_000; i++) {
                writer.append(String.format(
                                "%s, "
                                + "%s, "
                                + "\"%s\", "
                                + new Formatter(Locale.US).format("%.2f, ", Math.scalb(rand.nextDouble() * 100, 2))
                                + "\"period %s\", "
                                + "\"AF%sAAA\", "
                                + "%s\n",
                        i, i * 10, format.format(rand.nextLong() % new Date().getTime()), i, i, i * 17));
            }
        }
    }
}
