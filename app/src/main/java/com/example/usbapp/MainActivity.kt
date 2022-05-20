package com.example.usbapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.coroutineScope
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.usbapp.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.jahnen.libaums.core.UsbMassStorageDevice
import me.jahnen.libaums.core.fs.UsbFileOutputStream

private const val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"
private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var bytes: ByteArray
    private val TIMEOUT = 0
    private val forceClaim = true
    private var connection: UsbDeviceConnection? = null
    private var intf: UsbInterface? = null
    private var usbManager: UsbManager? = null
    private lateinit var saveButton: Button
    var usbReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {

            if (UsbManager.ACTION_USB_DEVICE_DETACHED == intent.action) {
                connection?.releaseInterface(intf)
                connection?.close()
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.saveButton.setOnClickListener { view ->
            Toast.makeText(this@MainActivity, "We're going to move images to internal storage now.", Toast.LENGTH_SHORT).show()
            val usb: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
            usb?.apply {
                val devices = UsbMassStorageDevice.getMassStorageDevices(this@MainActivity /* Context or Activity */)
                val device = devices[0]
                // before interacting with a device you need to call init()!
                device.init()
                // Only uses the first partition on the device
                val currentFs = device.partitions[0].fileSystem
                Log.d(TAG, "Capacity: " + currentFs.capacity)
                Log.d(TAG, "Occupied Space: " + currentFs.occupiedSpace)
                Log.d(TAG, "Free Space: " + currentFs.freeSpace)
                Log.d(TAG, "Chunk size: " + currentFs.chunkSize)
                val root = currentFs.rootDirectory

                val files = root.listFiles()
                for (file in files) {
                    Log.d(TAG, file.name)
                }
                lifecycle.coroutineScope.launch(Dispatchers.IO){
                    val newDir = root.createDirectory(binding.toAddress.text.toString())
                    val file = newDir.createFile("bar.txt")
                    // write to a file
                    val os = UsbFileOutputStream(file)
                    os.write("hello".toByteArray())
                    os.close()
                    device.close()
                }
            }
        }

        registerReceiver(usbReceiver, IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED))

    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}