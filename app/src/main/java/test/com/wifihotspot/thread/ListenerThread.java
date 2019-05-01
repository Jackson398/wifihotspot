package test.com.wifihotspot.thread;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import test.com.wifihotspot.MainActivity;

/**
 * Created by hu.qinghui on 2019/4/30.
 */

public class ListenerThread extends Thread {

    private static final String TAG = "ListenerThread";

    private ServerSocket serverSocket = null;
    private Handler handler;
    private int port;
    private Socket socket;

    public ListenerThread(int port, Handler handler) {
        setName("ListenerThread");
        this.port = port;
        this.handler = handler;
        try {
            //listen to localhost device 12345 port
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                Log.d(TAG, "等待设备连接");

                //blocked,waiting for device connect.
                socket = serverSocket.accept();
                Message message = Message.obtain();
                message.what = MainActivity.DEVICE_CONNECTING;
                handler.sendMessage(message);
            } catch (IOException e) {
                Log.d(TAG, "error：" + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public Socket getSocket() { return socket;}
}
