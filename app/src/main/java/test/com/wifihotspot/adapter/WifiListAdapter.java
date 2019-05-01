package test.com.wifihotspot.adapter;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import test.com.wifihotspot.R;

/**
 * Created by hu.qinghui on 2019/4/30.
 */

public class WifiListAdapter extends ArrayAdapter<ScanResult> {

    private final LayoutInflater mInflater;
    private int mResource;

    public WifiListAdapter(Context context, int resource) {
        super(context, resource);
        mInflater = LayoutInflater.from(context);
        mResource = resource;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = mInflater.inflate(mResource, parent, false);
        }

        TextView name = (TextView) convertView.findViewById(R.id.wifi_name);
        TextView signal = (TextView) convertView.findViewById(R.id.wifi_signal);

        ScanResult scanResult = getItem(position);
        name.setText(scanResult.SSID);

        int level = scanResult.level;
        if (level <= 0 && level >= -50) {
            signal.setText("信号很好");
        } else if (level < -50 && level >= -70) {
            signal.setText("信号较好");
        } else if (level < -70 && level >= -80) {
            signal.setText("信号一般");
        } else if (level < -80 && level >= -100) {
            signal.setText("信号较差");
        } else {
            signal.setText("信号很差");
        }
        return convertView;
    }
}
