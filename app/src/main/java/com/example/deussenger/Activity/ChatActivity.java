package com.example.deussenger.Activity;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.deussenger.Adapter.ChatAdapter;
import com.example.deussenger.Model.Chat;
import com.example.deussenger.Model.Kullanici;
import com.example.deussenger.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity {
    private static final int IZIN_KODU = 0;
    private static final int IZIN_ALINDI_KODU = 1;

    private ProgressDialog mProgress;
    private FirebaseUser mUser;
    private HashMap<String,Object> mData;

    private RecyclerView mRecyclerView;
    private EditText editMesaj;
    private String txtMesaj,docId;
    private CircleImageView hedefProfil;
    private TextView hedefIsim;
    private Intent gelenIntent,galeriyeGit;
    private String hedefId,kanalId,hedefProfilUrl;
    private DocumentReference hedefRef;
    private Kullanici hedefKullanici;
    private FirebaseFirestore mFireStore;

    private Query chatQuery;
    private ArrayList<Chat> mChatList;
    private Chat mChat;
    private ChatAdapter chatAdapter;

    private LinearLayoutManager mManager;
    private Uri imgUri;
    private String kayitYeri, indirmeLinki;
    private Bitmap imgBitmap;
    private ByteArrayOutputStream outputStream;
    private byte[] imgByte;
    private ImageDecoder.Source imgSource;
    private StorageReference mStorageRef, yeniRef, sRef;

    private void init(){
        mRecyclerView = (RecyclerView) findViewById(R.id.chat_activity_recyclerView);
        editMesaj = (EditText) findViewById(R.id.chat_activity_editMesaj);
        hedefProfil = (CircleImageView) findViewById(R.id.chat_activity_imgHedefProfil);
        hedefIsim = (TextView) findViewById(R.id.chat_activity_txtHedefIsim);

        mStorageRef = FirebaseStorage.getInstance().getReference();
        mProgress = new ProgressDialog(ChatActivity.this);

        mUser = FirebaseAuth.getInstance().getCurrentUser();
        mFireStore = FirebaseFirestore.getInstance();
        gelenIntent = getIntent();
        hedefId = gelenIntent.getStringExtra("hedefId");
        kanalId = gelenIntent.getStringExtra("kanalId");
        hedefProfilUrl = gelenIntent.getStringExtra("hedefProfil");

        mChatList = new ArrayList<>();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chat);
        init();

        hedefRef = mFireStore.collection("Kullanıcılar").document(hedefId);
        hedefRef.addSnapshotListener(this, new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error != null){
                    Toast.makeText(ChatActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }

                if (value != null && value.exists()){
                    hedefKullanici = value.toObject(Kullanici.class);

                    if (hedefKullanici != null){
                        hedefIsim.setText(hedefKullanici.getKullaniciIsmi());

                        if (hedefKullanici.getKullaniciProfil().equals("default"))
                            hedefProfil.setImageResource(R.drawable.ic_person);
                        else
                            Picasso.get().load(hedefKullanici.getKullaniciProfil()).resize(76,76).into(hedefProfil);
                    }
                }
            }
        });

        mRecyclerView.setHasFixedSize(true);
        mManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mManager.setStackFromEnd(true);
        mRecyclerView.setLayoutManager(mManager);

        chatQuery = mFireStore.collection("ChatKanalları").document(kanalId).collection("Mesajlar")
                .orderBy("mesajTarihi", Query.Direction.ASCENDING);
        chatQuery.addSnapshotListener(this, (value, error) -> {
            if (error != null){
                Toast.makeText(ChatActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }

            if (value != null){
                mChatList.clear();

                for (DocumentSnapshot snapshot : value.getDocuments()){
                    mChat=snapshot.toObject(Chat.class);

                    if (mChat != null)
                        mChatList.add(mChat);
                }

                chatAdapter = new ChatAdapter(mChatList , ChatActivity.this , mUser.getUid(), hedefProfilUrl);
                mRecyclerView.setAdapter(chatAdapter);
            }
        });
    }

    public void btnMesajGonder(View v){
        txtMesaj= editMesaj.getText().toString();

        if (!TextUtils.isEmpty(txtMesaj)){
            mesajGonder(txtMesaj, "text");
        }else
            Toast.makeText(ChatActivity.this, "Mesaj Göndermek İçin Bir Şeyler Yazın.", Toast.LENGTH_SHORT).show();
    }

    public void btnChatKapat(View v){
        finish();
    }

    public void btnGaleridenResimGonder(View v){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(ChatActivity.this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES}, IZIN_KODU);
            } else {
                galeriIntent();
            }
        } else if (ContextCompat.checkSelfPermission(ChatActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, IZIN_KODU);
        } else {
            galeriIntent();
        }
    }

    private void galeriIntent(){
        galeriyeGit = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galeriyeGit, IZIN_ALINDI_KODU);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == IZIN_KODU){
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                galeriIntent();
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode == IZIN_ALINDI_KODU){
            if (resultCode == RESULT_OK && data != null && data.getData() != null){
                imgUri = data.getData();

                try {
                    if (Build.VERSION.SDK_INT >= 28){
                        imgSource = ImageDecoder.createSource(this.getContentResolver(), imgUri);
                        imgBitmap = ImageDecoder.decodeBitmap(imgSource);
                    }else{
                        imgBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imgUri);
                    }

                    outputStream = new ByteArrayOutputStream();
                    imgBitmap.compress(Bitmap.CompressFormat.PNG, 75, outputStream);
                    imgByte = outputStream.toByteArray();

                    kayitYeri = "ChatYuklenenler/" + kanalId + "/" + mUser.getUid() + "/" + System.currentTimeMillis() + ".png";
                    sRef = mStorageRef.child(kayitYeri);
                    sRef.putBytes(imgByte)
                            .addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                    mProgress.setTitle("Resim Gönderiliyor...");
                                    mProgress.show();

                                    yeniRef = FirebaseStorage.getInstance().getReference(kayitYeri);
                                    yeniRef.getDownloadUrl()
                                            .addOnSuccessListener(ChatActivity.this, new OnSuccessListener<Uri>() {
                                                @Override
                                                public void onSuccess(Uri uri) {
                                                    indirmeLinki = uri.toString();

                                                    mesajGonder(indirmeLinki, "resim");
                                                }
                                            }).addOnFailureListener(ChatActivity.this, new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    e.printStackTrace();
                                                    progressAyari();
                                                }
                                            });
                                }
                            }).addOnFailureListener(this, new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    e.printStackTrace();
                                }
                            });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void mesajGonder(String txtMesaj, String mesajTipi){
        docId = UUID.randomUUID().toString();

        mData = new HashMap<>();
        mData.put("mesajIcerigi" ,txtMesaj);
        mData.put("gonderen" ,mUser.getUid());
        mData.put("alici" ,hedefId);
        mData.put("mesajTipi",mesajTipi);
        mData.put("mesajTarihi" , FieldValue.serverTimestamp());
        mData.put("docId",docId);

        mFireStore.collection("ChatKanalları").document(kanalId).collection("Mesajlar").document(docId)
                .set(mData)
                .addOnCompleteListener(ChatActivity.this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()){
                            editMesaj.setText("");

                            progressAyari();
                        }else
                            Toast.makeText(ChatActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void progressAyari(){
        if (mProgress.isShowing())
            mProgress.dismiss();
    }

    private void kullaniciSetOnline(Boolean b){
        mData = new HashMap<>();
        mData.put("kullaniciOnline", b);
        mFireStore.collection("Kullanıcılar").document(mUser.getUid())
                .update(mData);
    }

    @Override
    protected void onResume() {
        super.onResume();
        kullaniciSetOnline(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        kullaniciSetOnline(false);
    }
}