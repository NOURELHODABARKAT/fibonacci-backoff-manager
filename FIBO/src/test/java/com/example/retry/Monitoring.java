package com.example.retry;
import java.util.concurrent.Callable;
import java.io.PrintWriter;
import java.io.IOException;

public class Monitoring {
    public static void exportRetryData(long[] delays, String filename) {
        try (PrintWriter pw = new PrintWriter(filename)) {
            pw.println("Attempt,Delay(ms)");
            for (int i = 0; i < delays.length; i++) {
                pw.println((i+1) + "," + delays[i]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
