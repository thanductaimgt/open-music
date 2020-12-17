package com.mgt.openmusic

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_search_history.view.*
import kotlinx.android.synthetic.main.item_song.view.*


class SearchHistoryAdapter(
    private val clickListener: ClickListener
) : RecyclerView.Adapter<SearchHistoryAdapter.SearchHistoryHolder>() {
    var searchHistories = ArrayList<SearchHistory>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchHistoryHolder {
        return SearchHistoryHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_search_history, parent, false)
        )
    }

    override fun onBindViewHolder(holder: SearchHistoryHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int {
        return searchHistories.size
    }

    inner class SearchHistoryHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        fun bind(position: Int) {
            val searchHistory = searchHistories[position]
            itemView.apply {
                searchHistoryTextView.text = searchHistory.text

                deleteButton.setOnClickListener(this@SearchHistoryHolder)
                setOnClickListener(this@SearchHistoryHolder)
            }
        }

        override fun onClick(v: View?) {
            when (v?.id) {
                R.id.deleteButton -> clickListener.onDelete(adapterPosition)
                R.id.rootItemView -> clickListener.onSelect(adapterPosition)
            }
        }
    }

    interface ClickListener {
        fun onDelete(position: Int)
        fun onSelect(position: Int)
    }
}