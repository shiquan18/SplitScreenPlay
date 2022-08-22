package com.example.splitscreenactivity

data class ConfigBean(
    var isMain:Boolean,  //是否为主屏
    var address:String,  //udp 通讯的设备ip
    var with:Int,        // 联屏的总宽度
    var height:Int,      // 联屏的总高度
    var offsetX:Int,     // 当前屏幕的x轴偏移量
    var offsetY:Int,     // 当前屏幕的y轴偏移量
    var receivePort:Int  //udp 通讯的设备端口号
)
