package io.mods;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import android.os.Build;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class ModDetailsFragment extends Fragment {

    private Mod mod;
    private ProgressBar progressBar;
    private long currentDownloadId = -1;
    private DownloadManager downloadManager;
    private Handler handler = new Handler();
    private BroadcastReceiver receiver;

    public static ModDetailsFragment newInstance(Mod mod) {
        ModDetailsFragment fragment = new ModDetailsFragment();
        Bundle args = new Bundle();
        args.putParcelable("mod", mod);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mod = getArguments().getParcelable("mod");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.mod_details, container, false);

        ImageView detailIcon = view.findViewById(R.id.detail_icon);
        TextView detailName = view.findViewById(R.id.detail_name);
        TextView detailVersion = view.findViewById(R.id.detail_version);
        TextView detailSize = view.findViewById(R.id.detail_size);
        TextView detailArch = view.findViewById(R.id.detail_arch);
        TextView detailViews = view.findViewById(R.id.detail_views);
        TextView detailDownloads = view.findViewById(R.id.detail_downloads);
        TextView detailDate = view.findViewById(R.id.detail_date);
        TextView detailDescription = view.findViewById(R.id.detail_description);
        TextView detailFeatures = view.findViewById(R.id.detail_features);
        MaterialButton downloadBtn = view.findViewById(R.id.download_btn);

        Glide.with(this).load(mod.getModIconUrl()).into(detailIcon);
        detailName.setText(mod.getName());
        detailVersion.setText("Version: " + mod.getVersion());
        detailSize.setText("Size: " + formatSize(mod.getSize()));
        detailArch.setText(mod.getArchitecture().toUpperCase());
        detailViews.setText(formatCount(mod.getViews()));
        detailDownloads.setText(formatCount(mod.getDownloads()));
        detailDate.setText("Uploaded: " + mod.getCreatedAt());
        detailDescription.setText(mod.getDescription());
        detailFeatures.setText(formatFeatures(mod.getFeatures()));

        progressBar = view.findViewById(R.id.download_progress);
        progressBar.setMax(100);
        progressBar.setProgress(0);
        progressBar.setVisibility(View.GONE);

        downloadBtn.setOnClickListener(v -> handleDownload());

        return view;
    }

private void handleDownload() {
    String downloadUrl = convertPixelDrainUrl(mod.getApkUrl());
    if (downloadUrl == null || downloadUrl.isEmpty()) {
        Toast.makeText(requireContext(), "Invalid download URL.", Toast.LENGTH_SHORT).show();
        return;
    }

    try {
        // Create download directory if it doesn't exist
        File poisonDir = new File(requireContext().getExternalFilesDir(null), "poison");
        if (!poisonDir.exists()) {
            if (!poisonDir.mkdirs()) {
                Toast.makeText(requireContext(), "Failed to create download directory", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        File apkFile = new File(poisonDir, createSafeFilename(mod.getName(), mod.getVersion()));
        Uri fileUri = Uri.fromFile(apkFile);

        // Delete existing file if it exists
        if (apkFile.exists()) {
            if (!apkFile.delete()) {
                Toast.makeText(requireContext(), "Failed to delete existing file", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadUrl));
        request.setTitle(mod.getName() + " v" + mod.getVersion());
        request.setDescription("Downloading mod...");
        request.setDestinationUri(fileUri);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.addRequestHeader("User-Agent", "Mozilla/5.0");
        request.addRequestHeader("Referer", "https://pixeldrain.com/");
        request.setAllowedOverMetered(true);
        request.setAllowedOverRoaming(true);

        downloadManager = (DownloadManager) requireContext().getSystemService(Context.DOWNLOAD_SERVICE);
        if (downloadManager != null) {
            currentDownloadId = downloadManager.enqueue(request);
            progressBar.setVisibility(View.VISIBLE);
            trackDownloadProgress();

            receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                    if (id == currentDownloadId) {
                        DownloadManager.Query query = new DownloadManager.Query();
                        query.setFilterById(id);
                        try (Cursor cursor = downloadManager.query(query)) {
                            if (cursor != null && cursor.moveToFirst()) {
                                int status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                    progressBar.setProgress(100);
                                    installAPK(fileUri);
                                } else {
                                    int reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON));
                                    handleDownloadError(reason);
                                }
                            }
                        }
                        try {
                            requireContext().unregisterReceiver(this);
                        } catch (IllegalArgumentException e) {
                            // Receiver was not registered, ignore
                        }
                    }
                }
            };

            // Register receiver with proper export flag
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requireContext().registerReceiver(
                    receiver,
                    new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                    Context.RECEIVER_EXPORTED
                );
            } else {
                requireContext().registerReceiver(
                    receiver,
                    new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
                );
            }
        }
    } catch (Exception e) {
        Toast.makeText(requireContext(), "Download failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        Log.e("DOWNLOAD", "Error starting download", e);
    }
}

