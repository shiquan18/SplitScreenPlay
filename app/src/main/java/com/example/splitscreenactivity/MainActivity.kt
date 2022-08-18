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

    //4联屏配置
    private var deviceList:ArrayList<ConfigBean> = arrayListOf(
        ConfigBean(true,"" , 2560 , 1600, 0 , 0 ,8856),//主屏配置在第一个
        ConfigBean(false,"192.168.10.47" , 2560 , 1600, -1280 , 0 ,8855),
        ConfigBean(false,"192.168.10.79" , 2560 , 1600, 0 , -800 ,8854),
        ConfigBean(false,"192.168.10.81" , 2560 , 1600, -1280 , -800 ,8853)
    )

//    private var currentDevice:ConfigBean = deviceList[0] // 主屏的配置
    private var currentDevice:ConfigBean = deviceList[1] // 右上屏的配置
//    private var currentDevice:ConfigBean = deviceList[2] // 左下屏的配置
//    private var currentDevice:ConfigBean = deviceList[3] // 右下屏的配置

    private var mHandler: Handler = @SuppressLint("HandlerLeak")
    object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg.what) {
                0 -> {
                    Log.e("MainActivity" , "发送成功 ${System.currentTimeMillis()}")
                    Log.d("MainActivity", "进度：" + mVideoView?.currentPosition)
                    onStartView(0)
                }
                1 -> {
                    val bundle = msg.data
                    var content = bundle.getString("content") // 这里的orderid是一个全局变量
                    Log.e("MainActivity" , "接收到的消息 $content")
                    Log.d("MainActivity", "自己的状态：time ${System.currentTimeMillis()} progress ${mVideoView?.currentPosition}")
                    if (!content.isNullOrEmpty()){
                        var arrayList = content.split("/")
                        var intervalTime = System.currentTimeMillis() - arrayList[0].toLong()
                        var intervalProgress = arrayList[1].toInt() - mVideoView?.currentPosition!!
                        //根据进度间隔与时间间隔的差值，计算副屏进度实际延迟的时间，然后根据延迟时间快进
                        //比如主屏传递过来的时间比当前时间慢了70ms , 但是主屏传递过来的进度依然快了540ms , 那么副屏的视频的进度延迟时间是 540 - 70 = 470ms
                        //额外加50ms是计算时间在内的误差值
//                        var delayTime = intervalProgress - intervalTime //用这个时间存在问题，intervalTime获取的时间戳在不同的设备上不一致
//                        onStartView(delayTime + 50)

                        onStartView(intervalProgress + 100) //快进的差值需要根据主屏的播放进度计算

                    }else{
                        onStartView(0) //快进的差值需要根据主屏的播放进度计算
                    }
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
        initView()

        checkPermission()

        if(currentDevice.isMain){
            start?.visibility = View.VISIBLE
            socketManager.addDevice(deviceList)
            start?.setOnClickListener {
                start()
            }
        }else{
            Thread{
                socketManager.receiveMsg(mHandler ,currentDevice.receivePort)
            }.start()
        }
    }

    /**
     * 注意视频原有的尺寸是以宽度为基准的，比例无法更改
     * 如果视频的比例与整体的比例不一致，就会导致高度方向有留白的问题
     */
    private fun initView() {
        val lp = LinearLayout.LayoutParams(currentDevice.with, currentDevice.height)
        lp.setMargins(currentDevice.offsetX, currentDevice.offsetY, 0, 0)
        mVideoView?.layoutParams = lp

        mVideoView?.setOnCompletionListener {
            playState = false
            Log.d("MainActivity", "播放结束：")
        }
    }

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

    /**
     * 主屏触发消息
     */
    fun start(){
        Log.e("MainActivity" , "按钮被点击了 ${System.currentTimeMillis()}")
        Thread{
            socketManager.sendMsg( "",mHandler)
            Thread.sleep(1500) //1.5秒钟后再发一次消息，用于同步进度
            socketManager.sendMsg( "${System.currentTimeMillis()}/${ mVideoView?.currentPosition}",mHandler)
            Thread.sleep(1500) //1.5秒钟后再发一次消息，用于同步进度
            socketManager.sendMsg( "${System.currentTimeMillis()}/${ mVideoView?.currentPosition}",mHandler)
        }.start()
    }

    var playState:Boolean = false
    fun onStartView(progress:Int) {
        if(playState){
            if(progress!=0){
                Log.e("MainActivity" , "快进 $progress")
                mVideoView?.currentPosition?.plus(progress)?.let { mVideoView?.seekTo(it) };  //以毫秒为单位设置视频的播放进度
            }
        }else{
            playState = true
            mVideoView?.setVideoPath(Environment.getExternalStorageDirectory().toString() + "/video.MP4")
            mVideoView?.setMediaController(mMediaController);
            mVideoView?.seekTo(0)
            mVideoView?.requestFocus()
            mVideoView?.start()
        }
    }

}