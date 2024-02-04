package dev.fabik.merossble.fragments

import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import dev.fabik.merossble.R
import dev.fabik.merossble.protocol.Packet
import dev.fabik.merossble.protocol.payloads.DeviceInfo
import dev.fabik.merossble.protocol.payloads.Gateway
import dev.fabik.merossble.protocol.payloads.KeyConfig
import dev.fabik.merossble.protocol.payloads.Wifi
import java.security.MessageDigest

class ConfigFragment(
    private var wifiNetworks: List<Wifi> = emptyList(),
    private val onRefresh: () -> Unit,
    private val onUpdateTimestamp: (String) -> Unit, // timezone
    private val onConfirmMqtt: (KeyConfig) -> Unit, // mqtt server, port, userId, key
    private val onConfirmWifi: (Wifi, String) -> Unit // ssid, password
) : Fragment() {

    private var deviceInfo: DeviceInfo? = null

    private lateinit var wifiSelection: MaterialAutoCompleteTextView
    private lateinit var passwordInput: TextInputEditText
    private lateinit var searchWifiButton: Button
    private lateinit var wifiConfirmButton: Button

    private lateinit var mqttServerField: TextView
    private lateinit var portField: TextView
    private lateinit var userIdField: TextView
    private lateinit var keyField: TextView
    private lateinit var passwordField: TextView
    private lateinit var mqttConfirmButton: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_config, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        wifiSelection = view.findViewById(R.id.wifiSelection)
        setWifiNetworks(wifiNetworks)

        passwordInput = view.findViewById(R.id.wifiPassword)
        searchWifiButton = view.findViewById(R.id.searchWifiButton)
        wifiConfirmButton = view.findViewById(R.id.writeWifi)

        var selectedWifi: Wifi? = null
        wifiSelection.setOnItemClickListener { _, _, position, _ ->
            selectedWifi = wifiNetworks[position]
        }

        wifiConfirmButton.setOnClickListener {
            Log.d("ConfigFragment", "wifi: $selectedWifi, password: ${passwordInput.text}")
            selectedWifi?.let { wifi ->
                val password = passwordInput.text.toString()
                deviceInfo?.let {
                    val hashed = Packet.calculateWifiXPassword(password, it.type, it.uuid, it.mac)
                    Log.d("ConfigFragment", "hashed: $hashed")
                    onConfirmWifi(wifi, hashed)
                }
            }
        }

        val timezoneField = view.findViewById<TextView>(R.id.timezone)
        val updateTimestampButton = view.findViewById<Button>(R.id.writeTimestamp)
        updateTimestampButton.setOnClickListener {
            onUpdateTimestamp(timezoneField.text.toString())
        }

        passwordField = view.findViewById(R.id.password)
        userIdField = view.findViewById(R.id.userIdInput)
        keyField = view.findViewById(R.id.keyInput)
        mqttServerField = view.findViewById(R.id.mqttServer)
        portField = view.findViewById(R.id.mqttPort)
        mqttConfirmButton = view.findViewById(R.id.writeKey)

        mqttConfirmButton.setOnClickListener {
            val mqttServer = mqttServerField.text.toString()
            runCatching { portField.text.toString().toInt() }.getOrNull()?.let { port ->
                val userId = userIdField.text.toString()
                val key = keyField.text.toString()

                val gateway = Gateway(mqttServer, port, port, mqttServer)
                val keyConfig = KeyConfig(userId, key, gateway)
                onConfirmMqtt(keyConfig)
            }
        }

        searchWifiButton.setOnClickListener {
            onRefresh()
        }

        userIdField.addTextChangedListener {
            updateMQTTPassword()
        }

        keyField.addTextChangedListener {
            updateMQTTPassword()
        }

        super.onViewCreated(view, savedInstanceState)
    }

    fun setDeviceInfo(deviceInfo: DeviceInfo) {
        this.deviceInfo = deviceInfo
        wifiConfirmButton.isEnabled = true
        mqttConfirmButton.isEnabled = true

        mqttServerField.text = deviceInfo.mqtt
        portField.text = deviceInfo.port.toString()
        userIdField.text = deviceInfo.userId

        updateMQTTPassword()
    }

    private fun updateMQTTPassword() {
        val mac = deviceInfo?.mac ?: return
        val key = keyField.text.toString()
        val userId = userIdField.text.toString()

        val md5 = MessageDigest.getInstance("MD5")
        md5.update(mac.toByteArray() + key.toByteArray())
        val digest = md5.digest()

        passwordField.text = "${userId}_${digest.joinToString("") { "%02x".format(it) }}"
    }

    fun setWifiNetworks(wifiNetworks: List<Wifi>) {
        this.wifiNetworks = wifiNetworks
        wifiSelection.setSimpleItems(wifiNetworks.map { it.ssid + " (${it.bssid})" }.toTypedArray())
        if (wifiNetworks.isNotEmpty()) {
            wifiSelection.setText(wifiSelection.adapter.getItem(0).toString(), false)
            wifiSelection.onItemClickListener?.onItemClick(null, wifiSelection, 0, 0)
        }
    }

}