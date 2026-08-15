package com.mcai.faketime.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.mcai.faketime.R
import com.mcai.faketime.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        val adapter = PagerAdapter(this)
        binding.pager.adapter = adapter
        binding.pager.offscreenPageLimit = 2

        TabLayoutMediator(binding.tabs, binding.pager) { tab, position ->
            tab.text = if (position == 0) {
                getString(R.string.tab_clock)
            } else {
                getString(R.string.tab_apps)
            }
        }.attach()

        binding.pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (position == 1) {
                    adapter.appsFragment?.reload()
                }
            }
        })
    }

    fun showMessage(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
