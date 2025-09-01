package com.example.smarttodo.ui

import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode

import android.graphics.Canvas
import android.graphics.Color
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
    private val completeBackground = ColorDrawable(Color.parseColor("#4CAF50"))
    private val deleteBackground = ColorDrawable(Color.parseColor("#F44336"))

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val position = viewHolder.adapterPosition
        when (direction) {
            ItemTouchHelper.RIGHT -> onSwipeRight(position)
            ItemTouchHelper.LEFT -> onSwipeLeft(position)
        }
    }

    override fun onChildDraw(
        c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
        dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean
    ) {
        val itemView = viewHolder.itemView
        val itemHeight = itemView.bottom - itemView.top
        val isCanceled = dX == 0f && !isCurrentlyActive

        if (isCanceled) {
            clearCanvas(c, itemView.right + dX, itemView.top.toFloat(), itemView.right.toFloat(), itemView.bottom.toFloat())
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            return
        }

        if (completeIcon == null) {
            completeIcon = ContextCompat.getDrawable(recyclerView.context, R.drawable.ic_check)
            deleteIcon = ContextCompat.getDrawable(recyclerView.context, R.drawable.ic_delete)
        }

        if (dX > 0) {
            completeBackground.setBounds(itemView.left, itemView.top, itemView.left + dX.toInt(), itemView.bottom)
            completeBackground.draw(c)
            completeIcon?.let {
                val iconTop = itemView.top + (itemHeight - it.intrinsicHeight) / 2
                val iconMargin = (itemHeight - it.intrinsicHeight) / 2
                val iconLeft = itemView.left + iconMargin
                val iconRight = itemView.left + iconMargin + it.intrinsicWidth
                val iconBottom = iconTop + it.intrinsicHeight
                it.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                it.draw(c)
            }
        } else if (dX < 0) {
            deleteBackground.setBounds(itemView.right + dX.toInt(), itemView.top, itemView.right, itemView.bottom)
            deleteBackground.draw(c)
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

    private fun clearCanvas(c: Canvas?, left: Float, top: Float, right: Float, bottom: Float) {
        val transparentPaint = Paint().apply {
            color = Color.TRANSPARENT
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        }
        c?.drawRect(left, top, right, bottom, transparentPaint)
    }


}
