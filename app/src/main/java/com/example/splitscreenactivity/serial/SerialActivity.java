package com.example.splitscreenactivity.serial;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import com.example.splitscreenactivity.R;

import java.nio.ByteBuffer;
import java.util.Arrays;

import androidx.appcompat.app.AppCompatActivity;

public class SerialActivity extends AppCompatActivity implements SerialInter {
    private static final String TAG = SerialActivity.class.getSimpleName();

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            SerialManage.getInstance().send("132456789");//发送指令
            handler.postDelayed(runnable ,3000);
        }
    };

    private static ByteBuffer buffer = ByteBuffer.allocate(8);
    public static byte[] longToBytes(long x) { buffer.putLong(0, x); return buffer.array(); }


    Handler handler =  new Handler();

    public static byte[] chaxun = {(byte)0xa0,(byte)0x00,(byte)0x03,(byte)0x02,(byte)0x00,(byte)0x01,(byte)0xaa};
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_serial);
        initQrCodeSerialPort();
        findViewById(R.id.start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                SystemControl.reboot();
                SerialManage.getInstance().send(bytesHexString(chaxun , 0 , 7));//发送指令
            }
        });
        handler.postDelayed(runnable ,3000);
    }

    private void initQrCodeSerialPort() {

        SerialManage.getInstance().init(this);//串口初始化
        SerialManage.getInstance().open();//打开串口

//        //参数：1。串口地址2波特开车

//            @Override
//            protected void onDataReceived(ComBean paramComBean) {
//                //子线程操作 解析数据
//                //返回数据
//                byte[] bRec = paramComBean.bRec;
//                //解析方法设备不同所以逻辑不同，具体根据厂家沟通和说明文档来写下面有例子
//
//                Log.e(TAG, "接受到了串口的数据：" + Arrays.toString(bRec));
//
//                serialHelper.send(chaxun);//发送数据,查询
//
//            }
//        };
//        try {
//            serialHelper.open();
//        } catch (IOException e) {
//            e.printStackTrace();
//            Log.e(TAG, "串口打开失败");
//        }

    }

    /**
     * 描述：将byte数组转字符串
     * 举例：bytesHexString(bytes,0,size);
     * @param bytes 数组
     * @param dec 起始位
     * @param size 数组长度
     * @return
     */
    public String bytesHexString(byte[] bytes, int dec, int size){
        byte[] temp = new byte[size];
        System.arraycopy(bytes, dec, temp, 0, size);
        StringBuilder builder = new StringBuilder();
        if (bytes == null || bytes.length <= 0) return "";
        char[] buffer = new char[2];
        for (int i = 0; i < bytes.length; i++) {
            buffer[0] = Character.forDigit((bytes[i] >>> 4) & 0x0F, 16);
            buffer[1] = Character.forDigit(bytes[i] & 0x0F, 16);
            builder.append(buffer);
        }
        return builder.toString().toUpperCase();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SerialManage.getInstance().colse();
//        if (serialHelper != null) {
//            serialHelper.close();
//        }
    }

    @Override
    public void connectMsg(String path, boolean isSucc) {
        String msg = isSucc ? "成功" : "失败";
        Log.e("串口连接回调", "串口 "+ path + " -连接" + msg);
    }

    /**
     * //若在串口开启的方法中 传入false 此处不会返回数据
     */
    @Override
    public void readData(String path, String result, int size) {
        Log.e("串口数据回调","串口 "+ path + " -获取数据" + result);
    }
}