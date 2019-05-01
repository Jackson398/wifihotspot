package test.com.wifihotspot;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import test.com.wifihotspot.adapter.WifiListAdapter;
import test.com.wifihotspot.thread.ConnectThread;
import test.com.wifihotspot.thread.ListenerThread;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";

    private ListView mList;
    private Button btn_search;
    private Button btn_send;
    private TextView textview;
    private TextView text_state;
    private EditText editText;

    private WifiManager wifiManager;
    private WifiListAdapter wifiListAdapter;
    private WifiConfiguration config;
    private int wcgID;

    //hotspot name of the device
    private static final String WIFI_HOTSPOT_SSID = "device";

    //the device port
    private static final int PORT = 8888;

    private static final int WIFICIPHER_NOPASS = 1;
    private static final int WIFICIPHER_WEP = 2;
    private static final int WIFICIPHER_WPA = 3;

    public static final int DEVICE_CONNECTING = 1; //there has device try to connect the hotspot.
    public static final int DEVICE_CONNECTED = 2;//there has device connected the hotspot.
    public static final int SEND_MSG_SUCCESS = 3;//send the message successfully.
    public static final int SEND_MSG_ERROR = 4;//send the message failed.
    public static final int GET_MSG = 6;//get the new message.

    //connect thread
    private ConnectThread connectThread;

    //listenThread
    private ListenerThread listenerThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setPermission();

        initView();
        initBroadcastReceiver();
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        listenerThread = new ListenerThread(PORT, mHandler);
        listenerThread.start();
    }

    private void initBroadcastReceiver() {
        IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
//        intentFilter.addAction(WifiManager.RSSI_CHANGED_ACTION);

        registerReceiver(receiver, intentFilter);
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case DEVICE_CONNECTING:
                    connectThread = new ConnectThread(listenerThread.getSocket(), mHandler);
                    connectThread.start();
                    break;
                case DEVICE_CONNECTED:
                    textview.setText("设备连接成功");
                    break;
                case SEND_MSG_SUCCESS:
                    textview.setText("发送消息成功:" + msg.getData().getString("MSG"));
                    break;
                case SEND_MSG_ERROR:
                    textview.setText("发送消息失败:" + msg.getData().getString("MSG"));
                    break;
                case GET_MSG:
                    textview.setText("收到消息:" + msg.getData().getString("MSG"));
                    break;
            }
        }
    };

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                Log.d(TAG, "receiver action = SCAN_RESULTS_AVAILABLE_ACTION.");
                List<ScanResult> scanResults = wifiManager.getScanResults();
                wifiListAdapter.clear();
                wifiListAdapter.addAll(scanResults);
            } else if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                Log.d(TAG, "receiver action = WIFI_STATE_CHANGED_ACTION");
                int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0);
                switch (wifiState) {
                    case WifiManager.WIFI_STATE_ENABLED:
                        //获取到wifi开启的广播时，开始扫描
                        wifiManager.startScan();
                        break;
                    case WifiManager.WIFI_STATE_DISABLED:
                        //wifi关闭发出的广播
                        wifiManager.startScan();
                        break;
                }
            } else if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                Log.d(TAG, "receiver action = NETWORK_STATE_CHANGED_ACTION");
                NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if (info.getState().equals(NetworkInfo.State.DISCONNECTED)) {
                    text_state.setText("连接已经断开");
                } else if (info.getState().equals(NetworkInfo.State.CONNECTED)) {
                    //在连接网络的情况下。针对于发送端，连接者，去请求热点
                    WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                    final WifiInfo wifiInfo = wifiManager.getConnectionInfo();

                    text_state.setText("已经连接到网络：" + wifiInfo.getSSID());

                    //如果当前连接到的wifi是热点,则开启连接线程
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                ArrayList<String> connectedIP = getConnectedIP();

                                for (String ip : connectedIP) {
                                    Socket socket = new Socket(ip, PORT);
                                    connectThread = new ConnectThread(socket, mHandler);
                                    connectThread.start();
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                } else {
                    NetworkInfo.DetailedState state = info.getDetailedState();
                    if (state == NetworkInfo.DetailedState.CONNECTING) {
                        text_state.setText("连接中...");
                    } else if (state == NetworkInfo.DetailedState.AUTHENTICATING) {
                        text_state.setText("正在验证身份信息...");
                    } else if (state == NetworkInfo.DetailedState.OBTAINING_IPADDR) {
                        text_state.setText("正在获取IP地址...");
                    } else if (state == NetworkInfo.DetailedState.FAILED) {
                        text_state.setText("连接失败");
                    }
                }
            }
        }
    };

    //获取到连接到的热点上的手机的ip
    private ArrayList<String> getConnectedIP() {
        ArrayList<String> connectedIP = new ArrayList<String>();
        try {
            BufferedReader br = new BufferedReader(new FileReader("/proc/net/arp"));
            String line;
            while((line = br.readLine()) != null) {
                String[] splitted = line.split(" +");
                if (splitted != null && splitted.length >= 4) {
                    String ip = splitted[0];
                    connectedIP.add(ip);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return connectedIP;
    }

    private void initView() {
        mList = (ListView) findViewById(R.id.listView);
        btn_send = (Button) findViewById(R.id.btn_send);
        btn_search = (Button) findViewById(R.id.btn_search);
        textview = (TextView) findViewById(R.id.textview);
        text_state = (TextView) findViewById(R.id.text_state);
        editText = (EditText) findViewById(R.id.input);

        btn_send.setOnClickListener(this);
        btn_search.setOnClickListener(this);

        wifiListAdapter = new WifiListAdapter(this, R.layout.wifi_list_item);
        mList.setAdapter(wifiListAdapter);

        mList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                //disconnect before connect
                wifiManager.disconnect();

                final ScanResult scanResult = wifiListAdapter.getItem(position);

                //The type of wifi
                String capabilities = scanResult.capabilities;

                int type = WIFICIPHER_WEP;
                if (!TextUtils.isEmpty(capabilities)) {
                    if (capabilities.contains("WAP") || capabilities.contains("wap")) {
                        type = WIFICIPHER_WPA;
                    } else if (capabilities.contains("WEP") || capabilities.contains("wep")) {
                        type = WIFICIPHER_WEP;
                    } else {
                        type = WIFICIPHER_NOPASS; //Unencrypted connection,don't need password.
                    }
                }
                config = isExsits(scanResult.SSID);
                if (config == null) {
                    if (type != WIFICIPHER_NOPASS) { //Need password
                        final EditText editText = new EditText(MainActivity.this);
                        final int finalType = type;
                        new AlertDialog.Builder(MainActivity.this).setTitle("请输入wifi密码")
                                .setIcon(android.R.drawable.ic_dialog_info).setView(editText)
                                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Log.d(TAG, "editText.getText():" + editText.getText());
                                        config = createWifiInfo(scanResult.SSID, editText.getText().toString(), finalType);
                                        connect(config);
                                    }
                                }).setNegativeButton("取消", null).show();
                                return;
                    } else {
                        //don't need password to connect
                        config = createWifiInfo(scanResult.SSID, "", type);
                        connect(config);
                    }
                } else {
                    connect(config);
                }
            }
        });
    }

    private void connect(WifiConfiguration config) {
        text_state.setText("连接中...");
        wcgID = wifiManager.addNetwork(config);
        wifiManager.enableNetwork(wcgID, true);
    }

    public WifiConfiguration createWifiInfo(String SSID,String password, int type) {
        Log.d(TAG, "SSID = " + SSID + "password = " + password + "type = " + type);
        WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();
        config.SSID = "\"" + SSID + "\"";
        if (type == WIFICIPHER_NOPASS) {
            config.wepKeys[0] = "\"" + "\"";
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        } else if (type == WIFICIPHER_WEP) {
            config.preSharedKey = "\"" + password + "\"";
            config.hiddenSSID = true;
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        } else if (type == WIFICIPHER_WPA) {
            config.preSharedKey = "\"" + password + "\"";
            config.hiddenSSID = true;
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            // config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            config.status = WifiConfiguration.Status.ENABLED;
        } else {
            return null;
        }
        return config;
    }

    //判断当前wifi是否之前存在密码配置.
    private WifiConfiguration isExsits(String SSID) {
        List<WifiConfiguration> existingConfigs = wifiManager.getConfiguredNetworks();
        //遍历所有配置里面是否存在当前ssid
        for (WifiConfiguration existingConfig : existingConfigs) {
            if (existingConfig.SSID.equals("\"" + SSID + "\"")) {
                return existingConfig;
            }
        }
        return null;
    }

    private void setPermission() {
        //Android 6.0 需要动态申请权限
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)) {
            //请求权限
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)) {
                Toast.makeText(getApplicationContext(), "需要蓝牙权限", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_send:
                if (connectThread != null) {
                    connectThread.sendData(editText.getText().toString().trim());
                } else {
                    Log.d(TAG, "connectThread is null");
                }
                break;
            case R.id.btn_search:
                search();
                break;
        }
    }

    //search wifi hotspot
    private void search() {
        //If wifi is enabled, it will open a dialog to require wifi permission
        if (!wifiManager.isWifiEnabled()) {
            //enable wifi
            wifiManager.setWifiEnabled(true);
        }
        //Start to scan hotspot
        wifiManager.startScan();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }
}
