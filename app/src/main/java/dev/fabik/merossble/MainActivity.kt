package dev.fabik.merossble

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothStatusCodes
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.ParcelUuid
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dev.fabik.merossble.fragments.LogFragment
import dev.fabik.merossble.fragments.OverviewFragment

@SuppressLint("MissingPermission")
class MainActivity : AppCompatActivity() {

    private val viewPager2: ViewPager2 by lazy { findViewById(R.id.viewPager2) }
    private val tabLayout: TabLayout by lazy { findViewById(R.id.tabLayout) }
    private val topAppBar: MaterialToolbar by lazy { findViewById(R.id.topAppBar) }

    private val logFragment: LogFragment by lazy { LogFragment() }
    private val overviewFragment: OverviewFragment by lazy { OverviewFragment(logFragment) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Basic permission check
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT),
                    1
                )
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.BLUETOOTH_ADMIN),
                    1
                )
            }
        }

        // Setup ViewPager2
        viewPager2.adapter = TabPagerAdapter(this, listOf(overviewFragment, logFragment))

        // Setup TabLayout with ViewPager2
        TabLayoutMediator(tabLayout, viewPager2) { tab, position ->
            tab.icon = when (position) {
                0 -> AppCompatResources.getDrawable(this, R.drawable.baseline_bluetooth_connected_24)
                1 -> AppCompatResources.getDrawable(this, R.drawable.baseline_manage_search_24)
                else -> null
            }
        }.attach()

        topAppBar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.disconectMenuItem) {
                overviewFragment.disconnect(true)
                return@setOnMenuItemClickListener true
            }
            false
        }
    }

    private class TabPagerAdapter(
        fragmentActivity: FragmentActivity,
        private val fragments: List<Fragment>
    ) : FragmentStateAdapter(fragmentActivity) {
        override fun getItemCount(): Int = fragments.size
        override fun createFragment(position: Int): Fragment = fragments[position]
    }
}