package com.example.socialdistancetracker.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.socialdistancetracker.R;

import java.util.List;

public class ListAdapter extends RecyclerView.Adapter<ListAdapter.ListViewHolder> {

    List<String> emailList;

    public ListAdapter(List<String> myDataset) {
        emailList = myDataset;
    }

    @NonNull
    @Override
    public ListAdapter.ListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.cardtext, parent, false);

        ListViewHolder vh = new ListViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull ListViewHolder holder, int position) {
        String video = emailList.get(position);
        holder.setDetails(video);
    }

    @Override
    public int getItemCount() {
        return emailList.size();
    }

    public static class ListViewHolder extends RecyclerView.ViewHolder {
        private TextView txtName;

        public ListViewHolder(View v) {
            super(v);
            txtName = v.findViewById(R.id.txtName);
        }

        void setDetails(String email) {
            txtName.setText(email);
        }
    }
}
