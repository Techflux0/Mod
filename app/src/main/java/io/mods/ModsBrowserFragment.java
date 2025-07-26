package io.mods;

import android.animation.ObjectAnimator;
import android.app.DownloadManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.airbnb.lottie.LottieAnimationView;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class ModsBrowserFragment extends Fragment {

    private FirebaseFirestore db;
    private List<Mod> allMods = new ArrayList<>();
    private ModsAdapter adapter;
    private LottieAnimationView loadingAnimation;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                           @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.mods_browser, container, false);
        
        // Initialize views
        loadingAnimation = view.findViewById(R.id.loading_animation);
        RecyclerView modsRecycler = view.findViewById(R.id.mods_recycler);
        ViewGroup recentContainer = view.findViewById(R.id.recent_container);
        
        // Setup RecyclerView
        adapter = new ModsAdapter(allMods, this::showModDetails);
        modsRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        modsRecycler.setAdapter(adapter);
        
        // Setup search
        setupSearch(view.findViewById(R.id.search_input));
        
        // Load data from Firestore
        db = FirebaseFirestore.getInstance();
        loadMods(recentContainer);
        
        return view;
                }
            
        
    

private void loadMods(ViewGroup recentContainer) {
    loadingAnimation.setVisibility(View.VISIBLE);
    loadingAnimation.playAnimation();
    
    db.collection("mods")
      .orderBy("createdAt", Query.Direction.DESCENDING)
      .get()
      .addOnCompleteListener(task -> {
          if (task.isSuccessful()) {
              allMods.clear();
              for (QueryDocumentSnapshot document : task.getResult()) {
                  // DEBUG: Log the entire document
                  Log.d("FIRESTORE_DATA", "Document ID: " + document.getId() + 
                       " | Data: " + document.getData());
                  
                  Mod mod = document.toObject(Mod.class);
                  mod.setId(document.getId());
                  
                  // DEBUG: Verify URL loading
                  Log.d("URL_DEBUG", "Loaded URL: " + mod.getApkUrl() +
                       " | Exists in doc: " + document.contains("apk_url"));
                  
                  allMods.add(mod);
              }
              adapter.notifyDataSetChanged();
          }
          loadingAnimation.setVisibility(View.GONE);
      });
}
    private void addRecentModView(Mod mod, ViewGroup container) {
        View view = LayoutInflater.from(getContext())
            .inflate(R.layout.recent_mod_item, container, false);
            
        ImageView icon = view.findViewById(R.id.recent_mod_icon);
        TextView name = view.findViewById(R.id.recent_mod_name);
        
        Glide.with(this)
            .load(mod.getModIconUrl())
            .placeholder(R.drawable.ic_default_mod)
            .error(R.drawable.ic_default_mod)
            .into(icon);
            
        name.setText(mod.getName());
        view.setOnClickListener(v -> showModDetails(mod));
        
        container.addView(view);
    }

    private void setupRecentModsAnimation() {
        HorizontalScrollView scrollView = getView().findViewById(R.id.recent_scroll);
        scrollView.post(() -> {
            scrollView.fullScroll(HorizontalScrollView.FOCUS_RIGHT);
            ObjectAnimator animator = ObjectAnimator.ofInt(
                scrollView,
                "scrollX",
                scrollView.getWidth(),
                0
            );
            animator.setDuration(1000);
            animator.setInterpolator(new AccelerateDecelerateInterpolator());
            animator.start();
        });
    }

    private void setupSearch(TextInputEditText searchInput) {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterMods(s.toString());
            }
        });
    }

    private void filterMods(String query) {
        List<Mod> filtered = new ArrayList<>();
        for (Mod mod : allMods) {
            if (mod.getName().toLowerCase().contains(query.toLowerCase()) ||
                mod.getDescription().toLowerCase().contains(query.toLowerCase()) ||
                mod.getFeatures().toLowerCase().contains(query.toLowerCase())) {
                filtered.add(mod);
            }
        }
        adapter.updateList(filtered);
    }

private void showModDetails(Mod mod) {
    // 🔍 Log all mod details
    Log.d("MOD_CLICKED", "Name: " + mod.getName() +
        "\nVersion: " + mod.getVersion() +
        "\nDesc: " + mod.getDescription() +
        "\nFeatures: " + mod.getFeatures() +
        "\nAPK URL: " + mod.getApkUrl() +
        "\nIcon URL: " + mod.getModIconUrl() +
        "\nArch: " + mod.getArchitecture() +
        "\nSize: " + mod.getSize() +
        "\nViews: " + mod.getViews() +
        "\nDownloads: " + mod.getDownloads() +
        "\nCreated At: " + mod.getCreatedAt());

    ModDetailsFragment detailsFragment = ModDetailsFragment.newInstance(mod);
    getParentFragmentManager().beginTransaction()
        .replace(R.id.fragment_container, detailsFragment)
        .addToBackStack(null)
        .commit();
}


    private static class ModsAdapter extends RecyclerView.Adapter<ModsAdapter.ModViewHolder> {
        private List<Mod> mods;
        private final OnModClickListener listener;

        public ModsAdapter(List<Mod> mods, OnModClickListener listener) {
            this.mods = mods;
            this.listener = listener;
        }

        public void updateList(List<Mod> newList) {
            mods = newList;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ModViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.mod_item, parent, false);
            return new ModViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ModViewHolder holder, int position) {
            Mod mod = mods.get(position);
            
            Glide.with(holder.itemView.getContext())
                .load(mod.getModIconUrl())
                .placeholder(R.drawable.ic_default_mod)
                .error(R.drawable.ic_default_mod)
                .into(holder.modIcon);
            
            holder.modName.setText(mod.getName());
            holder.modVersion.setText("v" + mod.getVersion());
            holder.modSize.setText(mod.getSize());
            
            // Set architecture background color
            if ("arm64".equalsIgnoreCase(mod.getArchitecture())) {
                holder.modArch.setBackgroundResource(R.drawable.bg_arch_32);
            } else {
                holder.modArch.setBackgroundResource(R.drawable.bg_arch_32);
            }
            holder.modArch.setText(mod.getArchitecture().toUpperCase());
            
            holder.modViews.setText(String.valueOf(mod.getViews()));
            holder.modDownloads.setText(String.valueOf(mod.getDownloads()));
            holder.modDate.setText(mod.getCreatedAt());
            
            holder.itemView.setOnClickListener(v -> listener.onModClick(mod));
        }

        @Override
        public int getItemCount() {
            return mods.size();
        }

        static class ModViewHolder extends RecyclerView.ViewHolder {
            ImageView modIcon;
            TextView modName, modVersion, modSize, modArch, modViews, modDownloads, modDate;

            public ModViewHolder(@NonNull View itemView) {
                super(itemView);
                modIcon = itemView.findViewById(R.id.mod_icon);
                modName = itemView.findViewById(R.id.mod_name);
                modVersion = itemView.findViewById(R.id.mod_version);
                modSize = itemView.findViewById(R.id.mod_size);
                modArch = itemView.findViewById(R.id.mod_arch);
                modViews = itemView.findViewById(R.id.mod_views);
                modDownloads = itemView.findViewById(R.id.mod_downloads);
                modDate = itemView.findViewById(R.id.mod_date);
            }
        }

        interface OnModClickListener {
            void onModClick(Mod mod);
        }
    }
}