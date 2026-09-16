package dev.brahmkshatriya.echo.ui.extensions.manage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.Extension
import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.models.ExtensionType
import dev.brahmkshatriya.echo.databinding.FragmentManageExtensionsBinding
import dev.brahmkshatriya.echo.ui.common.FragmentUtils.openFragment
import dev.brahmkshatriya.echo.ui.common.UiViewModel.Companion.applyBackPressCallback
import dev.brahmkshatriya.echo.ui.common.UiViewModel.Companion.applyInsets
import dev.brahmkshatriya.echo.ui.common.UiViewModel.Companion.applyInsetsWithChild
import dev.brahmkshatriya.echo.ui.extensions.ExtensionInfoFragment
import dev.brahmkshatriya.echo.ui.extensions.ExtensionInfoPreference.Companion.getType
import dev.brahmkshatriya.echo.ui.extensions.ExtensionsViewModel
import dev.brahmkshatriya.echo.utils.ContextUtils.observe
import dev.brahmkshatriya.echo.utils.ui.AnimationUtils.setupTransition
import dev.brahmkshatriya.echo.utils.ui.AutoClearedValue.Companion.autoCleared
import dev.brahmkshatriya.echo.utils.ui.FastScrollerHelper
import dev.brahmkshatriya.echo.utils.ui.UiUtils.configureAppBar
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class ManageExtensionsFragment : Fragment() {
    private var binding by autoCleared<FragmentManageExtensionsBinding>()
    private val viewModel by activityViewModel<ExtensionsViewModel>()
    private var allExtensions: List<Extension<*>> = emptyList()
    private var searchQuery: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentManageExtensionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupTransition(view)
        applyInsetsWithChild(binding.appBarLayout, binding.recyclerView, 104) {
            binding.fabContainer.applyInsets(it)
        }
        applyBackPressCallback()
        binding.appBarLayout.configureAppBar { offset ->
            binding.appBarOutline.alpha = offset
            binding.appBarOutline.isVisible = offset > 0
            binding.toolBar.alpha = 1 - offset
        }
        binding.toolBar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
        binding.toolBar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_update -> {
                    viewModel.update(requireActivity(), true)
                    true
                }
                else -> false
            }
        }

        FastScrollerHelper.applyTo(binding.recyclerView)

        val tabs = ExtensionType.entries.map {
            binding.tabLayout.newTab().apply {
                setText(getType(it))
            }
        }
        binding.tabLayout.run {
            tabs.forEach { addTab(it) }
        }

        val callback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPos = viewHolder.bindingAdapterPosition
                val toPos = target.bindingAdapterPosition
                viewModel.moveExtensionItem(toPos, fromPos)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

            override fun getMovementFlags(
                recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder
            ) = makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0)

        }

        val touchHelper = ItemTouchHelper(callback)
        val extensionAdapter = ExtensionAdapter(object : ExtensionAdapter.Listener {
            override fun onClick(extension: Extension<*>, view: View) {
                openFragment<ExtensionInfoFragment>(
                    view, ExtensionInfoFragment.getBundle(extension)
                )
            }

            override fun onDragHandleTouched(viewHolder: ExtensionAdapter.ViewHolder) {
                touchHelper.startDrag(viewHolder)
            }

            override fun onOpenClick(extension: Extension<*>) {
                viewModel.onExtensionSelected(extension as MusicExtension)
                parentFragmentManager.popBackStack()
                parentFragmentManager.popBackStack()
            }
        })

        suspend fun renderExtensions() {
            val filtered = if (searchQuery.isBlank()) {
                allExtensions
            } else {
                allExtensions.filter { extension ->
                    extension.name.contains(searchQuery, ignoreCase = true) ||
                        extension.id.contains(searchQuery, ignoreCase = true)
                }
            }
            extensionAdapter.submit(
                filtered,
                viewModel.lastSelectedManageExt.value,
                viewModel.app.settings,
                persistPriority = searchQuery.isBlank(),
            )
        }

        binding.toolBar.menu.findItem(R.id.menu_search)?.actionView?.let { actionView ->
            (actionView as? SearchView)?.apply {
                queryHint = getString(R.string.search)
                setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?) = true
                    override fun onQueryTextChange(newText: String?): Boolean {
                        searchQuery = newText.orEmpty().trim()
                        viewLifecycleOwner.lifecycleScope.launch { renderExtensions() }
                        return true
                    }
                })
            }
        }

        observe(viewModel.manageExtListFlow) { list ->
            allExtensions = list
            renderExtensions()
        }

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            fun select(tab: TabLayout.Tab) {
                viewModel.lastSelectedManageExt.value = tab.position
                searchQuery = ""
                binding.toolBar.menu.findItem(R.id.menu_search)?.collapseActionView()
                val type = ExtensionType.entries[viewModel.lastSelectedManageExt.value]
                allExtensions = viewModel.extensionLoader.getFlow(type).value
                viewLifecycleOwner.lifecycleScope.launch { renderExtensions() }
            }

            override fun onTabSelected(tab: TabLayout.Tab) = select(tab)
            override fun onTabReselected(tab: TabLayout.Tab) = select(tab)
            override fun onTabUnselected(tab: TabLayout.Tab) {}
        })

        binding.tabLayout.selectTab(tabs[viewModel.lastSelectedManageExt.value])
        binding.recyclerView.adapter = extensionAdapter.withEmptyAdapter()
        touchHelper.attachToRecyclerView(binding.recyclerView)
    }
}