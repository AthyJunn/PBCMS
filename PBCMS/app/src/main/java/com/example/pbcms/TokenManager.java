package com.example.pbcms;

import android.util.Log;

import androidx.annotation.NonNull;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.util.ArrayList;
import java.util.List;

public class TokenManager {

    private DatabaseReference tokensRef;

    public TokenManager() {
        tokensRef = FirebaseDatabase.getInstance().getReference("device_tokens");
    }

    public void saveToken(String token) {
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (userId != null) {
            // Save multiple tokens per user
            tokensRef.child(userId).child(token).setValue(true)
                    .addOnSuccessListener(aVoid -> Log.d("TokenManager", "Token saved successfully"))
                    .addOnFailureListener(e -> Log.d("TokenManager", "Failed to save token: " + e.getMessage()));
        }
    }


    public void getAllTokens(final TokensListener listener) {
        tokensRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<String> tokens = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String token = snapshot.getValue(String.class);
                    if (token != null) {
                        tokens.add(token);
                    }
                }
                listener.onTokensReceived(tokens);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                System.out.println("Failed to get tokens: " + databaseError.getMessage());
                listener.onError(databaseError.getMessage());
            }
        });
    }

    public void removeToken() {
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (userId != null) {
            tokensRef.child(userId).removeValue();
        }
    }

    public interface TokensListener {
        void onTokensReceived(List<String> tokens);
        void onError(String error);
    }
}