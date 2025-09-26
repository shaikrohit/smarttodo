package com.example.smarttodo.ui

import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.smarttodo.R

class SwipeGestureHelper(
    private val onSwipeRight: (Int) -> Unit,
    private val onSwipeLeft: (Int) -> Unit
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

    private var completeIcon: Drawable? = null
    private var deleteIcon: Drawable? = null
    private var completeBackground: ColorDrawable? = null
    private var deleteBackground: ColorDrawable? = null

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return false // Drag and drop is not supported.
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val position = viewHolder.bindingAdapterPosition
        if (position == RecyclerView.NO_POSITION) return

        when (direction) {
            ItemTouchHelper.RIGHT -> onSwipeRight(position)
            ItemTouchHelper.LEFT -> onSwipeLeft(position)
        }
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val itemView = viewHolder.itemView
        val itemHeight = itemView.bottom - itemView.top

        if (completeIcon == null) { // Lazily initialize colors and icons
            completeIcon = ContextCompat.getDrawable(recyclerView.context, R.drawable.ic_check)
            deleteIcon = ContextCompat.getDrawable(recyclerView.context, R.drawable.ic_delete)
            completeBackground = ColorDrawable(ContextCompat.getColor(recyclerView.context, R.color.complete_action))
            deleteBackground = ColorDrawable(ContextCompat.getColor(recyclerView.context, R.color.delete_action))
        }

        if (dX > 0) { // Swiping to the right
            completeBackground?.setBounds(itemView.left, itemView.top, itemView.left + dX.toInt(), itemView.bottom)
            completeBackground?.draw(c)

            completeIcon?.let {
                val iconTop = itemView.top + (itemHeight - it.intrinsicHeight) / 2
                val iconMargin = (itemHeight - it.intrinsicHeight) / 2
                val iconLeft = itemView.left + iconMargin
                val iconRight = itemView.left + iconMargin + it.intrinsicWidth
                val iconBottom = iconTop + it.intrinsicHeight
                it.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                it.draw(c)
            }
        } else if (dX < 0) { // Swiping to the left
            deleteBackground?.setBounds(itemView.right + dX.toInt(), itemView.top, itemView.right, itemView.bottom)
            deleteBackground?.draw(c)

            deleteIcon?.let {
                val iconTop = itemView.top + (itemHeight - it.intrinsicHeight) / 2
                val iconMargin = (itemHeight - it.intrinsicHeight) / 2
                val iconLeft = itemView.right - iconMargin - it.intrinsicWidth
                val iconRight = itemView.right - iconMargin
                val iconBottom = iconTop + it.intrinsicHeight
                it.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                it.draw(c)
            }
        }

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }
}
