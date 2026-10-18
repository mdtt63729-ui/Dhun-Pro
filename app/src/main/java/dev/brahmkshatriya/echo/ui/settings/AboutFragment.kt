package dev.brahmkshatriya.echo.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.databinding.FragmentGenericCollapsableBinding
import dev.brahmkshatriya.echo.ui.common.UiViewModel.Companion.applyBackPressCallback
import dev.brahmkshatriya.echo.utils.ui.AutoClearedValue.Companion.autoCleared
import dev.brahmkshatriya.echo.utils.ui.UiUtils.configureAppBar

/**
 * Added from ArchiveTune: a proper About screen. The settings sheet only ever showed a bare
 * version string; this adds the build type, package, device and links.
 */
class AboutFragment : Fragment() {

    private var binding: FragmentGenericCollapsableBinding by autoCleared()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentGenericCollapsableBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyBackPressCallback()
        binding.appBarLayout.configureAppBar { offset -> binding.toolbarOutline.alpha = offset }
        binding.toolBar.title = getString(R.string.about)
        binding.toolBar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
        binding.extensionIcon.isVisible = false
        childFragmentManager.beginTransaction()
            .replace(R.id.genericFragmentContainer, AboutContentFragment())
            .commit()
    }
}
