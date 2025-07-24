package io.mods;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.*;

import java.util.HashMap;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private static final int RC_SIGN_IN = 1002;
    private static final String PREF_NAME = "user_session";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PREMIUM = "premium";
    private static final String KEY_DOWNLOADS = "downloads";
    private static final String KEY_JOINED = "joined";

    private FirebaseAuth firebaseAuth;
    private GoogleSignInClient googleSignInClient;
    private SharedPreferences sharedPreferences;
    private TextView emailText, premiumText, downloadsText, joinedText;
    private Button logoutButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        emailText = view.findViewById(R.id.profile_email);
        premiumText = view.findViewById(R.id.profile_premium);
        downloadsText = view.findViewById(R.id.profile_downloads);
        joinedText = view.findViewById(R.id.profile_joined);
        logoutButton = view.findViewById(R.id.btn_logout);

        firebaseAuth = FirebaseAuth.getInstance();
        sharedPreferences = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso);

        logoutButton.setOnClickListener(v -> logoutUser());

        boolean isLoggedIn = sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
        FirebaseUser user = firebaseAuth.getCurrentUser();

        if (isLoggedIn && user != null) {
            updateUIFromPreferences();
        } else {
            signInWithGoogle();
        }

        return view;
    }

    private void signInWithGoogle() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN && data != null) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                Toast.makeText(getContext(), "Google Sign-In failed", Toast.LENGTH_SHORT).show();
                Log.e("PROFILE_SIGN_IN", "Failed with code: " + e.getStatusCode());
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(requireActivity(), task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null) {
                            createUserIfNotExists(user);
                        }
                    } else {
                        Toast.makeText(getContext(), "Authentication failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void createUserIfNotExists(FirebaseUser user) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = user.getUid();

        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("email", user.getEmail());
                        userData.put("uid", uid);
                        userData.put("premium", false);
                        userData.put("downloads", 0);
                        userData.put("createdAt", FieldValue.serverTimestamp());

                        db.collection("users").document(uid).set(userData)
                                .addOnSuccessListener(unused -> {
                                    sharedPreferences.edit()
                                            .putBoolean(KEY_IS_LOGGED_IN, true)
                                            .putString(KEY_EMAIL, user.getEmail())
                                            .putBoolean(KEY_PREMIUM, false)
                                            .putInt(KEY_DOWNLOADS, 0)
                                            .putString(KEY_JOINED, "New")
                                            .apply();
                                    updateUIFromPreferences();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(getContext(), "Failed to save user", Toast.LENGTH_SHORT).show();
                                    Log.e("FIRESTORE_CREATE", "Error", e);
                                });
                    } else {
                        sharedPreferences.edit()
                                .putBoolean(KEY_IS_LOGGED_IN, true)
                                .putString(KEY_EMAIL, user.getEmail())
                                .putBoolean(KEY_PREMIUM, documentSnapshot.getBoolean("premium") != null && documentSnapshot.getBoolean("premium"))
                                .putInt(KEY_DOWNLOADS, documentSnapshot.getLong("downloads") != null ? documentSnapshot.getLong("downloads").intValue() : 0)
                                .putString(KEY_JOINED, documentSnapshot.getDate("createdAt") != null ?
                                        documentSnapshot.getDate("createdAt").toString() : "Unknown")
                                .apply();
                        updateUIFromPreferences();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error checking user", Toast.LENGTH_SHORT).show();
                    Log.e("FIRESTORE_CHECK", "Error", e);
                });
    }

    private void updateUIFromPreferences() {
        String email = sharedPreferences.getString(KEY_EMAIL, "Unknown");
        boolean premium = sharedPreferences.getBoolean(KEY_PREMIUM, false);
        int downloads = sharedPreferences.getInt(KEY_DOWNLOADS, 0);
        String joined = sharedPreferences.getString(KEY_JOINED, "Unknown");

        emailText.setText("Email: " + email);
        premiumText.setText("Premium: " + premium);
        downloadsText.setText("Downloads: " + downloads);
        joinedText.setText("Joined: " + joined);
    }

    private void logoutUser() {
        firebaseAuth.signOut();
        googleSignInClient.signOut().addOnCompleteListener(requireActivity(), task -> {
            sharedPreferences.edit().clear().apply();
            Toast.makeText(getContext(), "Logged out", Toast.LENGTH_SHORT).show();
            signInWithGoogle(); 
        });
    }
}
