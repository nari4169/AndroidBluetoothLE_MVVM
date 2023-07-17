package com.lilly.ble.ui.main

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lilly.ble.PERMISSIONS
import com.lilly.ble.R
import com.lilly.ble.REQUEST_ALL_PERMISSION
import com.lilly.ble.adapter.BleListAdapter
import com.lilly.ble.databinding.ActivityMainBinding
import com.lilly.ble.viewmodel.MainViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel


class MainActivity : AppCompatActivity() {

    private val viewModel by viewModel<MainViewModel>()
    private var adapter: BleListAdapter? = null

    private var requiredPermissions: Array<String> = arrayOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
    )

    private  val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()) { permissions ->

        permissions.entries.forEach {
            Log.e("DEBUG", "requestPermissions ${it.key} = ${it.value}")
            if (!it.value) {
                requestPermissionLauncher.launch(it.key)
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        Log.e("", "isGrant = $isGranted")
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2) {
            requiredPermissions = arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.NEARBY_WIFI_DEVICES,
            )
        }

        if (!hasPermissions(applicationContext, *requiredPermissions)) {
            requestPermissions.launch(requiredPermissions)
        }

        val binding = DataBindingUtil.setContentView<ActivityMainBinding>(
            this,
            R.layout.activity_main
        )
        binding.viewModel = viewModel

        binding.rvBleList.setHasFixedSize(true)
        val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(this)
        binding.rvBleList.layoutManager = layoutManager


        adapter = BleListAdapter()
        binding.rvBleList.adapter = adapter
        adapter?.setItemClickListener(object : BleListAdapter.ItemClickListener {
            override fun onClick(view: View, device: BluetoothDevice?) {
                if (device != null) {
                    viewModel.connectDevice(device)
                }
            }
        })

        initObserver(binding)

    }
    private fun initObserver(binding: ActivityMainBinding){
        viewModel.requestEnableBLE.observe(this) {
            it.getContentIfNotHandled()?.let {
                requestEnableBLE()
            }
        }
        viewModel.listUpdate.observe(this) {
            it.getContentIfNotHandled()?.let { scanResults ->
                adapter?.setItem(scanResults)
            }
        }


        viewModel._isScanning.observe(this) {
            it.getContentIfNotHandled()?.let { scanning ->
                viewModel.isScanning.set(scanning)
            }
        }
        viewModel._isConnect.observe(this) {
            it.getContentIfNotHandled()?.let { connect ->
                viewModel.isConnect.set(connect)
            }
        }
        viewModel.statusTxt.observe(this) {

            binding.statusText.text = it

        }

        viewModel.readTxt.observe(this) {

            binding.txtRead.append(it)

            if ((binding.txtRead.measuredHeight - binding.scroller.scrollY) <=
                (binding.scroller.height + binding.txtRead.lineHeight)
            ) {
                binding.scroller.post {
                    binding.scroller.smoothScrollTo(0, binding.txtRead.bottom)
                }
            }

        }
    }
    override fun onResume() {
        super.onResume()
        // finish app if the BLE is not supported
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            finish()
        }
    }


    private val requestEnableBleResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val intent = result.data
            // do somthing after enableBleRequest
        }
    }

    /**
     * Request BLE enable
     */
    private fun requestEnableBLE() {
        val bleEnableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        requestEnableBleResult.launch(bleEnableIntent)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun hasPermissions(context: Context, vararg permissions: String): Boolean {

        for (permission in permissions) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
            Log.e("","permission=$permission true")
        }
        return true
    }

}