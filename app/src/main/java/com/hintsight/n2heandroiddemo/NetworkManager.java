package com.hintsight.n2heandroiddemo;

import static com.hintsight.n2heandroiddemo.Parameters.*;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class NetworkManager {
    private static final String TAG = "Network Manager";

    private final Context lrContext;
    private final int[] secretKey;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String serverPostUrl;
    private String serverGetUrl;
    private URL url;
    private HttpURLConnection con;
    private UserEncryptedResult encryptedResult = null;

    public NetworkManager(Context lrContext, String serverPostUrl, String serverGetUrl, int[] secretKey) {
        this.lrContext = lrContext;
        this.serverGetUrl = serverGetUrl;
        this.serverPostUrl = serverPostUrl;
        this.secretKey = secretKey;
    }

    public void setServerGetUrl(String serverGetUrl) { this.serverGetUrl = serverGetUrl; }
    public void setServerPostUrl(String serverPostUrl) { this.serverPostUrl = serverPostUrl; }

    public void postGetJSON(Map<String, Object> map) throws
            JsonProcessingException {
        Log.d(TAG, "Posting json data to cloud server...");

        String requestBody = objectMapper.writeValueAsString(map);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    url = new URL(serverPostUrl);
                    con = (HttpURLConnection) url.openConnection();
                    con.setRequestMethod("POST");
                } catch (IOException e) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(lrContext, "Server connection error. Please try again!",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
//                    Toast.makeText(lrContext, "Server connection error. Please try again!",
//                            Toast.LENGTH_SHORT).show();
                    throw new RuntimeException(e);
                }
                con.setRequestProperty("Content-Type", "application/json");
                con.setRequestProperty("Accept", "application/json");
                con.setDoInput(true);
                try (OutputStream os = con.getOutputStream()) {
                    byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                    new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8);
                } catch (Exception e) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(lrContext, "Connection timeout. Please try again!",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                    throw new RuntimeException(e);
                }

//                try (BufferedReader reader = new BufferedReader(
//                        new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
//                    StringBuilder response = new StringBuilder();
//                    String responsLine;
//                    while ((responsLine = reader.readLine()) != null) {
//                        response.append(responsLine.trim());
//                    }
//                } catch (Exception e) {
//                    throw new RuntimeException(e);
//                }

                getJSON();

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        long[] encryptedInput = new long[getPolyDegree() + 1];
                        for (int i = 0; i < (getPolyDegree() + 1); i++)
                            encryptedInput[i] = encryptedResult.getResult()[i];

                        long decryptedInput = Decryption.lwe64Dec(encryptedInput, secretKey, getPolyDegree());
                        System.out.println("decryption result of input = " + (decryptedInput));

                        AlertDialog.Builder builder = getBuilder(decryptedInput);
                        builder.create().show();
                    }
                });
            }

            private AlertDialog.Builder getBuilder(long decryptedInput) {
                AlertDialog.Builder builder = new AlertDialog.Builder(lrContext);
                builder.setTitle("Prediction Result!");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });

                if (decryptedInput < 20)
                    builder.setMessage("SAFE");
                else
                    builder.setMessage("RISKY");

                builder.setCancelable(false);
                return builder;
            }
        });

    }

    public void getJSON() {
        Log.d(TAG, "Getting json result from cloud server...");
        
        int statusCode;
        int triesCount = 50;
        int millisecondsToSleep = 50;

        try {
            url = new URL(serverGetUrl);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        do {
            try {
                con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                con.setRequestProperty("Content-Type", "application/json");
                con.setRequestProperty("Accept", "application/json");

                statusCode = con.getResponseCode();
                triesCount -= 1;
                Log.i(TAG, "GET request status code: " + con.getResponseCode());
                TimeUnit.MICROSECONDS.sleep(millisecondsToSleep);
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        } while (triesCount > 0 & statusCode != 200);

        if (statusCode == 200) {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String line;
                StringBuilder jsonData = new StringBuilder();

                while ((line = reader.readLine()) != null) {
                    jsonData.append(line);
                }

                encryptedResult = objectMapper.readValue(jsonData.toString(), UserEncryptedResult.class);
                reader.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            Log.d(TAG, "Failed to complete GET request");
        }

    }

}
