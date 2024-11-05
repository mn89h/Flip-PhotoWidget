package com.pmmn.flipphotowidget

class ResettableIterator<T>(private val list: List<T>) : Iterator<T> {
    private var index = 0

    private fun updateIndex() {
        index += 1
        if (index >= list.size) {
            // Reset the index when the end is reached
            index = 0
        }
    }

    override fun hasNext(): Boolean {
        return list.isNotEmpty()
    }

    override fun next(): T {
        val retVal = list[index]
        updateIndex()
        return retVal
    }

    fun getCurrentIndex(): Int {
        return index
    }

    fun setCurrentIndex(newIndex: Int): Boolean {
        if (newIndex < 0 || newIndex >= list.size) {
            return false
        }
        else {
            index = newIndex
            return true
        }
    }
}