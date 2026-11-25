package com.bellatrixos.app_explorer

import android.content.Context
import android.content.DialogInterface
import android.view.View
import com.bellatrixos.R
import com.bellatrixos.app_explorer.files.FileAdapter
import com.bellatrixos.app_explorer.files.Files
import com.bellatrixos.context_menu.UIContextMenu
import com.bellatrixos.context_menu.UIMenuItem
import com.bellatrixos.windows.dialogs.UIDialog
import com.bellatrixos.windows.files.FileIntent
import com.bellatrixos.windows.files.FileMeta
import com.bellatrixos.windows.files.clipboard.Clipboards
import com.bellatrixos.windows.files.operations.New
import com.bellatrixos.windows.files.operations.Paste
import com.bellatrixos.windows.files.operations.Rename
import com.bellatrixos.windows.files.operations.a as DeleteConfirm
import com.bellatrixos.windows.installed_apps.IntentSites
import java.io.File

/**
 * The Explorer's long-press menus: on an icon (open, edit, cut, copy, paste, rename, delete, share, select all) and on
 * empty space (new folder, paste, select all). The operations were ported but the menus that reach them were stubs,
 * so Explorer could browse and nothing else.
 */
internal object ExplorerMenu {
    private const val OPEN = 0
    private const val EDIT = 1
    private const val OPEN_WITH = 2
    private const val CUT = 3
    private const val COPY = 4
    private const val PASTE = 5
    private const val RENAME = 6
    private const val DELETE = 7
    private const val SHARE = 8
    private const val SELECT_ALL = 9
    private const val NEW_FOLDER = 10

    /** What the menu on an icon acts on: the whole selection when the icon is in it, otherwise just the icon. */
    private fun targets(adapter: FileAdapter, file: File): Set<File> =
        if (file in adapter.I && adapter.I.size > 1) HashSet(adapter.I) else setOf(file)

    private fun entry(
        context: Context,
        id: Int,
        label: Int,
        icon: Int = R.drawable.menu_ic_blank_20_regular,
        bold: Boolean = false,
    ) = UIMenuItem(0, id, icon, context.getString(label)).also { it.f4750f = bold }

    fun forItem(explorer: UIExplorer, xy: IntArray, meta: FileMeta) {
        val context = explorer.v0 ?: return
        val adapter = explorer.E0 ?: return
        val anchor = explorer.u0 ?: return
        val files = targets(adapter, meta.file)
        val single = files.size == 1
        val menu = UIContextMenu(context)
        val items = menu.f4745d
        if (single) items.add(entry(context, OPEN, R.string.context_menu_open, bold = true))
        if (single && !meta.isDirectory) {
            items.add(entry(context, EDIT, R.string.context_menu_edit_in_notepad, R.drawable.menu_ic_edit_20_regular))
        }
        if (single && !meta.isDirectory) items.add(entry(context, OPEN_WITH, R.string.context_menu_open_with))
        if (items.isNotEmpty()) items.add(UIMenuItem(1, 0, 0, ""))
        items.add(entry(context, CUT, R.string.context_menu_cut))
        items.add(entry(context, COPY, R.string.context_menu_copy))
        if (Clipboards.f4922a.isNotEmpty() && explorer.G0 != null) items.add(entry(context, PASTE, R.string.context_menu_paste))
        items.add(UIMenuItem(1, 0, 0, ""))
        if (single) items.add(entry(context, RENAME, R.string.context_menu_rename, R.drawable.menu_ic_edit_20_regular))
        items.add(entry(context, DELETE, R.string.context_menu_delete, R.drawable.menu_ic_delete_20_regular))
        if (single && !meta.isDirectory) items.add(entry(context, SHARE, R.string.context_menu_share, R.drawable.menu_ic_share_20_regular))
        items.add(UIMenuItem(1, 0, 0, ""))
        items.add(entry(context, SELECT_ALL, R.string.context_menu_select_all))
        menu.d(
            UIContextMenu.OnMenuItemClickListener { _, id ->
                when (id) {
                    OPEN -> explorer.u0(meta)
                    EDIT -> explorer.openEditor(meta.file)
                    OPEN_WITH -> FileIntent.a(context, meta.file)
                    CUT -> cut(adapter, files)
                    COPY -> copy(files)
                    PASTE -> paste(explorer)
                    RENAME -> rename(explorer, meta.file)
                    DELETE -> delete(explorer, files)
                    SHARE -> IntentSites.d(context, meta.file)
                    SELECT_ALL -> selectAll(adapter)
                }
            },
        )
        menu.showAt(anchor, xy[0], xy[1])
    }

