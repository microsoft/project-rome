//
// Copyright (c) Microsoft Corporation. All rights reserved.
//

package com.microsoft.romanapp;

import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

public class DeviceRecyclerAdapter extends RecyclerView.Adapter<DeviceRecyclerAdapter.DeviceViewHolder> {

    private static final String TAG = DeviceRecyclerAdapter.class.getName();

    private static ClickListener clickListener;
    List<Device> devices;

    public static class DeviceViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {

        CardView cv;
        TextView deviceName;
        TextView deviceType;
        TextView deviceTransports;

        DeviceViewHolder(View itemView) {
            super(itemView);

            cv = (CardView) itemView.findViewById(R.id.device_card);
            deviceName = (TextView) itemView.findViewById(R.id.device_name);
            deviceType = (TextView) itemView.findViewById(R.id.device_type);
            deviceTransports = (TextView) itemView.findViewById(R.id.device_transports);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            clickListener.onItemClick(getAdapterPosition(), v);
        }

    }

    public interface ClickListener {
        void onItemClick(int position, View v);
    }

    DeviceRecyclerAdapter(List<Device> devices) {
        this.devices = devices;
    }

    public void setOnItemClickListener(ClickListener clickListener) {
        this.clickListener = clickListener;
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    @Override
    public DeviceViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).
                inflate(R.layout.device_card, viewGroup, false);
        DeviceViewHolder dvh = new DeviceViewHolder(v);
        return dvh;
    }

    @Override
    public void onBindViewHolder(DeviceViewHolder deviceViewHolder, int i) {
        deviceViewHolder.deviceName.setText(devices.get(i).getName());
        deviceViewHolder.deviceType.setText(devices.get(i).getType());

        String transports = "Cloud";
        transports += devices.get(i).getIsAvailableByProximity() ? ", Proximal" : "";
        deviceViewHolder.deviceTransports.setText(transports);
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }
}
