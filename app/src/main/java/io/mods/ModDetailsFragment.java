package io.mods;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class ModDetailsFragment extends Fragment {

    private Mod mod;
    private ProgressBar progressBar;
    private Handler handler = new Handler();

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

        // Set mod data
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
        progressBar.setVisibility(View.GONE); // not needed with browser flow

        downloadBtn.setOnClickListener(v -> openInDefaultBrowser());

        return view;
    }

    private void openInDefaultBrowser() {
        String rawUrl = mod.getApkUrl();
        if (rawUrl == null || !rawUrl.contains("pixeldrain.com")) {
            Toast.makeText(requireContext(), "Invalid download URL", Toast.LENGTH_SHORT).show();
            return;
        }

        // Convert /u/ link to direct download API format
        String finalUrl = convertPixelDrainUrl(rawUrl);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl));
        startActivity(intent);
    }

    private String convertPixelDrainUrl(String url) {
        if (url.contains("pixeldrain.com/u/")) {
            String id = url.substring(url.lastIndexOf("/") + 1);
            return "https://pixeldrain.com/api/file/" + id + "?download";
        }
        return url;
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

    private String createSafeFilename(String name, String version) {
        try {
            return URLEncoder.encode(name.replaceAll("[^a-zA-Z0-9]", "_") + "_v" + version + ".apk", "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return "mod_" + System.currentTimeMillis() + ".apk";
        }
    }
}
