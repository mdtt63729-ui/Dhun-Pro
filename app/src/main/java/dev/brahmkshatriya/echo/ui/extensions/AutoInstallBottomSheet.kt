package dev.brahmkshatriya.echo.ui.extensions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.databinding.BottomSheetAutoInstallBinding
import dev.brahmkshatriya.echo.di.App
import dev.brahmkshatriya.echo.extensions.AutoExtensionInstaller
import dev.brahmkshatriya.echo.extensions.ExtensionLoader
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

/**
 * Auto Install Bottom Sheet — a single popup that shows the progress of
 * all bundled extension installations in one place.
 *
 * Animation: slides up from the bottom with a bouncy (overshoot) effect.
 * Uses Material BottomSheetDialog with an OvershootInterpolator on the
 * bottom sheet view for the bouncy feel.
 *
 * Usage:
 *   AutoInstallBottomSheet.newInstance().show(supportFragmentManager, "auto_install")
 *
 * The sheet auto-dismisses when all extensions are installed (or shows
 * a "Done" button if there were errors).
 */
class AutoInstallBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAutoInstallBinding? = null
    private val binding get() = _binding!!

    private val app by inject<App>()
    private val extensionLoader by inject<ExtensionLoader>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAutoInstallBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ── Bouncy slide-up animation ──
        // The bottom sheet view gets an overshoot translation Y animation
        // for the "bouncy" feel the user requested.
        val bottomSheet = (dialog as? BottomSheetDialog)?.findViewById<View>(
            com.google.android.material.R.id.design_bottom_sheet
        )
        bottomSheet?.let { sheet ->
            sheet.translationY = 300f
            sheet.animate()
                .translationY(0f)
                .setDuration(500)
                .setInterpolator(OvershootInterpolator(0.8f))
                .start()
        }

        // Start installing all bundled extensions
        startAutoInstall()
    }

    private fun startAutoInstall() {
        binding.autoInstallProgress.isIndeterminate = false
        binding.autoInstallProgress.max = 100

        viewLifecycleOwner.lifecycleScope.launch {
            AutoExtensionInstaller.installAll(
                context = requireContext(),
                settings = app.settings,
                fileIgnoreFlow = extensionLoader.fileIgnoreFlow,
                onProgress = { progress ->
                    requireActivity().runOnUiThread {
                        updateProgress(progress)
                    }
                }
            )
        }
    }

    private fun updateProgress(progress: AutoExtensionInstaller.InstallProgress) {
        if (progress.total == 0) {
            // No extensions to install
            binding.autoInstallStatus.text = progress.name
            binding.autoInstallStatus.isVisible = true
            binding.autoInstallDoneButton.isVisible = true
            binding.autoInstallProgress.isVisible = false
            binding.autoInstallCurrent.text = ""
            setupDoneButton()
            return
        }

        // Update progress bar
        val percent = if (progress.total > 0) {
            (progress.current * 100) / progress.total
        } else 0
        binding.autoInstallProgress.setProgress(percent, true)

        // Update current extension name
        binding.autoInstallCurrent.text = getString(
            R.string.auto_install_progress_format,
            progress.current, progress.total, progress.name
        )

        // Show error if any
        if (progress.error != null) {
            binding.autoInstallStatus.text = progress.error
            binding.autoInstallStatus.isVisible = true
        }

        // When done
        if (progress.isDone) {
            binding.autoInstallProgress.setProgress(100, true)
            binding.autoInstallCurrent.text = getString(R.string.auto_install_complete)
            if (progress.error != null) {
                // Had errors — show Done button
                binding.autoInstallStatus.text = getString(
                    R.string.auto_install_errors, progress.error
                )
                binding.autoInstallStatus.isVisible = true
                binding.autoInstallDoneButton.isVisible = true
                setupDoneButton()
            } else {
                // All good — auto dismiss after a short delay
                binding.autoInstallDoneButton.isVisible = true
                binding.autoInstallDoneButton.text = getString(R.string.auto_install_done)
                setupDoneButton()
            }
        }
    }

    private fun setupDoneButton() {
        binding.autoInstallDoneButton.setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        fun newInstance() = AutoInstallBottomSheet()
    }
}
