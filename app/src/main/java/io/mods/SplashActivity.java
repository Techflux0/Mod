package io.mods;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;

public class SplashActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        RelativeLayout splashLayout = findViewById(R.id.splashLayout);
        ImageView logo = findViewById(R.id.logo);

        AlphaAnimation fade = new AlphaAnimation(0.0f, 1.0f);
        fade.setDuration(2000);
        splashLayout.startAnimation(fade);

        Animation shake = new TranslateAnimation(-10, 10, -10, 10);
        shake.setDuration(500);
        shake.setRepeatMode(Animation.REVERSE);
        shake.setRepeatCount(5);
        logo.startAnimation(shake);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
                finish();
            }
        }, 3000);
    }
}
