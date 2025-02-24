package com.example.deussenger.Fragment;

import android.graphics.Rect;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.deussenger.Adapter.KullaniciAdapter;
import com.example.deussenger.Model.Kullanici;
import com.example.deussenger.R;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

public class KullanicilarFragment extends Fragment {

    private RecyclerView mRecyclerView;
    private View v;
    private KullaniciAdapter mAdapter;
    private ArrayList<Kullanici> mKullanicList;
    private Kullanici mKullanici;

    private FirebaseUser mUser;

    private DocumentReference mRef;
    private Kullanici kullanici;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        v = inflater.inflate(R.layout.fragment_kullanicilar, container, false);
        mUser = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseFirestore mFireStore = FirebaseFirestore.getInstance();

        mKullanicList=new ArrayList<>();

        mRecyclerView =v.findViewById(R.id.kullanicilar_fragment_recyclerView);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(v.getContext(), LinearLayoutManager.VERTICAL,false));


        mRef = mFireStore.collection("Kullanıcılar").document(mUser.getUid());
        mRef.get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists()) {
                            kullanici = documentSnapshot.toObject(Kullanici.class);

                            Query mQuery = mFireStore.collection("Kullanıcılar");
                            mQuery.addSnapshotListener((value, error) -> {
                                if(error!=null){
                                    Toast.makeText(v.getContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                if (value!=null){
                                    mKullanicList.clear();

                                    for (DocumentSnapshot snapshot: value.getDocuments()){
                                        mKullanici=snapshot.toObject(Kullanici.class);

                                        assert mKullanici!=null;
                                        if (!mKullanici.getKullaniciId().equals(mUser.getUid()))
                                            mKullanicList.add(mKullanici);
                                    }
                                    if (mRecyclerView.getItemDecorationCount() > 0)
                                        mRecyclerView.removeItemDecorationAt(0);

                                    mRecyclerView.addItemDecoration(new LinearDecoration(20, mKullanicList.size()));
                                    mAdapter = new KullaniciAdapter(mKullanicList,v.getContext(),kullanici.getKullaniciId(),kullanici.getKullaniciIsmi(),kullanici.getKullaniciProfil());
                                    mRecyclerView.setAdapter(mAdapter);
                                }
                            });
                        }
                    }
                });
        return v;
    }

    static class LinearDecoration extends RecyclerView.ItemDecoration{
        private final int boslukMiktari;
        private final int veriSayisi;

        public LinearDecoration(int boslukMiktari, int veriSayisi) {
            this.boslukMiktari = boslukMiktari;
            this.veriSayisi = veriSayisi;
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            int pos = parent.getChildAdapterPosition(view);

            if (pos!=(veriSayisi-1)){
                outRect.bottom=boslukMiktari;
            }
        }
    }
}