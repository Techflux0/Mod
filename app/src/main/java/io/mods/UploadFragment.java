package io.mods;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentReference;

import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;

public class UploadFragment extends Fragment {

    private static final int PICK_IMAGE = 1001;
    private static final int PICK_APK = 1002;

    private EditText modName, modVersion, modDescription, modFeatures;
    private Button selectImage, selectApk, uploadBtn;
    private TextView selectedImageText, selectedApkText;
    private Uri imageUri, apkUri;

    private final String imgbbApiKey = "55ff84fd00968e159a31fe769343ef0e";
    private final String pixelDrainApiKey = "4b7ef806-f072-47cf-8410-44854fe185ce";
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private Context context;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.upload_fragment, container, false);

        modName = view.findViewById(R.id.et_mod_name);
        modVersion = view.findViewById(R.id.et_mod_version);
        modDescription = view.findViewById(R.id.et_description);
        modFeatures = view.findViewById(R.id.et_features);
        selectImage = view.findViewById(R.id.btn_select_image);
        selectApk = view.findViewById(R.id.btn_select_apk);
        uploadBtn = view.findViewById(R.id.btn_upload);
        selectedImageText = view.findViewById(R.id.tv_image_path);
        selectedApkText = view.findViewById(R.id.tv_apk_path);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        selectImage.setOnClickListener(v -> openFileChooser(PICK_IMAGE));
        selectApk.setOnClickListener(v -> openFileChooser(PICK_APK));

        uploadBtn.setOnClickListener(v -> {
            if (auth.getCurrentUser() == null || !auth.getCurrentUser().getUid().equals("isFpWLV57ONA9TpXmMvNN9I78283")) {
                Toast.makeText(getContext(), "Unauthorized", Toast.LENGTH_SHORT).show();
                return;
            }
            uploadMod();
        });

        return view;
    }

    private void openFileChooser(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType(requestCode == PICK_IMAGE ? "image/*" : "application/vnd.android.package-archive");
        startActivityForResult(Intent.createChooser(intent, "Select File"), requestCode);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && data != null) {
            if (requestCode == PICK_IMAGE) {
                imageUri = data.getData();
                selectedImageText.setText(imageUri.getLastPathSegment());
            } else if (requestCode == PICK_APK) {
                apkUri = data.getData();
                selectedApkText.setText(apkUri.getLastPathSegment());
            }
        }
    }

    private void uploadMod() {
        ProgressDialog progress = ProgressDialog.show(getContext(), "Uploading", "Please wait...");
    
        new Thread(() -> {
            try {
                String base64Image = convertToBase64(imageUri);
                String imageUrl = uploadToImgbb(base64Image);
                String apkUrl = uploadToPixelDrain(apkUri);
                String size = getFileSize(apkUri);
                String architecture = size.contains("arm64") ? "arm64" : "arm32";
                String createdAt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
    
                Map<String, Object> modData = new HashMap<>();
                modData.put("name", modName.getText().toString());
                modData.put("version", modVersion.getText().toString());
                modData.put("description", modDescription.getText().toString());
                modData.put("features", modFeatures.getText().toString());
                modData.put("mod_icon_url", imageUrl);
                modData.put("apk_url", apkUrl);
                modData.put("downloads", 0);
                modData.put("views", 0);
                modData.put("size", size);
                modData.put("architecture", architecture);
                modData.put("createdAt", createdAt);
    
                DocumentReference ref = db.collection("mods").document();
                modData.put("id", ref.getId());
                ref.set(modData).addOnSuccessListener(aVoid -> {
                    progress.dismiss();
                    requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Upload complete", Toast.LENGTH_SHORT).show());
                });
            } catch (Exception e) {
                progress.dismiss();
                e.printStackTrace();
                requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private String convertToBase64(Uri imageUri) throws IOException {
        InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
        byte[] bytes = new byte[inputStream.available()];
        inputStream.read(bytes);
        inputStream.close();
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }

    private String uploadToImgbb(String base64Image) throws Exception {
        String apiKey = imgbbApiKey;
        String urlString = "https://api.imgbb.com/1/upload";

        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        String data = "key=" + URLEncoder.encode(apiKey, "UTF-8") +
                      "&image=" + URLEncoder.encode(base64Image, "UTF-8");

        try (OutputStream os = conn.getOutputStream()) {
            os.write(data.getBytes());
            os.flush();
        }

        int responseCode = conn.getResponseCode();
        if (responseCode == 200) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            JSONObject json = new JSONObject(response.toString());
            return json.getJSONObject("data").getString("url");
        } else {
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
            StringBuilder error = new StringBuilder();
            String line;
            while ((line = errorReader.readLine()) != null) {
                error.append(line);
            }
            errorReader.close();

            throw new IOException("IMGBB upload failed (code " + responseCode + "): " + error.toString());
        }
    }

private String uploadToPixelDrain(Uri uri) throws Exception {
    String apiKey = pixelDrainApiKey;
    InputStream fileStream = null;
    HttpURLConnection conn = null;

    try {
        fileStream = requireActivity().getContentResolver().openInputStream(uri);
        if (fileStream == null || fileStream.available() <= 0) {
            throw new IOException("APK file is missing or empty.");
        }

        // First attempt to upload
        URL url = new URL("https://pixeldrain.com/api/file/");
        conn = (HttpURLConnection) url.openConnection();
        conn.setInstanceFollowRedirects(false); // Handle redirect manually
        conn.setRequestMethod("PUT");
        conn.setDoOutput(true);
        conn.setUseCaches(false);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(30000);

        String auth = "Basic " + Base64.encodeToString((":" + apiKey).getBytes(), Base64.NO_WRAP);
        conn.setRequestProperty("Authorization", auth);
        conn.setRequestProperty("Content-Type", "application/octet-stream");
        conn.setRequestProperty("Accept", "application/json");

        // Don't send the body yet — check for redirect first
        conn.connect();
        int responseCode = conn.getResponseCode();

        if (responseCode == 307) {
            String redirectUrl = conn.getHeaderField("Location");
            if (redirectUrl == null) throw new IOException("Redirect URL not found");

            conn.disconnect(); // Close the initial connection

            // Prepare redirected upload
            fileStream.close();
            fileStream = requireActivity().getContentResolver().openInputStream(uri);

URL redirect = new URL(url, redirectUrl);
conn = (HttpURLConnection) redirect.openConnection();


            conn.setRequestMethod("PUT");
            conn.setDoOutput(true);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);
            conn.setRequestProperty("Authorization", auth);
            conn.setRequestProperty("Content-Type", "application/octet-stream");
            conn.setRequestProperty("Accept", "application/json");

            try (OutputStream os = conn.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fileStream.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                os.flush();
            }

            responseCode = conn.getResponseCode();
        } else {
            throw new IOException("Expected redirect, got " + responseCode);
        }

        // Parse final response
        if (responseCode == 200) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            JSONObject json = new JSONObject(response.toString());
            return "https://pixeldrain.com/u/" + json.getString("id");
        } else {
            InputStream errorStream = conn.getErrorStream();
            StringBuilder errorResponse = new StringBuilder();
            if (errorStream != null) {
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(errorStream));
                String line;
                while ((line = errorReader.readLine()) != null) {
                    errorResponse.append(line);
                }
                errorReader.close();
            } else {
                errorResponse.append("No error stream available.");
            }

            throw new IOException("Upload failed (" + responseCode + "): " + errorResponse);
        }

    } finally {
        if (fileStream != null) fileStream.close();
        if (conn != null) conn.disconnect();
    }
}



    private String getFileSize(Uri uri) throws IOException {
        InputStream inputStream = requireActivity().getContentResolver().openInputStream(uri);
        int sizeInBytes = inputStream.available();
        inputStream.close();
        return (sizeInBytes / 1024) + " KB";
    }
}