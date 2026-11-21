package com.bellatrixos.windows

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.bellatrixos.R
import com.bellatrixos.UIFragment
import com.bellatrixos.app_explorer.UIExplorer
import com.bellatrixos.linux.UILinuxWindow
import com.bellatrixos.taskbar.TaskBarAdapter
import com.bellatrixos.taskbar.TaskBarDB
import com.bellatrixos.taskbar.TaskBarItem
import com.bellatrixos.windows.drivers.Display
import com.bellatrixos.windows.installed_apps.AppsDatabase
import com.bellatrixos.windows.installed_apps.InstalledAppsDB
import com.bellatrixos.windows.installed_apps.models.InstalledApp
import com.bellatrixos.windows.switcher.UIInstanceSwitcher
import java.util.ArrayList

/** Registry jendela yang sedang aktif. */
object Windows {
    @JvmField var f4880a: Int = -1
    @JvmField var b: Int = -1
    @JvmField var c: Boolean = true
    @JvmField var f4881d: FragmentManager? = null
    @JvmField var f4882e: UIFragment? = null
    @JvmField var f4883f: UIExplorer? = null
    @JvmField var g: Int = R.anim.app_anim_open
    @JvmField var f4884h: Int = R.anim.app_anim_close

    @JvmStatic
    fun a(uIFragment: UIFragment?, taskBarAdapter: TaskBarAdapter?) {
        val manager = f4881d ?: return
        val fragment = uIFragment ?: return
        if (!fragment.isAdded || fragment.isRemoving) return
        val tag = fragment.tag ?: return
        val existing = manager.findFragmentByTag(tag) ?: return
        if (existing.isRemoving) return
        if (f4882e === fragment) {
            fragment.setWindowFocused(false)
            f4882e = null
        }
        val tx = manager.beginTransaction()
        tx.setCustomAnimations(0, R.anim.app_anim_close, 0, 0)
        tx.hide(existing)
        if (taskBarAdapter != null) {
            tx.runOnCommit(a(taskBarAdapter, 2, fragment))
        }
        i(manager, tx)
    }

    /**
     * True close: remove the window from the fragment manager and from the taskbar.
     * Hiding (`a`) is only a minimise; the X button must not leave a dead window in
     * Recents/taskbar after the Linux toplevel is gone.
     */
    @JvmStatic
    fun close(uIFragment: UIFragment?, taskBarAdapter: TaskBarAdapter?) {
        val fragment = uIFragment ?: return
        val adapter = taskBarAdapter ?: return
        val manager = f4881d ?: return
        if (f4882e === fragment) f4882e = null
        fragment.setWindowFocused(false)
        if (fragment.isAdded && !fragment.isRemoving) {
            val tx = manager.beginTransaction()
            tx.setCustomAnimations(0, f4884h, 0, 0)
            tx.remove(fragment)
            i(manager, tx)
        }
        j(0, adapter, fragment)
    }

    @JvmStatic
    fun b(str: String, str2: String?): UIFragment? {
        val manager = f4881d ?: return null
        var found: UIFragment? = null
        for (fragment in manager.fragments) {
            val tag = fragment.tag ?: continue
            if (!tag.startsWith(str) || fragment !is UIFragment) continue
            if (found == null) found = fragment
            if (fragment.n0()) {
                val data = fragment.d0()
                if (str2 == null) return fragment
                if (data != null && data == str2) return fragment
            }
        }
        if (found == null || found.n0()) return null
        return found
    }

    @JvmStatic
    fun c(str: String): ArrayList<UIFragment> = c(str, null)

    /** Instances of [str], optionally ignoring one that is being closed right now. */
    @JvmStatic
    fun c(str: String, exclude: UIFragment?): ArrayList<UIFragment> {
        val manager = f4881d ?: return ArrayList()
        val list = ArrayList<UIFragment>()
        for (fragment in manager.fragments) {
            if (fragment is UIFragment && fragment !== exclude && !fragment.isRemoving) {
                val tag = fragment.tag
                if (tag != null && tag.startsWith(str)) {
                    list.add(fragment)
                }
            }
        }
        return list
    }

    /** The taskbar key of a window. Plain terminals use the pinned Terminal button, not a new recent. */
    @JvmStatic
    fun taskTag(fragment: UIFragment): String? {
        val tag = fragment.tag ?: return null
        val base = tag.substringBefore("::::")
        return if (base == UILinuxWindow.TAG) "UITerminal" else base
    }

    /** Remaining windows for a taskbar key, excluding one that is closing. */
    private fun countTaskTag(taskTagValue: String, exclude: UIFragment?): Int {
        val manager = f4881d ?: return 0
        var count = 0
        for (fragment in manager.fragments) {
            if (fragment is UIFragment &&
                fragment !== exclude &&
                !fragment.isRemoving &&
                taskTag(fragment) == taskTagValue
            ) {
                count++
            }
        }
        return count
    }

