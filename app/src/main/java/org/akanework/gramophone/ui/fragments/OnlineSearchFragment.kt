package org.akanework.gramophone.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.playYouTubeStream
import org.akanework.gramophone.ui.MainActivity
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.SongItem

class OnlineSearchFragment : BaseFragment(null) {
    private lateinit var input: EditText
    private lateinit var go: Button
    private lateinit var status: TextView
    private lateinit var progress: ProgressBar
    private lateinit var list: RecyclerView
    private lateinit var adapter: OnlineResultsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val context = requireContext()
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 24)
        }

        input = EditText(context).apply {
            hint = "Search YouTube Music"
        }.also { root.addView(it) }

        go = Button(context).apply { text = "Search" }
        go.setOnClickListener { runSearch(input.text.toString()) }
        root.addView(go)

        progress = ProgressBar(context).apply { visibility = View.GONE }.also { root.addView(it) }

        status = TextView(context).also {
            it.textSize = 14f
            it.setPadding(0, 24, 0, 24)
            root.addView(it)
        }

        adapter = OnlineResultsAdapter()
        list = RecyclerView(context).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@OnlineSearchFragment.adapter
        }.also { root.addView(it) }

        adapter.onPlay = { item ->
            val activity = requireActivity() as MainActivity
            lifecycleScope.launch {
                status.text = "Loading stream for ${item.title}..."
                val error = playYouTubeStream(requireContext(), activity.getPlayer(), item.id)
                status.text = if (error == null) "Now playing: ${item.title}" else "Could not stream: $error"
            }
        }

        return root
    }

    private fun runSearch(query: String) {
        if (query.isBlank()) return
        progress.visibility = View.VISIBLE
        status.text = "Searching..."
        lifecycleScope.launch {
            try {
                val result = YouTube.search(
                    query,
                    YouTube.SearchFilter.FILTER_SONG,
                )
                val songs = result.getOrNull()?.items?.filterIsInstance<SongItem>().orEmpty()
                if (songs.isEmpty()) {
                    status.text = "No online results."
                } else {
                    status.text = "${songs.size} online result(s)"
                    adapter.submit(songs)
                }
            } catch (e: Exception) {
                status.text = "Search failed: ${e.message}"
            } finally {
                progress.visibility = View.GONE
            }
        }
    }

    companion object {
        var lastStreamError: String? = null
    }
}

private class OnlineResultsAdapter :
    androidx.recyclerview.widget.RecyclerView.Adapter<OnlineResultsAdapter.VH>() {
    private val items = mutableListOf<SongItem>()
    var onPlay: ((SongItem) -> Unit)? = null

    fun submit(newItems: List<SongItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val row = LinearLayout(parent.context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 16, 0, 16)
        }
        val title = TextView(parent.context).apply { textSize = 16f }
        val subtitle = TextView(parent.context).apply { textSize = 12f }
        row.addView(title)
        row.addView(subtitle)
        return VH(row, title, subtitle)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.title.text = item.title
        holder.subtitle.text = item.artists.joinToString { it.name }
        holder.itemView.setOnClickListener { onPlay?.invoke(item) }
    }

    class VH(itemView: View, val title: TextView, val subtitle: TextView) :
        androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView)
}