    fun forEmpty(explorer: UIExplorer, xy: IntArray) {
        val context = explorer.v0 ?: return
        val adapter = explorer.E0 ?: return
        val anchor = explorer.u0 ?: return
        val dir = explorer.G0 ?: return
        val menu = UIContextMenu(context)
        val items = menu.f4745d
        items.add(entry(context, NEW_FOLDER, R.string.files_new_folder, R.drawable.menu_ic_folder_open_20_regular))
        if (Clipboards.f4922a.isNotEmpty()) items.add(entry(context, PASTE, R.string.context_menu_paste))
        items.add(UIMenuItem(1, 0, 0, ""))
        items.add(entry(context, SELECT_ALL, R.string.context_menu_select_all))
        menu.d(
            UIContextMenu.OnMenuItemClickListener { _, id ->
                when (id) {
                    NEW_FOLDER -> newFolder(explorer, dir)
                    PASTE -> paste(explorer)
                    SELECT_ALL -> selectAll(adapter)
                }
            },
        )
        menu.showAt(anchor, xy[0], xy[1])
    }

    private fun cut(adapter: FileAdapter, files: Set<File>) {
        adapter.G = true
        adapter.J.clear()
        adapter.J.addAll(files)
        adapter.notifyItemRangeChanged(0, adapter.itemCount, FileAdapter.L)
        ContextMenuFE.b(HashSet(files))
    }

    private fun copy(files: Set<File>) = ContextMenuFE.a(HashSet(files))

    private fun paste(explorer: UIExplorer) {
        val context = explorer.v0 ?: return
        val adapter = explorer.E0 ?: return
        val dir = explorer.G0 ?: return
        Paste.a(context, dir.absolutePath, adapter)
    }

    private fun selectAll(adapter: FileAdapter) {
        adapter.I.clear()
        for (item in adapter.H) adapter.I.add(item.file)
        adapter.notifyItemRangeChanged(0, adapter.itemCount, FileAdapter.L)
    }

    /** Re-sorts nothing: shows the current list again and rebuilds the file -> row index the selection uses. */
    private fun refresh(adapter: FileAdapter) {
        adapter.submitList(adapter.D(adapter.H)) {
            adapter.B(adapter.currentList)
            adapter.D?.b(adapter.H.size)
        }
    }

    private fun newFolder(explorer: UIExplorer, dir: File) {
        val context = explorer.v0 ?: return
        val adapter = explorer.E0 ?: return
        val made = New.a(context, dir)
        if (made == null) {
            Files.a(context, context.getString(R.string.toast_file_failed_copy))
            return
        }
        adapter.H.add(FileMeta(made))
        refresh(adapter)
    }

    private fun rename(explorer: UIExplorer, file: File) {
        val context = explorer.v0 ?: return
        val adapter = explorer.E0 ?: return
        val dialog = UIDialog(context, context.getString(R.string.context_menu_rename), null)
        dialog.x = context.getString(R.string.files_file_name)
        dialog.y = file.name
        dialog.z = context.getString(R.string.dialog_actions_ok)
        dialog.A = context.getString(R.string.dialog_actions_cancel)
        dialog.setCanceledOnTouchOutside(true)
        dialog.C = DialogInterface.OnClickListener { _, _ ->
            val name = dialog.w?.text?.toString()?.trim().orEmpty()
            if (name.isNotEmpty() && name != file.name) {
                val renamed = Rename.a(context, file, name)
                if (renamed != null) {
                    adapter.H.removeAll { it.file == file }
                    adapter.H.add(FileMeta(renamed))
                    adapter.I.remove(file)
                    refresh(adapter)
                }
            }
        }
        dialog.show()
    }

    private fun delete(explorer: UIExplorer, files: Set<File>) {
        val context = explorer.v0 ?: return
        val adapter = explorer.E0 ?: return
        val first = files.first()
        val dialog = if (files.size == 1) {
            UIDialog(
                context,
                context.getString(if (first.isDirectory) R.string.context_menu_delete_folder else R.string.context_menu_delete_file),
                context.getString(
                    if (first.isDirectory) R.string.context_menu_delete_folder_permanent_subtitle
                    else R.string.context_menu_delete_file_permanent_subtitle,
                ),
            )
        } else {
            UIDialog(
                context,
                context.getString(R.string.context_menu_delete),
                context.getString(R.string.context_menu_delete_multiple, files.size),
            )
        }
        dialog.z = context.getString(R.string.context_menu_delete)
        dialog.A = context.getString(R.string.dialog_actions_cancel)
        dialog.setCanceledOnTouchOutside(true)
        // The confirm callback deletes one file (x = 1) or the whole set, then removes the rows that went.
        dialog.C = DeleteConfirm(context, if (files.size == 1) 1 else 0, first, HashSet(files), adapter)
        dialog.show()
    }
}
