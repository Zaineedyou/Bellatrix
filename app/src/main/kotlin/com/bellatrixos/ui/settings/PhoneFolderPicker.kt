package com.bellatrixos.ui.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import com.bellatrixos.R
import com.bellatrixos.storage.PhoneFolders
import com.bellatrixos.storage.showingStorage

/**
 * Android's folder picker for the file manager; the folder picked is kept as one of the [PhoneFolders].
 * The rest of the old phone-folder row moved into Bellatrix's native Permissions page.
 */
@Composable
internal fun rememberAddPhoneFolder(folders: PhoneFolders): () -> Unit {
    val context = LocalContext.current
    val resources = LocalResources.current
    val pick =
        rememberLauncherForActivityResult(
            object : ActivityResultContracts.OpenDocumentTree() {
                override fun createIntent(
                    context: Context,
                    input: Uri?,
                ) = super.createIntent(context, input).showingStorage()
            },
        ) { tree ->
            if (tree != null) {
                runCatching { folders.add(tree) }.onFailure {
                    Toast.makeText(context, resources.getString(R.string.permissions_folders_cant_keep), Toast.LENGTH_SHORT).show()
                }
            }
        }
    return {
        try {
            pick.launch(null)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, resources.getString(R.string.permissions_folders_no_picker), Toast.LENGTH_SHORT).show()
        }
    }
}
