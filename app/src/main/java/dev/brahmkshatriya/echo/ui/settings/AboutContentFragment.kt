package dev.brahmkshatriya.echo.ui.settings

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import dev.brahmkshatriya.echo.BuildConfig
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.databinding.FragmentAboutContentBinding
import dev.brahmkshatriya.echo.ui.extensions.ExtensionInfoFragment.Companion.openLink
import dev.brahmkshatriya.echo.utils.ui.AutoClearedValue.Companion.autoCleared

/** The scrolling body of the About screen (added from ArchiveTune). */
class AboutContentFragment : Fragment() {

    private var binding: FragmentAboutContentBinding by autoCleared()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAboutContentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val context = requireContext()
        binding.appName.text = getString(R.string.app_name)
        binding.appVersion.text =
            getString(R.string.version_label) + ": " + BuildConfig.VERSION_NAME
        binding.buildType.text =
            getString(R.string.build_type_label) + ": " + BuildConfig.BUILD_TYPE
        binding.packageName.text =
            getString(R.string.package_label) + ": " + context.packageName
        binding.deviceInfo.text =
            getString(R.string.device_label) + ": " + Build.MODEL + " · Android " +
                Build.VERSION.RELEASE

        binding.githubLink.setOnClickListener {
            requireActivity().openLink("https://github.com/${getString(R.string.app_github_repo)}")
        }
        binding.wikiLink.setOnClickListener {
            requireActivity().openLink("https://wotaku.wiki/guides/music/echo")
        }
    }
}
