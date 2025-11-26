// CargoAdapter.java
package com.example.cargofit;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

//public class CargoAdapter extends RecyclerView.Adapter<CargoAdapter.CargoViewHolder> {
//
//    private List<CargoItem> cargoList;
//
//    public CargoAdapter(List<CargoItem> cargoList) {
//        this.cargoList = cargoList;
//    }
//
//    @NonNull
//    @Override
//    public CargoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//        View view = LayoutInflater.from(parent.getContext())
//                .inflate(R.layout.item_cargo, parent, false);
//        return new CargoViewHolder(view);
//    }
//
//    @Override
//    public void onBindViewHolder(@NonNull CargoViewHolder holder, int position) {
//        CargoItem item = cargoList.get(position);
//        holder.tvProduct.setText(item.getProductName());
//        holder.tvQty.setText("Qty: " + item.getQuantity());
//        holder.tvWeight.setText("Weight: " + item.getWeight() + " kg");
//    }
//
//    @Override
//    public int getItemCount() {
//        return cargoList.size();
//    }

    //static class CargoViewHolder extends RecyclerView.ViewHolder {
//        TextView tvProduct, tvQty, tvWeight;

//        public CargoViewHolder(@NonNull View itemView) {
//            super(itemView);
//            tvProduct = itemView.findViewById(R.id.tv_product_name);
//            tvQty = itemView.findViewById(R.id.tv_quantity);
//            tvWeight = itemView.findViewById(R.id.tv_weight);
//        }
//  }
//}
