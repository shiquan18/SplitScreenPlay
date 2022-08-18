package com.example.splitscreenactivity

import android.os.Bundle
import android.os.Handler
import android.util.Log
import java.io.IOException
import java.net.*


class SocketManager {
    private var sendSocket: DatagramSocket? = null
    private val sendPort = 8856
    private val receivePort = 8855
    private var deviceList:ArrayList<ConfigBean> = arrayListOf()

    fun addDevice(deviceAddress:ArrayList<ConfigBean>){
        deviceList = deviceAddress
    }

    fun sendMsg(message:String , mHandler: Handler) {
        try {
            if (sendSocket == null) {
                sendSocket = DatagramSocket(sendPort)
            }
            val bytes: ByteArray = message.toByteArray()

            for(item in 1 until deviceList.size){ //过滤掉主屏的第一条
                val inetAddress: InetAddress = InetAddress.getByName(deviceList[item].address)
                val datagramPacket = DatagramPacket(bytes, bytes.size, inetAddress, receivePort)
                sendSocket!!.send(datagramPacket)
                mHandler.sendEmptyMessage(0)
            }
        } catch (e: SocketException) {
            e.printStackTrace()
        } catch (e: UnknownHostException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private var receiveSocket: DatagramSocket? = null
    var datagramPacket: DatagramPacket? = null
    fun receiveMsg(mHandler: Handler) {
        Log.e("SocketManager" ,  "等待socket消息")
        try {
            while (true) {
                if (receiveSocket == null) {
                    receiveSocket = DatagramSocket(receivePort)
                }
                val bytes = ByteArray(1024)
                datagramPacket = DatagramPacket(bytes, 0, bytes.size)
                receiveSocket!!.receive(datagramPacket)

                var msg = mHandler.obtainMessage()
                val bundle = Bundle()
                bundle.putString("content", String(bytes, 0, datagramPacket!!.length))
                msg.data = bundle
                msg.what = 1
                mHandler.sendMessage(msg)
            }
        } catch (e: SocketException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}