package com.bellatrixos.linux

import android.system.Os
import android.system.OsConstants
import java.io.File

/**
 * Ends a proot started with [Proot.process] and every guest process under it.
 * Ported verbatim from Bellatrix: SIGKILLing proot alone leaves its tracees
 * running, so each process in the tree is killed, children before their proot.
 */
fun killTree(process: Process) {
    val root = pidOf(process)
    if (root != null && process.isAlive) {
        repeat(3) {
            val tree = descendants(root)
            if (tree.isEmpty()) return@repeat
            for (pid in tree) runCatching { Os.kill(pid, OsConstants.SIGKILL) }
        }
        runCatching { Os.kill(root, OsConstants.SIGKILL) }
    }
    process.destroy()
}

private fun pidOf(process: Process): Int? = Regex("""pid=(\d+)""").find(process.toString())?.groupValues?.get(1)?.toIntOrNull()

internal fun statFields(proc: File): List<String>? =
    runCatching { File(proc, "stat").readText() }.getOrNull()?.substringAfterLast(')')?.trim()?.split(' ')

private fun descendants(root: Int): List<Int> {
    val children = HashMap<Int, MutableList<Int>>()
    for (dir in File("/proc").listFiles().orEmpty()) {
        val pid = dir.name.toIntOrNull() ?: continue
        val ppid = statFields(dir)?.getOrNull(1)?.toIntOrNull() ?: continue
        children.getOrPut(ppid) { ArrayList() }.add(pid)
    }
    val out = ArrayList<Int>()
    val queue = ArrayDeque(listOf(root))
    while (queue.isNotEmpty()) {
        for (c in children[queue.removeFirst()].orEmpty()) {
            out.add(c)
            queue.add(c)
        }
    }
    return out
}
