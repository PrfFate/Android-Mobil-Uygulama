package com.example.deussenger.Fragment;

import static android.app.Activity.RESULT_OK;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.deussenger.Model.Kullanici;
import com.example.deussenger.R;
import com.google.android.gms.auth.api.signin.internal.Storage;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfilFragment extends Fragment {
    private static final int IZIN_KODU = 0;
    private static final int IZIN_ALINDI_KODU = 1;

    private EditText editIsim,editEmail;
    private CircleImageView imgProfil;
    private ImageView imgyeniResim,imgSifirlaResim;
    private View v;

    private FirebaseFirestore mFirestore;
    private DocumentReference mRef;
    private FirebaseUser mUser;
    private Kullanici user;

    private Intent galeriIntent;
    private Uri mUri;
    private Bitmap gelenResim;
    private ImageDecoder.Source imgSource;
    private ByteArrayOutputStream outputStream;
    private byte[] imgByte;
    private StorageReference mStorageRef,yeniRef,sRef;
    private String kayitYeri,indirmeLinki;
    private HashMap<String, Object> mData;

    private Query mQuery;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        v = inflater.inflate(R.layout.fragment_profil, container, false);

        editIsim = v.findViewById(R.id.profil_fragment_editIsim);
        editEmail = v.findViewById(R.id.profil_fragment_editEmail);
        imgProfil = v.findViewById(R.id.profil_fragment_imgUserProfil);
        imgyeniResim = v.findViewById(R.id.profil_fragment_imgYeniResim);
        imgSifirlaResim = v.findViewById(R.id.profil_fragment_imgResmiSifirla);

        mUser = FirebaseAuth.getInstance().getCurrentUser();
        mFirestore = FirebaseFirestore.getInstance();
        mStorageRef = FirebaseStorage.getInstance().getReference();

        mRef = mFirestore.collection("Kullanıcılar").document(mUser.getUid());
        mRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error != null){
                    Toast.makeText(v.getContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }

                if (value != null && value.exists()){
                    user = value.toObject(Kullanici.class);

                    if(user != null){
                        editIsim.setText(user.getKullaniciIsmi());
                        editEmail.setText(user.getKullaniciEmail());

                        if (user.getKullaniciProfil().equals("default"))
                            imgProfil.setImageResource(R.drawable.ic_person);
                        else
                            Picasso.get().load(user.getKullaniciProfil()).resize(156,156).into(imgProfil);
                    }
                }
            }
        });

        imgyeniResim.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES}, IZIN_KODU);
                    } else {
                        galeriyeGit();
                    }
                } else if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, IZIN_KODU);
                } else {
                    galeriyeGit();
                }


            }
        });

        imgSifirlaResim.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!user.getKullaniciProfil().equals("default")) {
                    // Firebase Storage'dan mevcut resmi sil
                    StorageReference eskiResimRef = FirebaseStorage.getInstance().getReferenceFromUrl(user.getKullaniciProfil());
                    eskiResimRef.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                // Varsayılan resim URL'sini güncelle
                                mData = new HashMap<>();
                                mData.put("kullaniciProfil", "default");

                                mRef.update(mData).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            imgProfil.setImageResource(R.drawable.ic_person);
                                            Toast.makeText(v.getContext(), "Profil fotoğrafı sıfırlandı.", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(v.getContext(), "Profil güncellenirken hata oluştu: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                            } else {
                                Toast.makeText(v.getContext(), "Resim silinirken hata oluştu: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } else {
                    Toast.makeText(v.getContext(), "Zaten Profil Fotoğrafınız Yok.", Toast.LENGTH_SHORT).show();
                }
            }
        });


        return v;
    }



    private void galeriyeGit(){
        galeriIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galeriIntent, IZIN_ALINDI_KODU);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == IZIN_KODU) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                galeriyeGit();
            } else {
                Toast.makeText(v.getContext(), "Galeriye erişim izni verilmedi.", Toast.LENGTH_SHORT).show();
            }
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == IZIN_ALINDI_KODU){
            if (resultCode == RESULT_OK && data != null && data.getData() != null){
                mUri = data.getData();

                try {
                    if (Build.VERSION.SDK_INT >= 28){
                        imgSource = ImageDecoder.createSource(v.getContext().getContentResolver(), mUri);
                        gelenResim = ImageDecoder.decodeBitmap(imgSource);
                    }else{
                        gelenResim = MediaStore.Images.Media.getBitmap(v.getContext().getContentResolver(), mUri);
                    }

                    outputStream = new ByteArrayOutputStream();
                    gelenResim.compress(Bitmap.CompressFormat.PNG, 75, outputStream);
                    imgByte = outputStream.toByteArray();

                    kayitYeri = "Kullanıcılar/" + user.getKullaniciEmail() + "/profil.png";
                    sRef = mStorageRef.child(kayitYeri);
                    sRef.putBytes(imgByte)
                            .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                    yeniRef = FirebaseStorage.getInstance().getReference(kayitYeri);
                                    yeniRef.getDownloadUrl()
                                            .addOnSuccessListener(new OnSuccessListener<Uri>() {
                                                @Override
                                                public void onSuccess(Uri uri) {
                                                    indirmeLinki = uri.toString();
                                                    mData = new HashMap<>();
                                                    mData.put("kullaniciProfil", indirmeLinki);

                                                    mFirestore.collection("Kullanıcılar").document(mUser.getUid())
                                                            .update(mData)
                                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                @Override
                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                    if (task.isSuccessful()){
                                                                        iletisimIcinProfilGuncelle(indirmeLinki);
                                                                    }else
                                                                        Toast.makeText(v.getContext(), task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                                                }
                                                            });
                                                }
                                            }).addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    Toast.makeText(v.getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(v.getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void iletisimIcinProfilGuncelle(final String link){
        mQuery = mFirestore.collection("Kullanıcılar").document(mUser.getUid()).collection("Kanal");
        mQuery.get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        if(queryDocumentSnapshots.getDocuments().size() > 0){
                            for (DocumentSnapshot snp : queryDocumentSnapshots.getDocuments()){
                                mData = new HashMap<>();
                                mData.put("kullaniciProfil", link);

                                mFirestore.collection("Kullanıcılar").document(snp.getData().get("kullaniciId").toString()).collection("Kanal").document(mUser.getUid())
                                        .update(mData);
                            }

                            Toast.makeText(v.getContext(), "Profil Resminiz Başarıyla Güncellendi", Toast.LENGTH_SHORT).show();
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(v.getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}