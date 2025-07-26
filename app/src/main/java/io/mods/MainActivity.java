package io.mods;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private final String ADMIN_UID = "isFpWLV57ONA9TpXmMvNN9I78283";
    private static final int STORAGE_PERMISSION_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // Initialize Firebase
        FirebaseApp.initializeApp(this);

        bottomNav = findViewById(R.id.bottom_nav);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!user.getUid().equals(ADMIN_UID)) {
            Menu menu = bottomNav.getMenu();
            menu.removeItem(R.id.nav_upload);
        }

        checkStoragePermissionAndCreateDir();

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new ModsBrowserFragment())
                        .commit();
                return true;
            } else if (itemId == R.id.nav_games) {
                Toast.makeText(MainActivity.this, "Games", Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.nav_upload) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new UploadFragment())
                        .addToBackStack(null)
                        .commit();
                return true;
            } else if (itemId == R.id.nav_downloads) {
                Toast.makeText(MainActivity.this, "Downloads", Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.nav_profile) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new ProfileFragment())
                        .addToBackStack(null)
                        .commit();
                return true;
            }

            return false;
        });
    }

    private void checkStoragePermissionAndCreateDir() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            } else {
                createPoisonDirectory();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        STORAGE_PERMISSION_CODE);
            } else {
                createPoisonDirectory();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                createPoisonDirectory();
            } else {
                Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void createPoisonDirectory() {
        File dir = new File(Environment.getExternalStorageDirectory(), "poison");
        if (!dir.exists()) {
            boolean success = dir.mkdirs();
            if (success) {
                Toast.makeText(this, "Poison directory created", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to create poison directory", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Poison directory already exists", Toast.LENGTH_SHORT).show();
        }
    }
}
