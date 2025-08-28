package com.example.smarttodo.ui

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class TaskItemDecoration(private val spacing: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val position = parent.getChildAdapterPosition(view)
        if (position > 0) {
            outRect.top = spacing
        }
        outRect.left = spacing / 2
        outRect.right = spacing / 2
    }
}