    @JvmStatic
    fun d(window: Window, str: String): InstalledApp {
        val existing = InstalledAppsDB.c(str)
        if (existing != null) {
            window.a(existing.d())
            return existing
        }
        if (InstalledAppsDB.f4955a == null) {
            return InstalledApp(AppsDatabase.b[str], str, str, window.c, false)
        }
        val created = InstalledApp(AppsDatabase.b[str], str, str, window.c, false)
        val list = InstalledAppsDB.b().toMutableList()
        list.add(created)
        InstalledAppsDB.b[str] = list.size - 1
        InstalledAppsDB.f(list, false)
        return created
    }

    @JvmStatic
    fun e(fragmentManager: FragmentManager, uIFragment: UIFragment) {
        val tx = fragmentManager.beginTransaction()
        tx.setCustomAnimations(g, 0, 0, 0)
        tx.show(uIFragment)
        i(fragmentManager, tx)
    }

    @JvmStatic
    fun f(uIFragment: UIFragment?, taskBarAdapter: TaskBarAdapter?) {
        val fragment = uIFragment ?: return
        val adapter = taskBarAdapter ?: return
        if (!fragment.isAdded || fragment.isRemoving) return
        val targetIndex = TaskBarDB.a(taskTag(fragment) ?: "")
        val event = if (b != targetIndex) {
            if (!fragment.isVisible) {
                e(f4881d ?: return, fragment)
            }
            fragment.g0()
            4
        } else if (!fragment.isVisible) {
            e(f4881d ?: return, fragment)
            fragment.g0()
            j(2, adapter, fragment)
            return
        } else {
            val manager = f4881d ?: return
            if (!manager.isStateSaved) {
                val tx = manager.beginTransaction()
                tx.setCustomAnimations(0, f4884h, 0, 0)
                tx.hide(fragment)
                i(manager, tx)
            }
            3
        }
        j(event, adapter, fragment)
    }

    @JvmStatic
    fun g(uIFragment: UIFragment, str: String, taskBarAdapter: TaskBarAdapter, str2: String?) {
        val manager = f4881d ?: return
        if (str2 != null) {
            val bundle = Bundle()
            bundle.putString("path", str2)
            bundle.putString("packageName", str)
            uIFragment.arguments = bundle
        }
        val fragments = manager.fragments
        c = if (fragments.isEmpty() || f4882e == null) true else !f4882e!!.isVisible
        val tx = manager.beginTransaction()
        if (!uIFragment.isAdded) {
            tx.setCustomAnimations(R.anim.app_anim_open, 0, R.anim.app_anim_open, 0)
            tx.add(R.id.apps_window, uIFragment, "$str::::${System.currentTimeMillis()}")
        } else if (!uIFragment.isVisible && str2 == null) {
            tx.setCustomAnimations(g, 0, 0, 0)
            tx.show(uIFragment)
        } else if (str2 != null) {
            tx.setCustomAnimations(g, 0, 0, 0)
            tx.show(uIFragment)
            uIFragment.k0(str2)
        }
        if (uIFragment.n0() && c(str).isNotEmpty()) {
            taskBarAdapter.g(str, false, true)
        }
        i(manager, tx)
        j(1, taskBarAdapter, uIFragment)
    }

    @JvmStatic
    fun h(
        context: Context?,
        str: String,
        taskBarAdapter: TaskBarAdapter,
        str2: String?,
        view: View?,
        z: Boolean
    ) {
        if (!z) {
            var fragment = b(str, str2)
            if (fragment == null) fragment = WindowManager.b(str, str2)
            if (fragment != null) {
                if (fragment.isAdded && (str2 == null || fragment.s0 == 1)) {
                    f(fragment, taskBarAdapter)
                } else {
                    g(fragment, str, taskBarAdapter, str2)
                }
            }
            return
        }
        if (c(str).size <= 1) {
            var fragment = b(str, null)
            if (fragment == null) fragment = WindowManager.b(str, null)
            if (fragment != null) {
                if (fragment.isAdded) {
                    f(fragment, taskBarAdapter)
                } else {
                    g(fragment, str, taskBarAdapter, null)
                }
            }
            return
        }
        if (UIInstanceSwitcher.c == null && context != null) {
            UIInstanceSwitcher.c = UIInstanceSwitcher(context)
        }
        val switcher = UIInstanceSwitcher.c ?: return
        UIInstanceSwitcher.c = switcher
        val anchor = view ?: return
        if (anchor.windowToken == null || !anchor.isAttachedToWindow) return
        val instances = c(str)
        if (instances.isEmpty()) return
        val adapter = switcher.b
        adapter.z.clear()
        adapter.z.addAll(instances)
        adapter.notifyDataSetChanged()
        switcher.a()
        var offsetX = (anchor.width - switcher.f4960a.measuredWidth) / 2
        if (Display.f4898a) offsetX = -offsetX
        switcher.showAsDropDown(anchor, offsetX, (-anchor.height) / 4)
    }

