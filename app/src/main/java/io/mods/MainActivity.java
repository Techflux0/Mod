package io.mods;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.FirebaseApp;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // Initialize Firebase
        FirebaseApp.initializeApp(this);

        bottomNav = findViewById(R.id.bottom_nav);

        bottomNav.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                int itemId = item.getItemId();

                if (itemId == R.id.nav_home) {
                    Toast.makeText(MainActivity.this, "Home", Toast.LENGTH_SHORT).show();
                    return true;
                } else if (itemId == R.id.nav_games) {
                    Toast.makeText(MainActivity.this, "Games", Toast.LENGTH_SHORT).show();
                    return true;
                } else if (itemId == R.id.nav_downloads) {
                    Toast.makeText(MainActivity.this, "Downloads", Toast.LENGTH_SHORT).show();
                    return true;
                } else if (itemId == R.id.nav_profile) {
                    getSupportFragmentManager()
                        .beginTransaction()
                        .replace(android.R.id.content, new ProfileFragment())
                        .addToBackStack(null)
                        .commit();
                    return true;
                }

                return false;
            }
        });
    }
}