private void handleDownloadError(int reason) {
    String errorMessage;
    switch (reason) {
        case DownloadManager.ERROR_CANNOT_RESUME:
            errorMessage = "Download cannot be resumed";
            break;
        case DownloadManager.ERROR_DEVICE_NOT_FOUND:
            errorMessage = "No external storage device found";
            break;
        case DownloadManager.ERROR_FILE_ALREADY_EXISTS:
            errorMessage = "File already exists";
            break;
        case DownloadManager.ERROR_FILE_ERROR:
            errorMessage = "File system error";
            break;
        case DownloadManager.ERROR_HTTP_DATA_ERROR:
            errorMessage = "HTTP transfer error";
            break;
        case DownloadManager.ERROR_INSUFFICIENT_SPACE:
            errorMessage = "Insufficient storage space";
            break;
        case DownloadManager.ERROR_TOO_MANY_REDIRECTS:
            errorMessage = "Too many redirects";
            break;
        case DownloadManager.ERROR_UNHANDLED_HTTP_CODE:
            errorMessage = "Unhandled HTTP code";
            break;
        case DownloadManager.ERROR_UNKNOWN:
        default:
            errorMessage = "Unknown error occurred during download";
            break;
    }

    requireActivity().runOnUiThread(() -> {
        progressBar.setVisibility(View.GONE);
        Toast.makeText(requireContext(), "Download failed: " + errorMessage, Toast.LENGTH_LONG).show();
    });
    Log.e("DOWNLOAD", "Download failed with reason: " + reason);
}

    private void trackDownloadProgress() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(currentDownloadId);
                if (downloadManager == null) return;

                var cursor = downloadManager.query(query);
                if (cursor != null && cursor.moveToFirst()) {
                    int totalBytes = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                    int downloadedBytes = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));

                    if (totalBytes > 0) {
                        int progress = (int) ((downloadedBytes * 100L) / totalBytes);
                        progressBar.setProgress(progress);
                    }

                    int status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                    if (status == DownloadManager.STATUS_SUCCESSFUL || status == DownloadManager.STATUS_FAILED) {
                        cursor.close();
                        return;
                    }

                    cursor.close();
                    handler.postDelayed(this, 500); // check every 500ms
                }
            }
        }, 500);
    }

    private void installAPK(Uri apkUri) {
        try {
            File file = new File(apkUri.getPath());
            Uri contentUri = FileProvider.getUriForFile(requireContext(),
                    requireContext().getPackageName() + ".provider", file);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Install error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("INSTALL", "Error", e);
        }
    }

private String convertPixelDrainUrl(String url) {
    if (url.contains("pixeldrain.com/u/")) {
        String id = url.substring(url.lastIndexOf("/") + 1);
        return "https://pixeldrain.com/api/file/" + id + "/download";
    }
    return url;
}


    private String createSafeFilename(String name, String version) {
        try {
            return URLEncoder.encode(name.replaceAll("[^a-zA-Z0-9]", "_") + "_v" + version + ".apk", "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return "mod_" + System.currentTimeMillis() + ".apk";
        }
    }

    private String formatSize(String size) {
        if (size.contains("KB")) {
            try {
                double kb = Double.parseDouble(size.replace(" KB", ""));
                return String.format("%.1f MB", kb / 1024);
            } catch (NumberFormatException e) {
                return size;
            }
        }
        return size;
    }

    private String formatCount(int count) {
        if (count >= 1_000_000) return String.format("%.1fM", count / 1_000_000f);
        if (count >= 1_000) return String.format("%.1fK", count / 1_000f);
        return String.valueOf(count);
    }

    private String formatFeatures(String features) {
        return (features == null || features.isEmpty()) ? "No features listed"
                : "• " + features.replace(", ", "\n• ");
    }

    @Override
public void onDestroyView() {
    super.onDestroyView();
    if (receiver != null) {
        try {
            requireContext().unregisterReceiver(receiver);
        } catch (IllegalArgumentException e) {
            // Receiver was not registered, ignore
        }
        receiver = null;
    }
    handler.removeCallbacksAndMessages(null);
}
}
