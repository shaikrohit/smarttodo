package com.example.smarttodo.ui

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * A [RecyclerView.ItemDecoration] that adds custom spacing around items in a RecyclerView.
 * It applies vertical spacing above items (except for the first one) and
 * horizontal spacing to the left and right of each item.
 *
 * @property spacing The base amount of spacing (in pixels) to be applied.
 *                   Vertical spacing will use this full amount.
 *                   Horizontal spacing will use half of this amount on each side.
 */
class TaskItemDecoration(private val spacing: Int) : RecyclerView.ItemDecoration() {

    /**
     * Retrieves the offsets for the given item. Each field of `outRect` specifies
     * the number of pixels that the item view should be inset by, similar to padding.
     *
     * This implementation adds:
     * - Top spacing to all items except the first one in the list.
     * - Half of the specified [spacing] to the left and right of every item.
     *
     * @param outRect Rect to receive the output.
     * @param view The View CSEMntitled (child) that this item decoration is decorating.
     * @param parent RecyclerView CSEMntitled this ItemDecoration is decorating.
     * @param state The current state of RecyclerView.
     */
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        // Get the adapter position of the item.
        val position = parent.getChildAdapterPosition(view)

        // Add top spacing only if it's not the first item, to create separation between items.
        if (position > 0) {
            outRect.top = spacing
        }

        // Apply half of the spacing to the left and right sides of the item.
        // This creates padding on the sides of each item.
        outRect.left = spacing / 2
        outRect.right = spacing / 2

        // No bottom spacing is explicitly set here, meaning items will be spaced by the 'top' of the next item.
        // If bottom padding for the last item is desired, it would need additional logic
        // or a RecyclerView.paddingBottom.
    }
}
