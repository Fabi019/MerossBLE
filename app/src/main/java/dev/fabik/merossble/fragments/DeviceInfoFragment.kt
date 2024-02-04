package dev.fabik.merossble.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import dev.fabik.merossble.R
import dev.fabik.merossble.protocol.payloads.DeviceInfo

class DeviceInfoFragment(
    private val deviceInfo: DeviceInfo? = null,
    private val onDataLoad: (() -> Unit)? = null
) : Fragment() {

    private lateinit var typeTextView: TextView
    private lateinit var versionTextView: TextView
    private lateinit var chipTextView: TextView
    private lateinit var macTextView: TextView
    private lateinit var mqttServerTextView: TextView
    private lateinit var firmwareTextView: TextView
    private lateinit var userIdTextView: TextView
    private lateinit var uuidTextView: TextView

    private lateinit var readButton: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_device_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        typeTextView = view.findViewById(R.id.type)
        versionTextView = view.findViewById(R.id.version)
        chipTextView = view.findViewById(R.id.chip)
        macTextView = view.findViewById(R.id.mac)
        mqttServerTextView = view.findViewById(R.id.mqtt)
        firmwareTextView = view.findViewById(R.id.firmware)
        userIdTextView = view.findViewById(R.id.userId)
        uuidTextView = view.findViewById(R.id.uuid)
        readButton = view.findViewById(R.id.readButton)

        onDataLoad?.let { onDataLoad ->
            readButton.visibility = View.VISIBLE
            readButton.setOnClickListener {
                onDataLoad()
            }
        }

        setDeviceInfo(deviceInfo)

        super.onViewCreated(view, savedInstanceState)
    }

    fun setDeviceInfo(deviceInfo: DeviceInfo?) {
        deviceInfo?.let {
            typeTextView.text = it.type
            versionTextView.text = it.version
            chipTextView.text = it.chip
            macTextView.text = it.mac
            mqttServerTextView.text = "${it.mqtt}:${it.port}"
            firmwareTextView.text = it.firm
            userIdTextView.text = it.userId
            uuidTextView.text = it.uuid
        }
    }

}