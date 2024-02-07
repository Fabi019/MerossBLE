package dev.fabik.merossble.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import dev.fabik.merossble.R

class LogFragment : Fragment() {

    companion object {
        private var buffer = StringBuffer()
        const val TAG = "LogFragment"
    }

    private lateinit var logTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_log, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        logTextView = view.findViewById(R.id.logTextView)

        if (buffer.isNotEmpty()) {
            log(buffer.toString())
        }

        super.onViewCreated(view, savedInstanceState)
    }

    fun log(message: String) {
        runCatching {
            requireActivity().runOnUiThread {
                logTextView.append("$message\n")
            }
        }.onFailure {
            Log.e(TAG, "Failed to log message: ${it.message}")
        }
        buffer.append("$message\n")
    }

}