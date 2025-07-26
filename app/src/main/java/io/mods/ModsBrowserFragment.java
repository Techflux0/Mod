package io.mods;

import android.animation.ObjectAnimator;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ModsBrowserFragment extends Fragment {

    private static final String PREFS_NAME = "ModsPrefs";
    private static final String MODS_CACHE_KEY = "cached_mods";
    
    private FirebaseFirestore db;
    private List<Mod> allMods = new ArrayList<>();
    private ModsAdapter adapter;
    private LottieAnimationView loadingAnimation;
    private SharedPreferences preferences;
    private HorizontalScrollView recentScrollView;
    private View rootView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                           @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.mods_browser, container, false);
        
        preferences = requireContext().getSharedPreferences(PREFS_NAME, 0);
        
        // Initialize views
        loadingAnimation = rootView.findViewById(R.id.loading_animation);
        RecyclerView modsRecycler = rootView.findViewById(R.id.mods_recycler);
        ViewGroup recentContainer = rootView.findViewById(R.id.recent_container);
        recentScrollView = rootView.findViewById(R.id.recent_scroll);
        
        // Setup RecyclerView
        adapter = new ModsAdapter(allMods, this::showModDetails);
        modsRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        modsRecycler.setAdapter(adapter);
        
        // Setup search
        setupSearch(rootView.findViewById(R.id.search_input));
        
        // Load data - first try cache, then network
        db = FirebaseFirestore.getInstance();
        loadCachedMods(recentContainer);
        loadModsFromNetwork(recentContainer);
        
        return rootView;
    }

    private void loadCachedMods(ViewGroup recentContainer) {
        String cachedModsJson = preferences.getString(MODS_CACHE_KEY, null);
        if (cachedModsJson != null) {
            Type type = new TypeToken<List<Mod>>() {}.getType();
            List<Mod> cachedMods = new Gson().fromJson(cachedModsJson, type);
            if (cachedMods != null && !cachedMods.isEmpty()) {
                updateUIWithMods(cachedMods, recentContainer);
            }
        }
    }

    private void loadModsFromNetwork(ViewGroup recentContainer) {
        loadingAnimation.setVisibility(View.VISIBLE);
        loadingAnimation.playAnimation();

        db.collection("mods")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnCompleteListener(task -> {
                loadingAnimation.setVisibility(View.GONE);
                
                if (task.isSuccessful()) {
                    List<Mod> freshMods = new ArrayList<>();
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        Mod mod = document.toObject(Mod.class);
                        mod.setId(document.getId());
                        freshMods.add(mod);
                    }
                    
                    // Update UI
                    updateUIWithMods(freshMods, recentContainer);
                    
                    // Cache the data
                    cacheMods(freshMods);
                }
            });
    }

    private void updateUIWithMods(List<Mod> mods, ViewGroup recentContainer) {
        allMods.clear();
        allMods.addAll(mods);
        adapter.notifyDataSetChanged();

        recentContainer.removeAllViews();
        int recentCount = Math.min(mods.size(), 5);
        
        for (int i = 0; i < recentCount; i++) {
            addRecentModView(mods.get(i), recentContainer);
        }

        recentContainer.setVisibility(recentCount > 0 ? View.VISIBLE : View.GONE);
        if (recentCount > 0 && recentScrollView != null) {
            setupRecentModsAnimation();
        }
    }

    private void cacheMods(List<Mod> mods) {
        String modsJson = new Gson().toJson(mods);
        preferences.edit()
            .putString(MODS_CACHE_KEY, modsJson)
            .apply();
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
        if (recentScrollView == null || !isAdded()) return;
        
        recentScrollView.post(() -> {
            if (recentScrollView != null) {
                recentScrollView.fullScroll(HorizontalScrollView.FOCUS_RIGHT);
                ObjectAnimator animator = ObjectAnimator.ofInt(
                    recentScrollView,
                    "scrollX",
                    recentScrollView.getWidth(),
                    0
                );
                animator.setDuration(1000);
                animator.setInterpolator(new AccelerateDecelerateInterpolator());
                animator.start();
            }
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
            
            holder.modArch.setTypeface(null, android.graphics.Typeface.BOLD);
            holder.modArch.setText(mod.getArchitecture().equalsIgnoreCase("arm64") ? "ARM64" : "ARM32");

            int bgColor = mod.getArchitecture().equalsIgnoreCase("arm64") ? 0xFF1976D2 : 0xFF388E3C; 
            float radius = holder.itemView.getResources().getDisplayMetrics().density * 12; 

            android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
            drawable.setColor(bgColor);
            drawable.setCornerRadius(radius);

            holder.modArch.setBackground(drawable);
            holder.modArch.setTextColor(0xFFFFFFFF); 
            
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