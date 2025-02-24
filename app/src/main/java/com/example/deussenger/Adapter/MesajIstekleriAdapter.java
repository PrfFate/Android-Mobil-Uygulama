package com.example.deussenger.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.deussenger.Model.MesajIstegi;
import com.example.deussenger.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

public class MesajIstekleriAdapter extends RecyclerView.Adapter<MesajIstekleriAdapter.MesajIstekleriHolder> {
    private ArrayList<MesajIstegi> mMesajIstegiList;
    private Context context;
    private MesajIstegi mesajIstegi, yeniMesajIstegi;
    private View v;
    private int mPos;
    private String mUId, mIsim, mProfilUrl;

    private FirebaseFirestore mFirestore;

    public MesajIstekleriAdapter(ArrayList<MesajIstegi> mMesajIstegiList, Context context,String mUId,String mIsim,String mProfilUrl) {
        this.mMesajIstegiList = mMesajIstegiList;
        this.context = context;
        this.mUId=mUId;
        this.mIsim=mIsim;
        this.mProfilUrl=mProfilUrl;
        mFirestore = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public MesajIstekleriHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        v = LayoutInflater.from(context).inflate(R.layout.gelen_mesaj_istekleri__item,parent,false);
        return new MesajIstekleriHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MesajIstekleriHolder holder, int position) {
        mesajIstegi=mMesajIstegiList.get(position);
        holder.txtMesaj.setText(mesajIstegi.getKullaniciIsim() + " Kullanıcısı Sana Bir Mesaj Göndermek İstiyor");

        if (mesajIstegi.getKullaniciProfil().equals("default"))
            holder.imgProfil.setImageResource(R.drawable.ic_person);
        else
            Picasso.get().load(mesajIstegi.getKullaniciProfil()).resize(77,77).into(holder.imgProfil);

        holder.imgOnay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPos = holder.getAdapterPosition();

                if (mPos != RecyclerView.NO_POSITION){
                    yeniMesajIstegi = new MesajIstegi(mMesajIstegiList.get(mPos).getKanalId(),mMesajIstegiList.get(mPos).getKullaniciId(),mMesajIstegiList.get(mPos).getKullaniciIsim(),mMesajIstegiList.get(mPos).getKullaniciProfil());

                    mFirestore.collection("Kullanıcılar").document(mUId).collection("Kanal").document(mMesajIstegiList.get(mPos).getKullaniciId())
                            .set(yeniMesajIstegi)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()){
                                        yeniMesajIstegi = new MesajIstegi(mMesajIstegiList.get(mPos).getKanalId(),mUId,mIsim,mProfilUrl);

                                        mFirestore.collection("Kullanıcılar").document(mMesajIstegiList.get(mPos).getKullaniciId()).collection("Kanal").document(mUId)
                                                .set(yeniMesajIstegi)
                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        if (task.isSuccessful())
                                                            mesajIsteginiSil(mMesajIstegiList.get(mPos).getKullaniciId(), "Mesaj İsteği Başarıyla Kabul Edildi.");
                                                        else
                                                            Toast.makeText(context, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                    }else
                                        Toast.makeText(context, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                }
            }
        });

        holder.imgIptal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPos = holder.getAdapterPosition();

                if (mPos != RecyclerView.NO_POSITION)
                    mesajIsteginiSil(mMesajIstegiList.get(mPos).getKullaniciId(), "Mesaj İsteği Başarıyla Reddedildi.");

            }
        });
    }

    @Override
    public int getItemCount() {
        return mMesajIstegiList.size();
    }

    class MesajIstekleriHolder extends RecyclerView.ViewHolder{
        CircleImageView imgProfil;
        TextView txtMesaj;
        ImageView imgIptal,imgOnay;


        public MesajIstekleriHolder(@NonNull View itemView) {
            super(itemView);

            imgProfil = itemView.findViewById(R.id.gelen_mesaj_istekleri_item_imgProfil);
            txtMesaj = itemView.findViewById(R.id.gelen_mesaj_istekleri_item_txtMesaj);
            imgIptal = itemView.findViewById(R.id.gelen_mesaj_istekleri_item_imgIptal);
            imgOnay = itemView.findViewById(R.id.gelen_mesaj_istekleri_item_imgOnayla);
        }
    }

    private void mesajIsteginiSil(String hedefUUID, final String mesajIcerigi){
        mFirestore.collection("Mesajİstekleri").document(mUId).collection("İstekler").document(hedefUUID)
                .delete()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()){
                            notifyDataSetChanged();
                            Toast.makeText(context, mesajIcerigi, Toast.LENGTH_SHORT).show();
                        }
                        else
                            Toast.makeText(context, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
