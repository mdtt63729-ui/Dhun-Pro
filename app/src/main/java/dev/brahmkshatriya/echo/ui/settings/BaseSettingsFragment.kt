package dev.brahmkshatriya.echo.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceFragmentCompat
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.ImageHolder
import dev.brahmkshatriya.echo.databinding.FragmentGenericCollapsableBinding
import dev.brahmkshatriya.echo.ui.common.UiViewModel.Companion.applyBackPressCallback
import dev.brahmkshatriya.echo.ui.common.UiViewModel.Companion.applyContentInsets
import dev.brahmkshatriya.echo.ui.common.UiViewModel.Companion.applyInsets
import dev.brahmkshatriya.echo.utils.image.ImageUtils.loadAsCircle
import dev.brahmkshatriya.echo.utils.ui.AnimationUtils.setupTransition
import dev.brahmkshatriya.echo.utils.ui.AutoClearedValue.Companion.autoCleared
import dev.brahmkshatriya.echo.utils.ui.FastScrollerHelper
import dev.brahmkshatriya.echo.utils.ui.UiUtils.configureAppBar

abstract class BaseSettingsFragment : Fragment() {

    abstract val title: String
    abstract val icon: ImageHolder?
    abstract val creator: () -> Fragment

    var binding: FragmentGenericCollapsableBinding by autoCleared()

    final override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentGenericCollapsableBinding.inflate(inflater, container, false)
        return binding.root
    }

    @CallSuper
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupTransition(view)

        applyBackPressCallback()
        binding.appBarLayout.configureAppBar { offset ->
            binding.toolbarOutline.alpha = offset
            binding.extensionIcon.alpha = 1 - offset
        }
        binding.toolBar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.toolBar.title = title
        icon.loadAsCircle(binding.extensionIcon, R.drawable.ic_extension_32dp) {
            binding.extensionIcon.setImageDrawable(it)
        }

        // Only build the child once. On a recreate the FragmentManager has already restored
        // it — replacing it again threw away its saved state, which is why toggling a theme
        // setting made the whole list jump back to the top.
        if (childFragmentManager.findFragmentById(R.id.genericFragmentContainer) == null) {
            childFragmentManager.beginTransaction()
                .replace(R.id.genericFragmentContainer, creator())
                .commit()
        }
    }

    companion object {
        fun PreferenceFragmentCompat.configure() {
            listView?.apply {
                clipToPadding = false
                applyInsets { applyContentInsets(it) }
                isVerticalScrollBarEnabled = false
                FastScrollerHelper.applyTo(this)
            }
        }
    }
}