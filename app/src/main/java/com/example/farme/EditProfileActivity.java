package com.example.farme;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.*;
import androidx.activity.result.*;
import androidx.activity.result.contract.*;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.io.*;
import java.util.*;

public class EditProfileActivity extends AppCompatActivity {

    private TextView  tvAvatarInitial;
    private ImageView ivAvatar;
    private EditText  etName, etPhone, etRegion;
    private Spinner   spinnerRegion;
    private View      btnSave, btnChangeAvatar;
    private ProgressBar pbSave;

    private DatabaseReference mDatabase;
    private String myUid;

    private static final String[] REGIONS = {
            "Чуйская область","Иссык-Кульская область","Ошская область",
            "Джалал-Абадская область","Нарынская область","Баткенская область",
            "Таласская область","г. Бишкек","г. Ош"
    };

    private final ActivityResultLauncher<String> pickImage =
            registerForActivityResult(
                    new ActivityResultContracts.GetContent(),
                    uri -> { if (uri != null) processAvatar(uri); });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);
        if (FirebaseAuth.getInstance().getCurrentUser() == null) { finish(); return; }
        myUid     = FirebaseAuth.getInstance().getCurrentUser().getUid();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        initViews();
        loadProfile();
    }

    private void initViews() {
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        tvAvatarInitial = findViewById(R.id.tvAvatarInitials);
        ivAvatar        = findViewById(R.id.ivAvatar);
        etName          = findViewById(R.id.etName);
        etPhone         = findViewById(R.id.etPhone);
        spinnerRegion   = findViewById(R.id.spinnerRegion);
        btnSave         = findViewById(R.id.btnSave);
        btnChangeAvatar = findViewById(R.id.btnChangeAvatar);
        pbSave          = findViewById(R.id.pbSave);

        if (spinnerRegion != null) {
            ArrayAdapter<String> a = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, REGIONS);
            a.setDropDownViewResource(
                    android.R.layout.simple_spinner_dropdown_item);
            spinnerRegion.setAdapter(a);
        }
        if (btnChangeAvatar != null)
            btnChangeAvatar.setOnClickListener(v ->
                    pickImage.launch("image/*"));
        if (btnSave != null)
            btnSave.setOnClickListener(v -> saveProfile());
    }

    private void loadProfile() {
        mDatabase.child("users").child(myUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snap) {
                        String name   = snap.child("name").getValue(String.class);
                        String phone  = snap.child("phone").getValue(String.class);
                        String region = snap.child("region").getValue(String.class);
                        String avatar = snap.child("avatar").getValue(String.class);

                        if (etName  != null && name  != null) etName.setText(name);
                        if (etPhone != null && phone != null) etPhone.setText(phone);

                        if (spinnerRegion != null && region != null) {
                            for (int i = 0; i < REGIONS.length; i++)
                                if (REGIONS[i].equals(region)) {
                                    spinnerRegion.setSelection(i); break;
                                }
                        }
                        if (tvAvatarInitial != null && name != null && !name.isEmpty())
                            tvAvatarInitial.setText(
                                    String.valueOf(name.charAt(0)).toUpperCase());

                        if (avatar != null && !avatar.isEmpty() && ivAvatar != null) {
                            try {
                                String data = avatar.contains(",")
                                        ? avatar.substring(avatar.indexOf(",") + 1) : avatar;
                                byte[] bytes = Base64.decode(data, Base64.DEFAULT);
                                ivAvatar.setVisibility(View.VISIBLE);
                                if (tvAvatarInitial != null)
                                    tvAvatarInitial.setVisibility(View.GONE);
                                Glide.with(EditProfileActivity.this)
                                        .load(bytes).circleCrop().into(ivAvatar);
                            } catch (Exception ignored) {}
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    private void saveProfile() {
        String name = etName  != null ? etName.getText().toString().trim() : "";
        String region = spinnerRegion != null && spinnerRegion.getSelectedItem() != null
                ? spinnerRegion.getSelectedItem().toString() : "";

        if (name.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_enter_name), Toast.LENGTH_SHORT).show();
            return;
        }
        if (pbSave != null) pbSave.setVisibility(View.VISIBLE);

        Map<String, Object> updates = new HashMap<>();
        updates.put("name",   name);
        updates.put("region", region);

        mDatabase.child("users").child(myUid).updateChildren(updates)
                .addOnSuccessListener(x -> {
                    if (pbSave != null) pbSave.setVisibility(View.GONE);
                    Toast.makeText(this, getString(R.string.profile_saved),
                            Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    if (pbSave != null) pbSave.setVisibility(View.GONE);
                    Toast.makeText(this, getString(R.string.error_save),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void processAvatar(Uri uri) {
        new Thread(() -> {
            try {
                InputStream is = getContentResolver().openInputStream(uri);
                Bitmap bmp = BitmapFactory.decodeStream(is);
                // Масштабируем
                int size = Math.min(bmp.getWidth(), bmp.getHeight());
                bmp = Bitmap.createBitmap(bmp,
                        (bmp.getWidth()-size)/2, (bmp.getHeight()-size)/2,
                        size, size);
                bmp = Bitmap.createScaledBitmap(bmp, 200, 200, true);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.JPEG, 75, baos);
                String b64 = "data:image/jpeg;base64,"
                        + Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);

                runOnUiThread(() -> {
                    mDatabase.child("users").child(myUid)
                            .child("avatar").setValue(b64)
                            .addOnSuccessListener(x -> {
                                if (ivAvatar != null) {
                                    ivAvatar.setVisibility(View.VISIBLE);
                                    if (tvAvatarInitial != null)
                                        tvAvatarInitial.setVisibility(View.GONE);
                                    Glide.with(this).load(uri)
                                            .circleCrop().into(ivAvatar);
                                }
                                Toast.makeText(this, getString(R.string.photo_updated),
                                        Toast.LENGTH_SHORT).show();
                            });
                });
            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, getString(R.string.error_photo_load),
                                Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}