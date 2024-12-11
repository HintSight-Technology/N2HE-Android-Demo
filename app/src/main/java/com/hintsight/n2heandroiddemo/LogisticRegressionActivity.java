package com.hintsight.n2heandroiddemo;

import static com.hintsight.n2heandroiddemo.Parameters.*;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class LogisticRegressionActivity extends AppCompatActivity {
    private static final String TAG = "Logistic Regression";

    private final int NUM_OF_ROWS = 6000;
    private final int NUM_OF_COLS = 25;

    private TextView limitBalText, genderText, educationText, marriageText, ageText, pay1Text,
            pay2Text, pay3Text, pay4Text, pay5Text, pay6Text, billAmt1, billAmt2, billAmt3,
            billAmt4, billAmt5, billAmt6, payAmt1, payAmt2, payAmt3, payAmt4, payAmt5, payAmt6;
    private Spinner spinnerID;
    private Button btnAccessRisk;

    private final int[][] data = new int[NUM_OF_ROWS][NUM_OF_COLS];
    private final long[][] publicKey = new long[2][getPolyDegree()];
    private final int[] secretKey = new int[getPolyDegree()];
    private final Map<String, Object> postData = new HashMap<String, Object>();
    private final NetworkManager networkManager = new NetworkManager(this,
            "", "", secretKey);

    private int dataID = -1;
    private long[][] encryptData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logistic_regression);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        initViews();
        setUpSpinner();
        try {
            readCsvData();
            readPublicKey();
            readSecretKey();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        spinnerID.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                dataID = position - 1;
                System.out.println(dataID);
                if (position > 0) {
                    displayData(position);
                    encryptTabularData();
                } else {
                    TextView textView = (TextView) view;
                    textView.setTextColor(Color.GRAY);
                    textView.setTypeface(Typeface.defaultFromStyle(Typeface.ITALIC));
                    clearData();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnAccessRisk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (dataID > -1) {
                    serverRequests();
                } else {
                    Toast.makeText(LogisticRegressionActivity.this, "Please select an ID to verify",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void serverRequests() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy_HH:mm:ss:SSS");
        String formattedNow = now.format(formatter);
        String username = "LR";
        postData.put("id", formattedNow);
        postData.put("name", username);
        postData.put("feature_vector", encryptData);

        String serverPostUrl = "<SERVER_POST_URL>";
        networkManager.setServerPostUrl(serverPostUrl);
        String serverGetUrl = "<SERVER_GET_URL>" + postData.get("name") +
                "_" + postData.get("id") + ".json";
        networkManager.setServerGetUrl(serverGetUrl);

        try {
            networkManager.postGetJSON(postData);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void encryptTabularData() {
        Log.d(TAG, "Encrypting tabular data...");
        setFeatureLength(23);
        encryptData = Encryption.encrypt(data[dataID], publicKey);
    }

    private void readSecretKey() throws IOException {
        Log.d(TAG, "Reading secret key txt...");
        String string = "";
        StringBuilder stringBuilder = new StringBuilder();
        InputStream inputStream = this.getResources().openRawResource(R.raw.rlwe_rlsk);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        while ((string = reader.readLine()) != null) {
            String[] secretKeyString = string.split(" ");
            for (int col = 0; col < getPolyDegree(); col++) {
                secretKey[col] = Integer.parseInt(secretKeyString[col]);
            }
        }
        inputStream.close();
    }

    private void readPublicKey() throws IOException {
        Log.d(TAG, "Reading public key txt...");
        String string = "";
        StringBuilder stringBuilder = new StringBuilder();
        InputStream inputStream = this.getResources().openRawResource(R.raw.rlwe_rlpk);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        int row = 0;
        while ((string = reader.readLine()) != null) {
            String[] publicKeyString = string.split(" ");
            for (int col = 0; col < getPolyDegree(); col++) {
                publicKey[row][col] = Long.parseLong(publicKeyString[col]);
            }
            row++;
        }
        inputStream.close();
    }

    private void setUpSpinner() {
        Log.d(TAG, "Setting up ID spinner...");
        List<String> id = new ArrayList<String>();
        id.add("ID");
        for (int i = 1; i <= NUM_OF_ROWS; i++) {
            id.add(Integer.toString(i));
        }

        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, id) {
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView textView = (TextView) view;
                if (position == 0) {
                    textView.setTextColor(Color.GRAY);
                    textView.setTypeface(Typeface.defaultFromStyle(Typeface.ITALIC));
                }
                return view;
            }
        };

        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerID.setAdapter(spinnerArrayAdapter);


    }

    private void displayData(int id) {
        limitBalText.setText(String.valueOf(data[id][0]));
        genderText.setText(String.valueOf(data[id][1]));
        educationText.setText(String.valueOf(data[id][2]));
        marriageText.setText(String.valueOf(data[id][3]));
        ageText.setText(String.valueOf(data[id][4]));
        pay1Text.setText(String.valueOf(data[id][5]));
        pay2Text.setText(String.valueOf(data[id][6]));
        pay3Text.setText(String.valueOf(data[id][7]));
        pay4Text.setText(String.valueOf(data[id][8]));
        pay5Text.setText(String.valueOf(data[id][9]));
        pay6Text.setText(String.valueOf(data[id][10]));
        billAmt1.setText(String.valueOf(data[id][11]));
        billAmt2.setText(String.valueOf(data[id][12]));
        billAmt3.setText(String.valueOf(data[id][13]));
        billAmt4.setText(String.valueOf(data[id][14]));
        billAmt5.setText(String.valueOf(data[id][15]));
        billAmt6.setText(String.valueOf(data[id][16]));
        payAmt1.setText(String.valueOf(data[id][17]));
        payAmt2.setText(String.valueOf(data[id][18]));
        payAmt3.setText(String.valueOf(data[id][19]));
        payAmt4.setText(String.valueOf(data[id][20]));
        payAmt5.setText(String.valueOf(data[id][21]));
        payAmt6.setText(String.valueOf(data[id][22]));
    }

    private void clearData() {
        limitBalText.setText("");
        genderText.setText("");
        educationText.setText("");
        marriageText.setText("");
        ageText.setText("");
        pay1Text.setText("");
        pay2Text.setText("");
        pay3Text.setText("");
        pay4Text.setText("");
        pay5Text.setText("");
        pay6Text.setText("");
        billAmt1.setText("");
        billAmt2.setText("");
        billAmt3.setText("");
        billAmt4.setText("");
        billAmt5.setText("");
        billAmt6.setText("");
        payAmt1.setText("");
        payAmt2.setText("");
        payAmt3.setText("");
        payAmt4.setText("");
        payAmt5.setText("");
        payAmt6.setText("");
    }

    private void initViews() {
        limitBalText = findViewById(R.id.limitBalText);
        genderText = findViewById(R.id.genderText);
        educationText = findViewById(R.id.educationText);
        marriageText = findViewById(R.id.marriageText);
        ageText = findViewById(R.id.ageText);
        pay1Text = findViewById(R.id.pay0Text);
        pay2Text = findViewById(R.id.pay2Text);
        pay3Text = findViewById(R.id.pay3Text);
        pay4Text = findViewById(R.id.pay4Text);
        pay5Text = findViewById(R.id.pay5Text);
        pay6Text = findViewById(R.id.pay6Text);
        billAmt1 = findViewById(R.id.billAmt1Text);
        billAmt2 = findViewById(R.id.billAmt2Text);
        billAmt3 = findViewById(R.id.billAmt3Text);
        billAmt4 = findViewById(R.id.billAmt4Text);
        billAmt5 = findViewById(R.id.billAmt5Text);
        billAmt6 = findViewById(R.id.billAmt6Text);
        payAmt1 = findViewById(R.id.payAmt1Text);
        payAmt2 = findViewById(R.id.payAmt2Text);
        payAmt3 = findViewById(R.id.payAmt3Text);
        payAmt4 = findViewById(R.id.payAmt4Text);
        payAmt5 = findViewById(R.id.payAmt5Text);
        payAmt6 = findViewById(R.id.payAmt6Text);

        spinnerID = findViewById(R.id.spinnerID);
        btnAccessRisk = findViewById(R.id.btnAccessRisk);
    }

    private void readCsvData() throws IOException {
        Log.d(TAG, "Reading csv tabular data...");
        //read in csv tabular data
        InputStream inputStream = this.getResources().openRawResource(R.raw.data);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream,
                StandardCharsets.UTF_8));

        int row = 0;
        int col = 0;
        while (reader.readLine() != null) {
            String[] values = reader.readLine().split(" ");
            for (String value : values) {
                float valueF = Float.parseFloat(value);
                data[row][col] = (int) valueF;
                col++;
            }
            row++;
            col = 0;
        }

        inputStream.close();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

}