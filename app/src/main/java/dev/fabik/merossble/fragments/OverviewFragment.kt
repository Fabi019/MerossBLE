package dev.fabik.merossble.fragments

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.fabik.merossble.BleCallback
import dev.fabik.merossble.BleManager
import dev.fabik.merossble.R
import dev.fabik.merossble.protocol.Header
import dev.fabik.merossble.protocol.Packet
import dev.fabik.merossble.protocol.payloads.DeviceInfo
import dev.fabik.merossble.protocol.payloads.Time
import dev.fabik.merossble.protocol.payloads.Wifi
import dev.fabik.merossble.protocol.payloads.toDeviceInfo
import dev.fabik.merossble.protocol.payloads.toJSONObject
import dev.fabik.merossble.protocol.payloads.toWifiList
import dev.fabik.merossble.protocol.toHeader
import org.json.JSONObject

class OverviewFragment(private val logFragment: LogFragment) : Fragment() {

    private lateinit var bleManager: BleManager

    private lateinit var noDeviceText: TextView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private var deviceInfo: DeviceInfo? = null
    private var wifiNetworks: List<Wifi>? = null

    private var waitingDialog: AlertDialog? = null

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

        bleManager = BleManager(requireActivity(), bleCallback, logFragment)

        swipeRefreshLayout.setOnRefreshListener {
            bleManager.scan()
        }

        super.onViewCreated(view, savedInstanceState)
    }

    private var bleCallback = object : BleCallback {
        override fun onPacketReceived(packetJson: String) {
            val json = JSONObject(packetJson)
            val header = json.getJSONObject("header").toHeader()

            if (header.namespace == "Appliance.System.All") {
                val payload = json.getJSONObject("payload")
                deviceInfo = payload.toDeviceInfo().also { deviceInfo ->
                    childFragmentManager.findFragmentById(R.id.deviceInfoCard)?.let {
                        requireActivity().runOnUiThread {
                            (it as DeviceInfoFragment).setDeviceInfo(deviceInfo)
                        }
                    }

                    childFragmentManager.findFragmentById(R.id.wifiConfigCard)?.let {
                        requireActivity().runOnUiThread {
                            (it as ConfigFragment).setDeviceInfo(deviceInfo)
                        }
                    }
                }
            } else if (header.namespace == "Appliance.Config.WifiList") {
                val payload = json.getJSONObject("payload")
                wifiNetworks = payload.toWifiList().also { wifiNetworks ->
                    childFragmentManager.findFragmentById(R.id.wifiConfigCard)?.let {
                        requireActivity().runOnUiThread {
                            (it as ConfigFragment).setWifiNetworks(wifiNetworks)
                        }
                    }
                }
            }

            waitingDialog?.dismiss()
            waitingDialog = null
        }

        override fun onNewDevice(device: BluetoothDevice?) {
            if (device == null) return

            requireActivity().runOnUiThread {
                noDeviceText.visibility = View.GONE

                childFragmentManager.beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(
                        R.id.deviceInfoCard, DeviceInfoFragment(
                            onDataLoad = {
                                showWaitingDialog()

                                logFragment.log("Requesting device info")
                                bleManager.sendPacket(
                                    Packet(
                                        Header(
                                            method = "GET",
                                            namespace = "Appliance.System.All"
                                        ),
                                    )
                                )
                            })
                    )
                    .commit()
            }
        }

        override fun onConnectionStateChanged(state: Int) {
            if (state == 99) {
                requireActivity().runOnUiThread {
                    swipeRefreshLayout.isRefreshing = false

                    childFragmentManager.beginTransaction()
                        .setReorderingAllowed(true)
                        .replace(
                            R.id.wifiConfigCard, ConfigFragment(
                                onRefresh = {
                                    showWaitingDialog()

                                    logFragment.log("Requesting wifi list")
                                    bleManager.sendPacket(
                                        Packet(
                                            Header(
                                                method = "GET",
                                                namespace = "Appliance.Config.WifiList"
                                            )
                                        )
                                    )
                                },
                                onUpdateTimestamp = { timezone ->
                                    showWaitingDialog()

                                    logFragment.log("Setting time to: $timezone")
                                    val time = Time(timezone, System.currentTimeMillis() / 1000)
                                    bleManager.sendPacket(
                                        Packet(
                                            Header(
                                                method = "SET",
                                                namespace = "Appliance.System.Time"
                                            ),
                                            payload = JSONObject().apply {
                                                put("time", time.toJSONObject())
                                            }
                                        )
                                    )

                                },
                                onConfirmMqtt = { keyConfig ->
                                    showWaitingDialog()

                                    logFragment.log("Configuring: $keyConfig")
                                    bleManager.sendPacket(
                                        Packet(
                                            Header(
                                                method = "SET",
                                                namespace = "Appliance.Config.Key"
                                            ),
                                            payload = JSONObject().apply {
                                                put("key", keyConfig.toJSONObject())
                                            }
                                        )
                                    )
                                },
                                onConfirmWifi = { wifi, password ->
                                    showWaitingDialog()

                                    logFragment.log("Selected wifi: $wifi, password: ***")
                                    bleManager.sendPacket(
                                        Packet(
                                            Header(
                                                method = "SET",
                                                namespace = "Appliance.Config.WifiX"
                                            ),
                                            payload = JSONObject().apply {
                                                put("wifi", wifi.toJSONObject(password))
                                            }
                                        )
                                    )
                                })
                        )
                        .commit()
                }
            } else if (state == BluetoothGatt.STATE_DISCONNECTED) {
                requireActivity().runOnUiThread {
                    disconnect()
                }
            }
        }
    }

    fun showWaitingDialog() {
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

    fun disconnect(client: Boolean = false) {
        if (client) {
            bleManager.disconnect()
        }

        deviceInfo = null
        wifiNetworks = null
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

}