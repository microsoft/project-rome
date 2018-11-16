//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

package com.microsoft.connecteddevices.nearshare;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.microsoft.connecteddevices.remotesystems.RemoteSystem;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter class for populating the listView of devices.
 */
public class DeviceListAdapter extends BaseAdapter {
    private List<RemoteSystem> mDevices = new ArrayList<>();
    private Context mContext;
    private View mSelectedView = null;

    public DeviceListAdapter(Context mContext) {
        this.mContext = mContext;
    }

    /**
     * How many items are in the data set represented by this Adapter.
     *
     * @return Count of items.
     */
    @Override
    public int getCount() {
        return mDevices.size();
    }

    /**
     * Get the data item associated with the specified position in the data set.
     *
     * @param position Position of the item whose data we want within the adapter's
     *                 data set.
     * @return The data at the specified position.
     */
    @Override
    public Object getItem(int position) {
        if (position < mDevices.size() && 0 <= position) {
            return mDevices.get(position);
        }

        return null;
    }

    /**
     * Get the row id associated with the specified position in the list.
     *
     * @param position The position of the item within the adapter's data set whose row id we want.
     * @return The id of the item at the specified position.
     */
    @Override
    public long getItemId(int position) {
        if (0 <= position && mDevices.size() >= position) {
            return position;
        }

        return 0;
    }

    /**
     * Get a View that displays the data at the specified position in the data set. You can either
     * create a View manually or inflate it from an XML layout file. When the View is inflated, the
     * parent View (GridView, ListView...) will apply default layout parameters unless you use
     * {@link LayoutInflater#inflate(int, ViewGroup, boolean)}
     * to specify a root view and to prevent attachment to the root.
     *
     * @param position    The position of the item within the adapter's data set of the item whose view
     *                    we want.
     * @param convertView The old view to reuse, if possible. Note: You should check that this view
     *                    is non-null and of an appropriate type before using. If it is not possible to convert
     *                    this view to display the correct data, this method can create a new view.
     *                    Heterogeneous lists can specify their number of view types, so that this View is
     *                    always of the right type (see {@link #getViewTypeCount()} and
     *                    {@link #getItemViewType(int)}).
     * @param parent      The parent that this view will eventually be attached to
     * @return A View corresponding to the data at the specified position.
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View listItemView = null;

        if (null == convertView) {
            LayoutInflater layoutInflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            listItemView = layoutInflater.inflate(R.layout.discovered_device_list_item, null);
        } else {
            listItemView = convertView;
        }

        TextView deviceNameText = listItemView.findViewById(R.id.txtDeviceName);
        TextView deviceTypeText = listItemView.findViewById(R.id.txtDeviceType);

        RemoteSystem entry = mDevices.get(position);

        if (null != entry) {
            deviceNameText.setText(entry.getDisplayName());
            deviceTypeText.setText(entry.getKind());
        }

        return listItemView;
    }

    public void addDevice(RemoteSystem remoteSystem) {
        mDevices.add(remoteSystem);
    }

    public void removeDevice(RemoteSystem remoteSystem) {
        for (RemoteSystem entry : mDevices) {
            if (remoteSystem == entry) {
                mDevices.remove(entry);
            }
        }
    }

    /**
    Set the view background when the view is selected.
     */

    public void setSelectedView(View view) {
        if (null != mSelectedView) {
            mSelectedView.setBackgroundResource(R.drawable.deselected_list_item);
        }

        mSelectedView = view;
        mSelectedView.setBackgroundResource(R.drawable.selected_list_item);
    }

    public void clear() {
        mDevices.clear();
    }
}
