package com.mgt.openmusic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.bottom_sheet_download.*

class DownloadBottomSheet:BottomSheetDialogFragment() {
    private lateinit var adapter: SongAdapter
    private val adapterClickListener = object :SongAdapter.ClickListener{
        override fun onDownload(position: Int) {
        }

        override fun onOpen(position: Int) {
        }

        override fun onCancel(position: Int) {
        }

        override fun onShare(position: Int) {
        }

        override fun onPlay(position: Int) {
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.TransparentBottomSheetDialogTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_download, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    private fun initView(){
//        adapter = SongAdapter(adapterClickListener, )
//        recyclerView.adapter =
    }
}