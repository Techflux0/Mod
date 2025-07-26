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
import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.provider.OpenableColumns;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentReference;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

public class UploadFragment extends Fragment {

    private static final int PICK_IMAGE = 1001;
    private static final int PICK_APK = 1002;

    private EditText modName, modVersion, modDescription, modFeatures;
    private Button selectImage, selectApk, uploadBtn;
    private TextView selectedImageText, selectedApkText;
    private Uri imageUri, apkUri;
    private ProgressDialog uploadProgressDialog;

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
        uploadProgressDialog = new ProgressDialog(getContext());
        uploadProgressDialog.setTitle("Uploading");
        uploadProgressDialog.setMessage("Please wait...");
        uploadProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        uploadProgressDialog.setCancelable(false);
        uploadProgressDialog.show();

        new Thread(() -> {
            try {
                String base64Image = convertToBase64(imageUri);
                String imageUrl = uploadToImgbb(base64Image);
                
                // Update progress for image upload
                requireActivity().runOnUiThread(() -> {
                    uploadProgressDialog.setMessage("Uploading APK...");
                    uploadProgressDialog.setProgress(33);
                });
                
                String apkUrl = uploadToPixelDrain(apkUri);
                
                // Update progress for APK upload
                requireActivity().runOnUiThread(() -> {
                    uploadProgressDialog.setMessage("Saving data...");
                    uploadProgressDialog.setProgress(66);
                });

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
                    uploadProgressDialog.dismiss();
                    requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Upload complete", Toast.LENGTH_SHORT).show());
                });
            } catch (Exception e) {
                uploadProgressDialog.dismiss();
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
        String urlString = "https://api.imgbb.com/1/upload";
        HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();

        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        String data = "key=" + URLEncoder.encode(imgbbApiKey, "UTF-8") +
                     "&image=" + URLEncoder.encode(base64Image, "UTF-8");

        try (OutputStream os = conn.getOutputStream()) {
            os.write(data.getBytes());
        }

        if (conn.getResponseCode() == 200) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                return new JSONObject(reader.readLine()).getJSONObject("data").getString("url");
            }
        } else {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                throw new IOException("IMGBB upload failed: " + reader.readLine());
            }
        }
    }

    private String uploadToPixelDrain(Uri uri) throws Exception {
        String filename = getFileNameFromUri(uri);
        long fileSize;
        
        try (InputStream sizeStream = requireActivity().getContentResolver().openInputStream(uri)) {
            fileSize = sizeStream.available();
            if (fileSize <= 0) throw new IOException("File is empty");
        }

        String boundary = "Boundary-" + System.currentTimeMillis();
        HttpURLConnection conn = (HttpURLConnection) new URL("https://pixeldrain.com/api/file").openConnection();
        
        try {
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Authorization", "Basic " + Base64.encodeToString((":" + pixelDrainApiKey).getBytes(), Base64.NO_WRAP));
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);

            try (OutputStream outputStream = conn.getOutputStream();
                 InputStream fileStream = requireActivity().getContentResolver().openInputStream(uri)) {
                
                String header = "--" + boundary + "\r\n" +
                    "Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"\r\n" +
                    "Content-Type: application/octet-stream\r\n\r\n";
                
                outputStream.write(header.getBytes(StandardCharsets.UTF_8));
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalUploaded = 0;
                while ((bytesRead = fileStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    totalUploaded += bytesRead;
                    int progress = (int) ((totalUploaded * 100) / fileSize);
                    requireActivity().runOnUiThread(() -> uploadProgressDialog.setProgress(progress));
                }
                
                outputStream.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            }

            if (conn.getResponseCode() == HttpURLConnection.HTTP_CREATED) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    JSONObject json = new JSONObject(reader.readLine());
                    if (!json.has("id")) throw new IOException("Missing file ID in response");
                    return "https://pixeldrain.com/u/" + json.getString("id");
                }
            } else {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                    throw new IOException("Upload failed: " + reader.readLine());
                }
            }
        } finally {
            conn.disconnect();
        }
    }

    private String getFileNameFromUri(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = requireActivity().getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) result = cursor.getString(nameIndex);
                }
            } catch (Exception ignored) {}
        }
        if (result == null) {
            result = uri.getLastPathSegment();
            if (result != null && result.contains("/")) {
                result = result.substring(result.lastIndexOf('/') + 1);
            }
        }
        return result != null ? result : "file.apk";
    }

    private String getFileSize(Uri uri) throws IOException {
        try (InputStream inputStream = requireActivity().getContentResolver().openInputStream(uri)) {
            double sizeInMB = inputStream.available() / (1024.0 * 1024.0);
            return String.format(Locale.getDefault(), "%.2f MB", sizeInMB);
        }
    }
}