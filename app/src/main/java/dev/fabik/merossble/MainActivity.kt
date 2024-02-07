package dev.fabik.merossble

import android.Manifest
import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dev.fabik.merossble.fragments.LogFragment
import dev.fabik.merossble.fragments.OverviewFragment
import dev.fabik.merossble.model.MainViewModel

@SuppressLint("MissingPermission")
class MainActivity : AppCompatActivity() {

    private val viewPager2: ViewPager2 by lazy { findViewById(R.id.viewPager2) }
    private val tabLayout: TabLayout by lazy { findViewById(R.id.tabLayout) }
    private val topAppBar: MaterialToolbar by lazy { findViewById(R.id.topAppBar) }

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Fix status bar text color
        val windowInsetController = WindowCompat.getInsetsController(window, window.decorView)
        val nightMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            resources.configuration.isNightModeActive
        } else {
            resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        }
        windowInsetController.isAppearanceLightStatusBars = !nightMode

        // Basic permission check
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                ), 0
            )
        }

        // Setup Bluetooth manager
        mainViewModel.bleManager = BleManager(this, mainViewModel.bleCallback)

        // Setup ViewPager2
        viewPager2.adapter = TabPagerAdapter(this, listOf(OverviewFragment(), LogFragment()))
        viewPager2.offscreenPageLimit = 2 // Preloads log fragment

        // Setup TabLayout with ViewPager2
        TabLayoutMediator(tabLayout, viewPager2) { tab, position ->
            tab.icon = when (position) {
                0 -> AppCompatResources.getDrawable(
                    this,
                    R.drawable.baseline_bluetooth_connected_24
                )

                1 -> AppCompatResources.getDrawable(this, R.drawable.baseline_manage_search_24)
                else -> null
            }
        }.attach()

        topAppBar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.disconectMenuItem) {
                mainViewModel.disconnect()
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