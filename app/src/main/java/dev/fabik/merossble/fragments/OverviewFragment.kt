package dev.fabik.merossble.fragments

import android.bluetooth.BluetoothGatt
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.fabik.merossble.R
import dev.fabik.merossble.model.ConfigViewModel
import dev.fabik.merossble.model.MainViewModel
import dev.fabik.merossble.protocol.Header
import dev.fabik.merossble.protocol.Packet
import dev.fabik.merossble.protocol.payloads.toDeviceInfo
import dev.fabik.merossble.protocol.payloads.toJSONObject
import dev.fabik.merossble.protocol.payloads.toWifiList
import dev.fabik.merossble.protocol.toHeader
import org.json.JSONObject

class OverviewFragment : Fragment() {

    private lateinit var noDeviceText: TextView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private var logFragment: LogFragment? = null

    private var waitingDialog: AlertDialog? = null

    private val viewModel: ConfigViewModel by viewModels()
    private val mainViewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_overview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        swipeRefreshLayout = view.findViewById(R.id.swiperefresh)
        noDeviceText = view.findViewById(R.id.noDevice)

        swipeRefreshLayout.setOnRefreshListener {
            log("Requested refresh")
            mainViewModel.bleManager?.scan()
        }

        if (viewModel.deviceInfo.value != null) {
            noDeviceText.visibility = View.GONE
        }

        viewModel.onRefresh = {
            showWaitingDialog()

            log("Requesting wifi list")
            mainViewModel.bleManager?.sendPacket(
                Packet(Header(method = "GET", namespace = "Appliance.Config.WifiList"))
            )
        }

        viewModel.onConfirmMqtt = { keyConfig ->
            showWaitingDialog()

            log("Configuring: $keyConfig")
            mainViewModel.bleManager?.sendPacket(
                Packet(
                    Header(method = "SET", namespace = "Appliance.Config.Key"),
                    payload = JSONObject().apply {
                        put("key", keyConfig.toJSONObject())
                    }
                )
            )
        }

        viewModel.onConfirmWifi = { wifi, password ->
            showWaitingDialog()

            log("Selected wifi: $wifi, password: ***")
            mainViewModel.bleManager?.sendPacket(
                Packet(
                    Header(method = "SET", namespace = "Appliance.Config.WifiX"),
                    payload = JSONObject().apply {
                        put("wifi", wifi.toJSONObject(password))
                    }
                )
            )
        }

        viewModel.onDataLoad = {
            showWaitingDialog()

            log("Requesting device info")
            mainViewModel.bleManager?.sendPacket(
                Packet(Header(method = "GET", namespace = "Appliance.System.All"))
            )
        }

        mainViewModel.connectionState.observe(viewLifecycleOwner) { state ->
            if (state == 99) {
                swipeRefreshLayout.isRefreshing = false
                childFragmentManager.beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.wifiConfigCard, ConfigFragment())
                    .commit()
            } else if (state == BluetoothGatt.STATE_DISCONNECTED) {
                disconnect()
            }
        }

        mainViewModel.packetData.observe(viewLifecycleOwner) { packetJson ->
            val json = JSONObject(packetJson)
            val header = json.getJSONObject("header").toHeader()
            val payload = json.getJSONObject("payload")

            if (header.namespace == "Appliance.System.All") {
                viewModel.deviceInfo.postValue(payload.toDeviceInfo())
            } else if (header.namespace == "Appliance.Config.WifiList") {
                viewModel.wifiNetworks.postValue(payload.toWifiList())
            }

            waitingDialog?.dismiss()
            waitingDialog = null
        }

        mainViewModel.newDevice.observe(viewLifecycleOwner) { device ->
            if (device == null) return@observe

            noDeviceText.visibility = View.GONE

            childFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.deviceInfoCard, DeviceInfoFragment())
                .commit()
        }

        super.onViewCreated(view, savedInstanceState)
    }

    private fun showWaitingDialog() {
        waitingDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Waiting...")
            .setView(R.layout.dialog_waiting)
            .setCancelable(false)
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                waitingDialog = null
            }
            .show()
    }

    private fun disconnect() {
        viewModel.deviceInfo.postValue(null)
        viewModel.wifiNetworks.postValue(emptyList())
        swipeRefreshLayout.isRefreshing = false
        noDeviceText.visibility = View.VISIBLE

        val transaction = childFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
        childFragmentManager.findFragmentById(R.id.deviceInfoCard)?.let {
            transaction.remove(it)
        }
        childFragmentManager.findFragmentById(R.id.wifiConfigCard)?.let {
            transaction.remove(it)
        }
        transaction.commit()
    }

    private fun log(message: String) {
        if (logFragment == null) {
            parentFragmentManager.findFragmentByTag("f1")?.let {
                logFragment = it as LogFragment
                logFragment?.log(message)
            }
        } else {
            logFragment?.log(message)
        }
    }

}