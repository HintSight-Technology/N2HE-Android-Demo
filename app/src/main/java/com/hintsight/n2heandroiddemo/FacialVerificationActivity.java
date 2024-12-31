package com.hintsight.n2heandroiddemo;

import static com.hintsight.n2heandroiddemo.Parameters.getPolyDegree;
import static com.hintsight.n2heandroiddemo.Parameters.setFeatureLength;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.ext.SdkExtensions;
import android.provider.MediaStore;
import android.system.ErrnoException;
import android.system.Os;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresExtension;
import androidx.appcompat.app.AppCompatActivity;

import org.pytorch.executorch.EValue;
import org.pytorch.executorch.Module;
import org.pytorch.executorch.Tensor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class FacialVerificationActivity extends AppCompatActivity implements Runnable {
    private static final String TAG = "Facial Verification";

    private Button btnReset, btnVerify;
    private ImageButton imgBtnCamera;
    private EditText edtTextName;
    private ImageView faceImage;

    private Module model = null;
    private Bitmap mBitmap = null;
    private float[] features = new float[512];
    private final long[][] publicKey = new long[2][getPolyDegree()];
    private final int[] secretKey = new int[getPolyDegree()];
    private final Map<String, Object> postData = new HashMap<String, Object>();
    private final NetworkManager networkManager = new NetworkManager(null,this,
            "", "", secretKey);
    private long[][] encryptedFeatures;
    private ActivityResultLauncher<Intent> resultLauncher;
//    private Context context = (Context) this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_facial_verification);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        initSetup();
        try {
            readPublicKey();
            readSecretKey();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        registerResult();
        imgBtnCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MediaStore.ACTION_PICK_IMAGES);
                resultLauncher.launch(intent);
            }
        });

        btnVerify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnVerify.setEnabled(false);
                btnVerify.setText(getString(R.string.runModel));

                Thread thread = new Thread(FacialVerificationActivity.this);
                thread.start();
            }
        });

        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnVerify.setEnabled(false);
                btnReset.setEnabled(false);
                edtTextName.setText("");
                imgBtnCamera.setVisibility(View.VISIBLE);
                faceImage.setImageBitmap(null);
                faceImage.setBackgroundColor(Color.LTGRAY);
            }
        });
    }

    @Override
    public void run() {
        final Tensor inputTensor = TensorUtils.bitmapToFloat32Tensor(mBitmap);

        final long startTime = SystemClock.elapsedRealtime();
        Tensor outputTensor = model.forward(EValue.from(inputTensor))[0].toTensor();
        final long inferenceTime = SystemClock.elapsedRealtime() - startTime;
        Log.d(TAG, "inference time (ms): " + inferenceTime);
        features = outputTensor.getDataAsFloatArray();

        encryptFeatures();
        serverRequests();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                btnVerify.setEnabled(false);
                btnVerify.setText(getString(R.string.btnVerify));
            }
        });
    }

    private void registerResult() {
        resultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        Intent data = result.getData();
                        if (data != null && data.getData() != null) {
                            Uri selectedImageUri = data.getData();
                            Log.d(TAG + " Photo Picker", "Selected URI: $uri");
                            try {
                                ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(), selectedImageUri);
                                ImageDecoder.OnHeaderDecodedListener listener = new ImageDecoder.OnHeaderDecodedListener() {
                                    @Override
                                    public void onHeaderDecoded(@NonNull ImageDecoder decoder, @NonNull ImageDecoder.ImageInfo info, @NonNull ImageDecoder.Source source) {
                                        decoder.setMutableRequired(true);
                                    }
                                };
                                mBitmap = ImageDecoder.decodeBitmap(source, listener);
                                mBitmap = Bitmap.createScaledBitmap(mBitmap, 160, 160, true);
                                faceImage.setImageBitmap(mBitmap);
                                imgBtnCamera.setVisibility(View.GONE);
                                btnReset.setEnabled(true);
                                btnVerify.setEnabled(true);
                            }
                            catch (IOException e) {
                                Log.e(TAG, String.valueOf(e));
                            }

                        } else {
                            Log.d(TAG + " Photo Picker", "No media selected");
                        }
                    }
                }
        );
    }

    private void serverRequests() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy_HH:mm:ss:SSS");
        String formattedNow = now.format(formatter);
        String username = edtTextName.getText().toString();
        postData.put("id", formattedNow);
        postData.put("name", username);
        postData.put("feature_vector", encryptedFeatures);

        String serverPostUrl = "<SERVER_POST_URL>";
        networkManager.setServerPostUrl(serverPostUrl);
        String serverGetUrl = "<SERVER_GET_URL>/" + postData.get("name") +
                "_" + postData.get("id") + ".json";
        networkManager.setServerGetUrl(serverGetUrl);
        networkManager.mode = "FacialVerification";

        try {
            networkManager.postGetJSON(postData);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void encryptFeatures() {
        Log.d(TAG, "Encrypting extracted feature...");
        setFeatureLength(features.length);

        double l2norm = 0;
        for (float feature : features) {
            l2norm += pow(feature, 2);
        }
        l2norm = sqrt(l2norm);

        int[] normalizedFeatures = new int[features.length];
        int scalar0 = 25;
        for (int i = 0; i < features.length; i++) {
            double t = features[i] / l2norm * scalar0;
            int tt = (int) t;
            if (t > 0 && t-tt > 0.5) {
                normalizedFeatures[i] = tt + 1;
            } else if (t < 0 && tt-t > 0.5) {
                normalizedFeatures[i] = tt - 1;
            } else {
                normalizedFeatures[i] = tt;
            }
        }

        encryptedFeatures = Encryption.encrypt(normalizedFeatures, publicKey);
    }

    private void readSecretKey() throws IOException {
        Log.d(TAG, "Reading secret key txt...");
        String string = "";
        InputStream inputStream = this.getResources().openRawResource(R.raw.rlwe_sk);
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
        InputStream inputStream = this.getResources().openRawResource(R.raw.rlwe_pk);
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

    private void initSetup() {
        btnReset = findViewById(R.id.btnReset);
        btnVerify = findViewById(R.id.btnVerify);
        edtTextName = findViewById(R.id.edtTxtName);
        faceImage = findViewById(R.id.faceImage);
        imgBtnCamera = findViewById(R.id.imgBtnCamera);

        try {
            Os.setenv("ADSP_LIBRARY_PATH", getApplicationInfo().nativeLibraryDir, true);
        } catch (ErrnoException e) {
            Log.e(TAG, "Cannot set ADSP_LIBRARY_PATH", e);
            finish();
        }

//        try {
//            mBitmap = BitmapFactory.decodeStream(getAssets().open("face.png"), null, null);
//            mBitmap = Bitmap.createScaledBitmap(mBitmap, 160, 160, true);
//            faceImage.setImageBitmap(mBitmap);
//        } catch (IOException e) {
//            Log.e(TAG, "Error reading image assets", e);
//            finish();
//        }

        try {
            model = Module.load(assetFilePath(getApplicationContext(), "icrsv1_xnnpack_fp32.pte"));
        } catch (IOException e) {
            Log.e(TAG, "Error reading module assets", e);
            finish();
        }

        faceImage.setBackgroundColor(Color.LTGRAY);
        btnReset.setEnabled(false);
        btnVerify.setEnabled(false);
    }

    public static String assetFilePath(Context context, String assetName) throws IOException {
        File file = new File(context.getFilesDir(), assetName);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }

        try (InputStream is = context.getAssets().open(assetName)) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return file.getAbsolutePath();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

}