    @JvmStatic
    fun i(fragmentManager: FragmentManager, fragmentTransaction: FragmentTransaction) {
        if (fragmentManager.isStateSaved) {
            fragmentTransaction.commitAllowingStateLoss()
        } else {
            fragmentTransaction.commit()
        }
    }

    @JvmStatic
    fun j(i2: Int, taskBarAdapter: TaskBarAdapter, uIFragment: UIFragment) {
        val tag = taskTag(uIFragment)
        val tagStr = tag ?: ""
        val itemIndex = TaskBarDB.a(tagStr)
        if (i2 == 0) {
            if (itemIndex == -1) return
            val count = countTaskTag(tagStr, uIFragment)
            if (count > 0) {
                taskBarAdapter.g(tagStr, true, count > 1)
                return
            }
            if (itemIndex < 0) {
                taskBarAdapter.hashCode()
                return
            }
            val list = taskBarAdapter.D
            if (itemIndex >= list.size) return
            taskBarAdapter.A = itemIndex
            taskBarAdapter.y = 1
            val item = list[itemIndex]
            if (item.h()) {
                item.j()
                taskBarAdapter.notifyItemChanged(itemIndex)
            } else {
                list.removeAt(itemIndex)
                TaskBarDB.d(list)
                taskBarAdapter.B = list.size
                taskBarAdapter.notifyItemRemoved(itemIndex)
            }
            TaskBarAdapter.J = "Blank"
            b = -1
            return
        }
        if (i2 != 1) {
            if (i2 == 2) {
                taskBarAdapter.y = 5
                taskBarAdapter.j(itemIndex, false)
                taskBarAdapter.notifyItemChanged(itemIndex)
                TaskBarAdapter.J = tag ?: "Blank"
                b = itemIndex
                val prev = f4882e
                if (prev != null && prev !== uIFragment) prev.setWindowFocused(false)
                uIFragment.setWindowFocused(true)
                f4882e = uIFragment
                return
            } else if (i2 == 3) {
                if (tag == null) return
                val instances = c(tag)
                if (instances.isNotEmpty()) {
                    var hasVisible = false
                    for (inst in instances) {
                        if (inst.isVisible) {
                            hasVisible = true
                            break
                        }
                    }
                    if (hasVisible) {
                        if (-1 == b) return
                    } else {
                        return
                    }
                }
                taskBarAdapter.y = 4
                taskBarAdapter.j(itemIndex, true)
                taskBarAdapter.notifyItemChanged(itemIndex)
                f4882e?.setWindowFocused(false)
                uIFragment.setWindowFocused(false)
                f4882e = null
                return
            } else if (i2 != 4) {
                return
            } else {
                val old = b
                taskBarAdapter.z = old
                taskBarAdapter.y = 2
                taskBarAdapter.j(old, true)
                taskBarAdapter.notifyItemChanged(itemIndex)
                taskBarAdapter.notifyItemChanged(b)
                TaskBarAdapter.J = tag ?: "Blank"
                b = itemIndex
                val prev = f4882e
                if (prev != null && prev !== uIFragment) prev.setWindowFocused(false)
                uIFragment.setWindowFocused(true)
                f4882e = uIFragment
                return
            }
        } else {
            taskBarAdapter.hashCode()
            if (tag == null || itemIndex != -1) {
                val old = b
                taskBarAdapter.z = old
                taskBarAdapter.y = 2
                taskBarAdapter.j(old, true)
                taskBarAdapter.notifyItemChanged(itemIndex)
                taskBarAdapter.notifyItemChanged(b)
                TaskBarAdapter.J = tag ?: "Blank"
                b = itemIndex
            } else {
                val taskBarItem = TaskBarItem(tag, false)
                val list = taskBarAdapter.D
                list.add(taskBarItem)
                val map = TaskBarDB.b
                map[tag] = map.size
                val newIndex = list.size - 1
                taskBarAdapter.z = b
                taskBarAdapter.y = 0
                taskBarAdapter.B = list.size - 1
                taskBarAdapter.j(taskBarAdapter.z, true)
                taskBarAdapter.notifyItemInserted(newIndex)
                taskBarAdapter.notifyItemChanged(taskBarAdapter.z)
                TaskBarAdapter.J = tag ?: "Blank"
                b = newIndex
            }
            val prev = f4882e
            if (prev != null && prev !== uIFragment) prev.setWindowFocused(false)
            uIFragment.setWindowFocused(true)
            f4882e = uIFragment
        }
    }
}
