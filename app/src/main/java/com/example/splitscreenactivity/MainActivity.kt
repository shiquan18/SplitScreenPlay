package com.example.splitscreenactivity

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.MediaController
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {

    var mMediaController: MediaController? = null
    var mVideoView: VideoView? = null
    var start: Button? = null
    var socketManager:SocketManager = SocketManager()

    //联屏配置
    private var deviceList:ArrayList<ConfigBean> = arrayListOf(
        ConfigBean(true,"" , 2560 , 1600, 0 , 0 ),//主屏配置在第一个
        ConfigBean(false,"192.168.10.54" , 2560 , 1600, -1280 , 0 ),
        ConfigBean(false,"192.168.10.47" , 2560 , 1600, 0 , -800 ),
        ConfigBean(false,"192.168.10.47" , 2560 , 1600, -1280 , -800 )
    )

    private var currentDevice:ConfigBean = deviceList[0] // 主屏的配置
//    private var currentDevice:ConfigBean = deviceList[1] // 右上屏的配置
//    private var currentDevice:ConfigBean = deviceList[2] // 左下屏的配置
//    private var currentDevice:ConfigBean = deviceList[3] // 右下屏的配置

    private var mHandler: Handler = @SuppressLint("HandlerLeak")
    object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg.what) {
                0 -> {
                    Log.e("MainActivity" , "发送成功 ${System.currentTimeMillis()}")
                    this.postDelayed(Runnable { onStartView() },400)
                }
                1 -> {
                    Log.e("MainActivity" , "接收成功 ${System.currentTimeMillis()}")
                    onStartView()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val decorView = window.decorView
        decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        decorView.setOnSystemUiVisibilityChangeListener {
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        }

        setContentView(R.layout.activity_main)
        mMediaController = MediaController(this);

        mVideoView = findViewById(R.id.video_view)
        start = findViewById(R.id.start)
        moveView()

        checkPermission()

        if(currentDevice.isMain){
            start?.visibility = View.VISIBLE
            socketManager.addDevice(deviceList)
        }else{
            Thread{
                socketManager.receiveMsg(mHandler)
            }.start()
        }
    }

    /**
     * 注意视频原有的尺寸是以宽度为基准的，比例无法更改
     * 如果视频的比例与整体的比例不一致，就会导致高度方向有留白的问题
     */
    private fun moveView() {
        val lp = LinearLayout.LayoutParams(currentDevice.with, currentDevice.height)
        lp.setMargins(currentDevice.offsetX, currentDevice.offsetY, 0, 0)
        mVideoView?.layoutParams = lp
    }

    //这是dp转为px的方法
    private fun dp2px(i: Int): Int {
        return (Resources.getSystem().displayMetrics.density * i + 0.5f).toInt()}

    fun checkPermission() {
        ActivityCompat.requestPermissions(
            this@MainActivity, arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ), 666 )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 666) {
            for (i in grantResults.indices) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    //缺少权限
                    Log.d("MainActivity", "onRequestPermissionsResult " + grantResults[i])
                    return
                }
            }
        }
    }

    fun start(view:View){
        Log.e("MainActivity" , "按钮被点击了 ${System.currentTimeMillis()}")
        Thread{
            socketManager.sendMsg("start",mHandler)
        }.start()
    }

    fun onStartView() {
        Log.e("MainActivity", "播放时间 ${System.currentTimeMillis()}")
        mVideoView?.setVideoPath(Environment.getExternalStorageDirectory().toString() + "/video.MP4")
        mVideoView?.setMediaController(mMediaController);
        mVideoView?.seekTo(0)
        mVideoView?.requestFocus()
        mVideoView?.start()
        Log.d("MainActivity", "进度：" + mVideoView?.currentPosition)
    }
}