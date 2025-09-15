package com.example.smarttodo.ui

import android.graphics.Canvas
// import android.graphics.Color // No longer needed for Color.parseColor
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.smarttodo.R

/**
 * An [ItemTouchHelper.SimpleCallback] implementation to add swipe-to-action
 * functionality (left and right swipes) to items in a [RecyclerView].
 * It provides visual feedback by drawing a colored background and an icon
 * as the item is swiped.
 *
 * @param onSwipeRight Callback function invoked when an item is swiped to the right.
 *                     It receives the adapter position of the swiped item.
 * @param onSwipeLeft Callback function invoked when an item is swiped to the left.
 *                    It receives the adapter position of the swiped item.
 */
class SwipeGestureHelper(
    private val onSwipeRight: (Int) -> Unit,
    private val onSwipeLeft: (Int) -> Unit
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

    // Drawable for the "complete" action icon (e.g., a checkmark). Lazily initialized.
    private var completeIcon: Drawable? = null
    // Drawable for the "delete" action icon (e.g., a trash can). Lazily initialized.
    private var deleteIcon: Drawable? = null

    // Background for right swipe (complete action). Lazily initialized in onChildDraw.
    private var completeBackground: ColorDrawable? = null
    // Background for left swipe (delete action). Lazily initialized in onChildDraw.
    private var deleteBackground: ColorDrawable? = null

    /**
     * Called when ItemTouchHelper wants to move the dragged item from its old position to
     * the new position. This implementation does not support drag-and-drop.
     *
     * @return False, as drag-and-drop is not handled by this helper.
     */
    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return false // Drag and drop is not supported.
    }

    /**
     * Called when a ViewHolder is swiped by the user.
     *
     * @param viewHolder The ViewHolder which has been swiped.
     * @param direction The direction to which the ViewHolder is swiped (e.g., [ItemTouchHelper.LEFT] or [ItemTouchHelper.RIGHT]).
     */
    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val position = viewHolder.bindingAdapterPosition // Use bindingAdapterPosition for safety.
        if (position == RecyclerView.NO_POSITION) return // Ignore if position is invalid.

        when (direction) {
            ItemTouchHelper.RIGHT -> onSwipeRight(position)
            ItemTouchHelper.LEFT -> onSwipeLeft(position)
        }
    }

    /**
     * Called by ItemTouchHelper on RecyclerView's onDraw callback.
     * This method is responsible for drawing the custom background and icon
     * under the swiping item.
     *
     * @param c The canvas which ItemTouchHelper is drawing on.
     * @param recyclerView The RecyclerView to which ItemTouchHelper is attached.
     * @param viewHolder The ViewHolder which is being interacted by the User or it was interacted and simply animating back to its original state.
     * @param dX The amount of horizontal displacement caused by user's action.
     * @param dY The amount of vertical displacement caused by user's action.
     * @param actionState The type of interaction on the View. Is either [ItemTouchHelper.ACTION_STATE_DRAG] or [ItemTouchHelper.ACTION_STATE_SWIPE].
     * @param isCurrentlyActive True if this view is currently being controlled by the user or false it is animating back to its original state.
     */
    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float, // Horizontal displacement
        dY: Float, // Vertical displacement (not used for horizontal swipes)
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val itemView = viewHolder.itemView
        val itemHeight = itemView.bottom - itemView.top
        // Check if the swipe was canceled (item is returning to original position without action).
        val isCanceled = dX == 0f && !isCurrentlyActive

        if (isCanceled) {
            // If swipe is canceled, clear the canvas area where custom drawing might have occurred.
            clearCanvas(c, itemView.right + dX, itemView.top.toFloat(), itemView.right.toFloat(), itemView.bottom.toFloat())
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            return
        }

        // Lazily initialize icons and backgrounds if they haven't been loaded yet.
        if (completeIcon == null) { // Assuming all are loaded together on first use.
            completeIcon = ContextCompat.getDrawable(recyclerView.context, R.drawable.ic_check)
            deleteIcon = ContextCompat.getDrawable(recyclerView.context, R.drawable.ic_delete)
            completeBackground = ColorDrawable(ContextCompat.getColor(recyclerView.context, R.color.complete_action))
            deleteBackground = ColorDrawable(ContextCompat.getColor(recyclerView.context, R.color.delete_action))
        }

        // Drawing for right swipe (positive dX)
        if (dX > 0) {
            // Draw green background for "complete" action.
            completeBackground?.setBounds(itemView.left, itemView.top, itemView.left + dX.toInt(), itemView.bottom)
            completeBackground?.draw(c)

            // Draw "complete" icon.
            completeIcon?.let { icon ->
                val iconTop = itemView.top + (itemHeight - icon.intrinsicHeight) / 2
                val iconMargin = (itemHeight - icon.intrinsicHeight) / 2 // Center icon vertically.
                val iconLeft = itemView.left + iconMargin
                val iconRight = itemView.left + iconMargin + icon.intrinsicWidth
                val iconBottom = iconTop + icon.intrinsicHeight
                icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                icon.draw(c)
            }
        }
        // Drawing for left swipe (negative dX)
        else if (dX < 0) {
            // Draw red background for "delete" action.
            deleteBackground?.setBounds(itemView.right + dX.toInt(), itemView.top, itemView.right, itemView.bottom)
            deleteBackground?.draw(c)

            // Draw "delete" icon.
            deleteIcon?.let { icon ->
                val iconTop = itemView.top + (itemHeight - icon.intrinsicHeight) / 2
                val iconMargin = (itemHeight - icon.intrinsicHeight) / 2 // Center icon vertically.
                val iconLeft = itemView.right - iconMargin - icon.intrinsicWidth
                val iconRight = itemView.right - iconMargin
                val iconBottom = iconTop + icon.intrinsicHeight
                icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                icon.draw(c)
            }
        }

        // Allow ItemTouchHelper to handle the default swipe animation (moving the item view).
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }

    /**
     * Clears a rectangular area on the canvas by drawing a transparent rectangle
     * with [PorterDuff.Mode.CLEAR]. Used to remove custom drawings when a swipe is canceled.
     *
     * @param c The canvas to draw on. Can be null.
     * @param left The left side of the rectangle to clear.
     * @param top The top side of the rectangle to clear.
     * @param right The right side of the rectangle to clear.
     * @param bottom The bottom side of the rectangle to clear.
     */
    private fun clearCanvas(c: Canvas?, left: Float, top: Float, right: Float, bottom: Float) {
        c?.drawRect(left, top, right, bottom, Paint().apply {
            // Use a paint with CLEAR mode to erase the area.
            // color = Color.TRANSPARENT // Color.TRANSPARENT is 0, default for Paint is black
            alpha = 0 // Ensure transparent
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        })
    }
}
