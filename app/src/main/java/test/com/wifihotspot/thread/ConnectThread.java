package test.com.wifihotspot.thread;


import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import test.com.wifihotspot.MainActivity;

/**
 * Created by hu.qinghui on 2019/4/30.
 */

public class ConnectThread extends Thread {

    private static final String TAG = "ConnectThread";

    private final Socket socket;
    private Handler mHandler;
    private InputStream inputStream;
    private OutputStream outputStream;

    public ConnectThread(Socket socket, Handler handler) {
        setName("ConnectThread");
        this.socket = socket;
        this.mHandler = handler;
    }

    @Override
    public void run() {

        if (socket == null) {
            return;
        }
        mHandler.sendEmptyMessage(MainActivity.DEVICE_CONNECTED);
        try {
            //get data stream
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();

            byte[] buffer = new byte[1024];
            int bytes;
            while (true) {
                //get data
                bytes = inputStream.read(buffer);
                if (bytes > 0) {
                    final byte[] data = new byte[bytes];
                    System.arraycopy(buffer, 0, data, 0, bytes);

                    Message message = Message.obtain();
                    message.what = MainActivity.GET_MSG;
                    Bundle bundle = new Bundle();
                    bundle.putString("MSGT", new String(data));
                    message.setData(bundle);
                    mHandler.sendMessage(message);

                    Log.d(TAG, "读取到的数据:" + new String(data));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //send data
    public void sendData(final String msg) {

        new Thread() {
            @Override
            public void run() {

                Log.d(TAG, "发送数据outputStream是否为空:" + (outputStream == null));

                if (outputStream != null) {
                    try {
                        outputStream.write(msg.getBytes());
                        Log.d(TAG, "发送消息:" + msg);
                        Message message = Message.obtain();
                        message.what = MainActivity.SEND_MSG_SUCCESS;
                        Bundle bundle = new Bundle();
                        bundle.putString("MSG", new String(msg));
                        message.setData(bundle);
                        mHandler.sendMessage(message);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Message message = Message.obtain();
                        message.what = MainActivity.SEND_MSG_ERROR;
                        Bundle bundle = new Bundle();
                        bundle.putString("MSG", new String(msg));
                        message.setData(bundle);
                        mHandler.sendMessage(message);
                    }
                }
            }
        }.start();
    }
}
