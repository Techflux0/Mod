package io.mods;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private final String ADMIN_UID = "isFpWLV57ONA9TpXmMvNN9I78283";

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
                } else if (itemId == R.id.nav_upload) {
                    Toast.makeText(MainActivity.this, "Upload", Toast.LENGTH_SHORT).show();
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
            }
        });
    }
}
