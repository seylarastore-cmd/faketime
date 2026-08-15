package com.mcai.faketime.ui

import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.mcai.faketime.Config
import com.mcai.faketime.R
import com.mcai.faketime.databinding.FragmentAppsBinding

class AppsFragment : Fragment() {

    private var _binding: FragmentAppsBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: AppsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAppsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = AppsAdapter(requireContext()) { pkg, real ->
            Config.setRealTimeApp(requireContext(), pkg, real)
        }
        binding.recyclerApps.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerApps.addItemDecoration(
            DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL),
        )
        binding.recyclerApps.adapter = adapter
        reload()
    }

    fun reload() {
        val ctx = requireContext()
        val pm = ctx.packageManager
        val realTime = Config.realTimeApps(ctx)
        val apps = pm.getInstalledApplications(0)
            .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
            .mapNotNull {
                try {
                    AppEntry(it.packageName, pm.getApplicationLabel(it).toString(), it.packageName in realTime)
                } catch (_: Throwable) {
                    null
                }
            }
            .sortedBy { it.label.lowercase() }
        adapter.submit(apps)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
