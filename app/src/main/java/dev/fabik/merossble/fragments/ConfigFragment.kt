package dev.fabik.merossble.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import dev.fabik.merossble.R
import dev.fabik.merossble.model.ConfigViewModel
import dev.fabik.merossble.protocol.bytes2hex
import dev.fabik.merossble.protocol.calculateWifiXPassword
import dev.fabik.merossble.protocol.payloads.Gateway
import dev.fabik.merossble.protocol.payloads.KeyConfig
import dev.fabik.merossble.protocol.payloads.Wifi
import java.security.MessageDigest

class ConfigFragment : Fragment() {

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

    private val viewModel: ConfigViewModel by viewModels({ requireParentFragment() })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_config, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        wifiSelection = view.findViewById(R.id.wifiSelection)

        viewModel.wifiNetworks.observe(viewLifecycleOwner) {
            setWifiNetworks(it)
        }

        passwordInput = view.findViewById(R.id.wifiPassword)
        searchWifiButton = view.findViewById(R.id.searchWifiButton)
        wifiConfirmButton = view.findViewById(R.id.writeWifi)

        var selectedWifi: Wifi? = null
        wifiSelection.setOnItemClickListener { _, _, position, _ ->
            selectedWifi = viewModel.wifiNetworks.value?.get(position)
        }

        wifiConfirmButton.setOnClickListener {
            selectedWifi?.let { wifi ->
                val password = passwordInput.text.toString()
                viewModel.deviceInfo.value?.let {
                    val hashed = calculateWifiXPassword(password, it.type, it.uuid, it.mac)
                    viewModel.onConfirmWifi?.invoke(wifi, hashed)
                }
            }
        }

        passwordField = view.findViewById(R.id.password)
        userIdField = view.findViewById(R.id.userIdInput)
        keyField = view.findViewById(R.id.keyInput)
        mqttServerField = view.findViewById(R.id.mqttServer)
        portField = view.findViewById(R.id.mqttPort)
        mqttConfirmButton = view.findViewById(R.id.writeKey)

        mqttConfirmButton.setOnClickListener {
            val mqttServer = mqttServerField.text.toString()
            runCatching {
                portField.text.toString().toInt()
            }.getOrNull()?.let { port ->
                val userId = userIdField.text.toString()
                val key = keyField.text.toString()

                val gateway = Gateway(mqttServer, port, port, mqttServer)
                val keyConfig = KeyConfig(userId, key, gateway)
                viewModel.onConfirmMqtt?.invoke(keyConfig)
            }
        }

        searchWifiButton.setOnClickListener {
            viewModel.onRefresh?.invoke()
        }

        userIdField.addTextChangedListener {
            updateMQTTPassword()
        }

        keyField.addTextChangedListener {
            updateMQTTPassword()
        }

        viewModel.deviceInfo.observe(viewLifecycleOwner) {
            it?.let {
                wifiConfirmButton.isEnabled = true
                mqttConfirmButton.isEnabled = true

                mqttServerField.text = it.mqtt
                portField.text = it.port.toString()
                userIdField.text = it.userId

                updateMQTTPassword()
            } ?: run {
                wifiConfirmButton.isEnabled = false
                mqttConfirmButton.isEnabled = false
            }
        }

        super.onViewCreated(view, savedInstanceState)
    }

    private fun updateMQTTPassword() {
        val mac = viewModel.deviceInfo.value?.mac ?: return
        val key = keyField.text.toString()
        val userId = userIdField.text.toString()

        val md5 = MessageDigest.getInstance("MD5")
        md5.update(mac.toByteArray() + key.toByteArray())
        val digest = md5.digest()

        passwordField.text = "${userId}_${bytes2hex(digest)}"
    }

    private fun setWifiNetworks(wifiNetworks: List<Wifi>) {
        wifiSelection.setSimpleItems(wifiNetworks.map { it.ssid + " (${it.bssid})" }.toTypedArray())
        if (wifiNetworks.isNotEmpty()) {
            wifiSelection.setText(wifiSelection.adapter.getItem(0).toString(), false)
            wifiSelection.onItemClickListener?.onItemClick(null, wifiSelection, 0, 0)
        }
    }

}