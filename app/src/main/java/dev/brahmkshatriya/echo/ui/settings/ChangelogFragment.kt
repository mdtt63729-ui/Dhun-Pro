package dev.brahmkshatriya.echo.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.databinding.FragmentChangelogBinding
import dev.brahmkshatriya.echo.ui.common.UiViewModel.Companion.applyBackPressCallback
import dev.brahmkshatriya.echo.utils.Serializer.toData
import dev.brahmkshatriya.echo.utils.net.HttpClient
import dev.brahmkshatriya.echo.utils.ui.AutoClearedValue.Companion.autoCleared
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.Request
import okio.use

/**
 * Added from ArchiveTune: a changelog screen built from the app's own GitHub releases.
 */
class ChangelogFragment : Fragment() {

    private var binding: FragmentChangelogBinding by autoCleared()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChangelogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyBackPressCallback()
        binding.toolBar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
        load()
    }

    private fun load() = viewLifecycleOwner.lifecycleScope.launch {
        binding.loading.isVisible = true
        val text = runCatching { fetch() }.getOrNull()
        binding.loading.isVisible = false
        binding.changelogText.text =
            text?.takeIf { it.isNotBlank() } ?: getString(R.string.no_changelog)
    }

    private suspend fun fetch(): String = withContext(Dispatchers.IO) {
        val repo = getString(R.string.app_github_repo)
        val request = Request.Builder()
            .url("https://api.github.com/repos/$repo/releases?per_page=15")
            .build()
        val body = HttpClient.client(requireContext()).newCall(request).execute().use {
            it.body.string()
        }
        body.toData<List<ChangelogRelease>>().getOrThrow()
            .filter { it.tagName.isNotBlank() }
            .joinToString("\n\n\n") { release ->
                val date = release.publishedAt.take(10)
                val notes = release.body.orEmpty().trim().ifBlank {
                    getString(R.string.no_changelog)
                }
                "${release.tagName}   ·   $date\n$notes"
            }
    }

    @Serializable
    data class ChangelogRelease(
        @SerialName("tag_name") val tagName: String,
        @SerialName("published_at") val publishedAt: String = "",
        val body: String? = null,
    )
}
