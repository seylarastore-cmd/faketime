package com.mcai.faketime.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mcai.faketime.Config
import com.mcai.faketime.R
import com.mcai.faketime.databinding.FragmentClockBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ClockFragment : Fragment() {

    private var _binding: FragmentClockBinding? = null
    private val binding get() = _binding!!

    private val handler = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            renderPreview()
            handler.postDelayed(this, 1000)
        }
    }

    private val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentClockBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val ctx = requireContext()

        binding.switchEnabled.isChecked = Config.isEnabled(ctx)
        binding.switchEnabled.setOnCheckedChangeListener { _, checked ->
            Config.setEnabled(ctx, checked)
            (activity as MainActivity).showMessage(
                if (checked) "Fake time enabled" else "Fake time disabled",
            )
        }

        binding.btnApply.setOnClickListener {
            applyFromInputs()
        }

        binding.btnClear.setOnClickListener {
            binding.inputDays.setText("0")
            binding.inputHours.setText("0")
            binding.inputMinutes.setText("0")
            Config.setOffsetMillis(ctx, 0L)
            (activity as MainActivity).showMessage("Offset reset to 0")
            renderPreview()
        }

        // Prefill current offset split into days/hours/minutes.
        val current = Config.offsetMillis(ctx)
        prefill(current)
    }

    private fun prefill(millis: Long) {
        val totalMinutes = millis / 60_000L
        val days = totalMinutes / (24 * 60)
        val hours = (totalMinutes % (24 * 60)) / 60
        val minutes = totalMinutes % 60
        binding.inputDays.setText(days.toString())
        binding.inputHours.setText(hours.toString())
        binding.inputMinutes.setText(minutes.toString())
    }

    private fun applyFromInputs() {
        val ctx = requireContext()
        val days = binding.inputDays.text.toString().toLongOrNull() ?: 0L
        val hours = binding.inputHours.text.toString().toLongOrNull() ?: 0L
        val minutes = binding.inputMinutes.text.toString().toLongOrNull() ?: 0L
        val total = days * 86_400_000L + hours * 3_600_000L + minutes * 60_000L
        Config.setOffsetMillis(ctx, total)
        (activity as MainActivity).showMessage("Offset applied")
        renderPreview()
    }

    private fun renderPreview() {
        val real = System.currentTimeMillis()
        val offset = Config.offsetMillis(requireContext())
        val fake = real + offset
        binding.textReal.text = getString(R.string.now_real) + ":  " + formatter.format(Date(real))
        binding.textFake.text = getString(R.string.now_fake) + ":  " + formatter.format(Date(fake))
        binding.textNote.text = "Offset: ${offset / 3_600_000.0} hours  |  " +
            "Apps read time + offset. Per-app overrides are in the Per-App tab."
    }

    override fun onResume() {
        super.onResume()
        handler.post(tick)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(tick)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(tick)
        _binding = null
    }
}
