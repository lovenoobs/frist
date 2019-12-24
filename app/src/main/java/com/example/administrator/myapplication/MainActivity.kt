package com.example.administrator.myapplication


import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.ArrayList
import java.util.UUID

import android.R
import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast

 class BuletoothClientActivity:Activity(), OnItemClickListener {
 // 获取到蓝牙适配器
    private var mBluetoothAdapter:BluetoothAdapter? = null
 // 用来保存搜索到的设备信息
    private val bluetoothDevices = ArrayList<String>()
 // ListView组件
    private var lvDevices:ListView? = null
 // ListView的字符串数组适配器
    private var arrayAdapter:ArrayAdapter<String>? = null
 // UUID，蓝牙建立链接需要的
    private val MY_UUID = UUID
.fromString("00001101-0000-1000-8000-00805F9B34FB")
 // 为其链接创建一个名称
    private val NAME = "Bluetooth_Socket"
 // 选中发送数据的蓝牙设备，全局变量，否则连接在方法执行完就结束了
    private var selectDevice:BluetoothDevice? = null
 // 获取到选中设备的客户端串口，全局变量，否则连接在方法执行完就结束了
    private var clientSocket:BluetoothSocket? = null
 // 获取到向设备写的输出流，全局变量，否则连接在方法执行完就结束了
    private var os:OutputStream? = null
 // 服务端利用线程不断接受客户端信息
    private var thread:AcceptThread? = null
 //定义按钮
    //定义按钮
    private var close_all_led:Button? = null
private var red1:Button? = null
private var green1:Button? = null
private var blue1:Button? = null
private var breath:Button? = null
private var receive1:TextView? = null
private var seekBar:SeekBar? = null
private var LED_STATE = "A 红灯亮"
private var re_msg:TextView? = null
 // 注册广播接收者
    private val receiver = object:BroadcastReceiver() {
override fun onReceive(arg0:Context, intent:Intent) {
 // 获取到广播的action
            val action = intent.action
 // 判断广播是搜索到设备还是搜索完成
            if (action == BluetoothDevice.ACTION_FOUND)
{
 // 找到设备后获取其设备
                val device = intent
.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
 // 判断这个设备是否是之前已经绑定过了，如果是则不需要添加，在程序初始化的时候已经添加了
                if (device.bondState != BluetoothDevice.BOND_BONDED)
{
 // 设备没有绑定过，则将其保持到arrayList集合中
                    bluetoothDevices.add(
                        device.name + ":"
    + device.address + "\n"
                    )
 // 更新字符串数组适配器，将内容显示在listView中
                    arrayAdapter!!.notifyDataSetChanged()
}
}
else if (action == BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
{
    title = "搜索完成"
}
}
}

 // 创建handler，因为我们接收是采用线程来接收的，在线程中无法操作UI，所以需要handler
    internal var handler:Handler = object:Handler() {
override fun handleMessage(msg:Message) {
 // TODO Auto-generated method stub
            super.handleMessage(msg)
 // 通过msg传递过来的信息，吐司一下收到的信息
           // Toast.makeText(BuletoothClientActivity.this, (String) msg.obj, Toast.LENGTH_SHORT).show();
    re_msg!!.text = msg.obj as String
}
}

override fun onCreate(savedInstanceState:Bundle?) {
super.onCreate(savedInstanceState)
setContentView(R.layout.layout_buletooth_seacher)
red1 = findViewById(R.id.red) as Button
green1 = findViewById(R.id.green) as Button
blue1 = findViewById(R.id.blue) as Button
receive1 = findViewById(R.id.receive_text) as TextView
close_all_led = findViewById(R.id.close_all_led) as Button
breath = findViewById(R.id.breath) as Button
seekBar = findViewById(R.id.seekBar) as SeekBar
re_msg = findViewById(R.id.msg) as TextView

red1!!.setOnClickListener {
    LED_STATE = "R"
    receive1!!.text = LED_STATE
}
    green1!!.setOnClickListener {
        LED_STATE = "G"
        receive1!!.text = LED_STATE
    }
    blue1!!.setOnClickListener {
        LED_STATE = "B"
        receive1!!.text = LED_STATE
    }
    close_all_led!!.setOnClickListener {
        LED_STATE = "E"
        receive1!!.text = LED_STATE
    }
    breath!!.setOnClickListener {
        LED_STATE = "H"
        receive1!!.text = LED_STATE
    }
    seekBar!!.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener {
override fun onProgressChanged(seekBar:SeekBar, i:Int, b:Boolean) {
if (b)
{
LED_STATE = "{0:$i}"
    receive1!!.text = LED_STATE
}
}

override fun onStartTrackingTouch(seekBar:SeekBar) {

}

override fun onStopTrackingTouch(seekBar:SeekBar) {

}
})
 // 获取到蓝牙默认的适配器
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
 // 获取到ListView组件
        lvDevices = findViewById<ListView>(R.id.lvDevices)
 // 为listview设置字符换数组适配器
        arrayAdapter = ArrayAdapter(this,
R.layout.simple_list_item_1, R.id.text1,
bluetoothDevices)
 // 为listView绑定适配器
    lvDevices!!.adapter = arrayAdapter
 // 为listView设置item点击事件侦听
    lvDevices!!.onItemClickListener = this

 // 用Set集合保持已绑定的设备   将绑定的设备添加到Set集合。
        val devices = mBluetoothAdapter!!.bondedDevices
if (devices.size > 0)
{
for (bluetoothDevice in devices)
{
 // 保存到arrayList集合中
                bluetoothDevices.add((bluetoothDevice.name + ":"
+ bluetoothDevice.address + "\n"))
}
}

 // 因为蓝牙搜索到设备和完成搜索都是通过广播来告诉其他应用的
        // 这里注册找到设备和完成搜索广播
        var filter = IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
registerReceiver(receiver, filter)
filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
registerReceiver(receiver, filter)

 // 实例接收客户端传过来的数据线程
        thread = AcceptThread()
 // 线程开始
        thread!!.start()
}

 //搜索蓝牙设备
 @SuppressLint("MissingPermission")
 fun onClick_Search(view:View) {
     title = "正在扫描..."
 // 点击搜索周边设备，如果正在搜索，则暂停搜索
        if (mBluetoothAdapter!!.isDiscovering)
{
mBluetoothAdapter!!.cancelDiscovery()
}
mBluetoothAdapter!!.startDiscovery()
}

 // 点击listView中的设备，传送数据
    @SuppressLint("WrongConstant")
    override fun onItemClick(parent:AdapterView<*>, view:View, position:Int,
                             id:Long) {
 // 获取到这个设备的信息
        val s = arrayAdapter!!.getItem(position)
 // 对其进行分割，获取到这个设备的地址
        val address = s!!.substring(s!!.indexOf(":") + 1).trim { it <= ' ' }
     Log.d("TAG", address)
 // 判断当前是否还是正在搜索周边设备，如果是则暂停搜索
        if (mBluetoothAdapter!!.isDiscovering)
{
mBluetoothAdapter!!.cancelDiscovery()
}
 // 如果选择设备为空则代表还没有选择设备
        if (selectDevice == null)
{
 //通过地址获取到该设备
            selectDevice = mBluetoothAdapter!!.getRemoteDevice(address)
}
 // 这里需要try catch一下，以防异常抛出
        try
{
 // 判断客户端接口是否为空
            if (clientSocket == null)
{
 // 获取到客户端接口
                clientSocket = selectDevice!!
.createRfcommSocketToServiceRecord(MY_UUID)
 // 向服务端发送连接
                clientSocket!!.connect()
 // 获取到输出流，向外写数据
                os = clientSocket!!.outputStream

}
 // 判断是否拿到输出流
            if (os != null)
{
 // 需要发送的信息
                //String text = "我传过去了";
                // 以utf-8的格式发送出去
                os!!.write(LED_STATE.toByteArray(charset("UTF-8")))
}
 // 吐司一下，告诉用户发送成功
            Toast.makeText(this, "发送信息成功，请查收", 0).show()
}
catch (e:IOException) {
 // TODO Auto-generated catch block
            e.printStackTrace()
 // 如果发生异常则告诉用户发送失败
            Toast.makeText(
                this,
                "发送信息失败",
                0
            ).show()
}

}

 // 服务端接收信息线程
    private inner class AcceptThread:Thread() {
private var serverSocket:BluetoothServerSocket? = null// 服务端接口
private var socket:BluetoothSocket? = null// 获取到客户端的接口
private var `is`:InputStream? = null// 获取到输入流
private var os:OutputStream? = null// 获取到输出流
init{
try
{
 // 通过UUID监听请求，然后获取到对应的服务端接口
                serverSocket = mBluetoothAdapter!!
.listenUsingRfcommWithServiceRecord(NAME, MY_UUID)
}
catch (e:Exception) {
e.printStackTrace()
}

}

override fun run() {
try
{
 // 接收其客户端的接口
                socket = serverSocket!!.accept()
 // 获取到输入流
                `is` = socket!!.inputStream
 // 获取到输出流
                os = socket!!.outputStream

 // 无线循环来接收数据
                while (true)
{
 // 创建一个128字节的缓冲
                    val buffer = ByteArray(128)
 // 每次读取128字节，并保存其读取的角标
                    val count = `is`!!.read(buffer)
 // 创建Message类，向handler发送数据
                    val msg = Message()
 // 发送一个String的数据，让他向上转型为obj类型
                    msg.obj = String(buffer, 0, count, "utf-8")
 // 发送数据
                    handler.sendMessage(msg)
}
}
catch (e:Exception) {
 // TODO: handle exception
                e.printStackTrace()
}

}

     private fun String(bytes: ByteArray, offset: Int, length: Int, charset: String): String {


     }
 }
}