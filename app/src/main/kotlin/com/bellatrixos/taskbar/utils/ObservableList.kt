package com.bellatrixos.taskbar.utils

/** Daftar yang memberi tahu pendengar setiap kali isinya berubah. */
class ObservableList<T> : ArrayList<T>() {
    interface OnListChangedListener<T> {
        fun a(obj: Any?)
        fun b()
    }

    @JvmField var u: OnListChangedListener<T>? = null

    override fun add(element: T): Boolean {
        val added = super.add(element)
        if (added) u?.a(element)
        return added
    }

    override fun addAll(elements: Collection<T>): Boolean {
        val added = super.addAll(elements)
        if (added) elements.forEach { u?.a(it) }
        return added
    }

    override fun removeAt(index: Int): T {
        val removed = super.removeAt(index)
        u?.b()
        return removed
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        val removed = super.removeAll(elements)
        if (removed) elements.forEach { u?.b() }
        return removed
    }
}
