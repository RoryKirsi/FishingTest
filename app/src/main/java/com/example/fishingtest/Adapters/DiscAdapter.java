package com.example.fishingtest.Adapters;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.fishingtest.R;

import java.util.ArrayList;

public class DiscAdapter extends RecyclerView.Adapter<DiscAdapter.ImageViewHolder>{

    private ArrayList<Integer> comps;



    public  DiscAdapter(ArrayList<Integer> comps){
        this.comps = comps;

    }

    // New class addressing each "Competition" item view in the list
    public static class ImageViewHolder extends RecyclerView.ViewHolder{

        ImageView compImage;
        TextView compTittle;


        public ImageViewHolder(View itemView){
            super(itemView);
            compImage = itemView.findViewById(R.id.comp_image);
            compTittle = itemView.findViewById(R.id.comp_title);
        }
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {

        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.layout_comp_item,viewGroup,false);
        ImageViewHolder imageViewHolder = new ImageViewHolder(view);


        return imageViewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder viewHolder, int i) {
        int image_id = comps.get(i);
        viewHolder.compImage.setImageResource(image_id);
        viewHolder.compTittle.setText("Recommendation: " + i); // TODO: Might need to modify according to UI design

    }


    @Override
    public int getItemCount() {
        return comps.size();
    }



}