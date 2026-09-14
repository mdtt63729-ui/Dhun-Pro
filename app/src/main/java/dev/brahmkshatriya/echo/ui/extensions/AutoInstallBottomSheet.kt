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
 * First-launch "Install all extensions" popup.
 *
 * It is intentionally a single surface: bundled YouTube Music + Saavn are
 * installed first, then every other extension in the remote catalog is
 * downloaded and installed sequentially in the same popup.
 */
class AutoInstallBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAutoInstallBinding? = null
    private val binding get() = _binding!!

    private val app by inject<App>()
    private val extensionLoader by inject<ExtensionLoader>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = BottomSheetAutoInstallBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog?.setCanceledOnTouchOutside(false)
        (dialog as? BottomSheetDialog)?.behavior?.isDraggable = false

        // Small overshoot entrance so the setup sheet feels integrated with the
        // rest of Dhun's motion language.
        (dialog as? BottomSheetDialog)
            ?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            ?.let { sheet ->
                sheet.translationY = 260f
                sheet.animate()
                    .translationY(0f)
                    .setDuration(500)
                    .setInterpolator(OvershootInterpolator(0.8f))
                    .start()
            }

        binding.autoInstallDoneButton.setOnClickListener { dismissAllowingStateLoss() }
        binding.autoInstallDoneButton.isVisible = false
        binding.autoInstallProgress.isIndeterminate = false
        binding.autoInstallProgress.max = 100
        binding.autoInstallStatus.isVisible = false
        binding.autoInstallLog.text = ""

        startAutoInstall()
    }

    private fun startAutoInstall() {
        viewLifecycleOwner.lifecycleScope.launch {
            AutoExtensionInstaller.installAll(
                context = requireContext(),
                settings = app.settings,
                fileIgnoreFlow = extensionLoader.fileIgnoreFlow,
                onProgress = { progress ->
                    if (isAdded) {
                        requireActivity().runOnUiThread { updateProgress(progress) }
                    }
                },
            )
        }
    }

    private fun updateProgress(progress: AutoExtensionInstaller.InstallProgress) {
        if (_binding == null) return

        if (progress.total == 0) {
            binding.autoInstallProgress.isVisible = false
            binding.autoInstallCurrent.text = progress.name
            binding.autoInstallStatus.text = progress.error ?: getString(R.string.auto_install_complete)
            binding.autoInstallStatus.isVisible = true
            binding.autoInstallDoneButton.isVisible = true
            binding.autoInstallDoneButton.text = getString(R.string.auto_install_done)
            return
        }

        val percent = (progress.current * 100 / progress.total).coerceIn(0, 100)
        binding.autoInstallProgress.setProgress(percent, true)
        binding.autoInstallCurrent.text = getString(
            R.string.auto_install_progress_format,
            progress.current,
            progress.total,
            progress.name,
        )

        if (progress.error != null) {
            binding.autoInstallStatus.text = progress.error
            binding.autoInstallStatus.isVisible = true
        }

        val previous = binding.autoInstallLog.text?.toString().orEmpty()
        val line = if (progress.error == null) "✓ ${progress.name}" else "✕ ${progress.name}"
        if (previous.isEmpty() || !previous.endsWith(line)) {
            binding.autoInstallLog.text = if (previous.isEmpty()) line else "$previous\n$line"
        }

        if (progress.isDone) {
            binding.autoInstallProgress.setProgress(100, true)
            binding.autoInstallCurrent.text = progress.name
            binding.autoInstallDoneButton.isVisible = true
            binding.autoInstallDoneButton.text = getString(R.string.auto_install_done)
            if (progress.error != null) {
                binding.autoInstallStatus.text = getString(
                    R.string.auto_install_errors,
                    progress.error,
                )
                binding.autoInstallStatus.isVisible = true
            } else {
                binding.autoInstallStatus.text = getString(R.string.auto_install_complete)
                binding.autoInstallStatus.isVisible = true
            }
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
