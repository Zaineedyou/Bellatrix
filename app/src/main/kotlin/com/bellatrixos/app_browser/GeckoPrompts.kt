package com.bellatrixos.app_browser

import android.content.Context
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSession.PromptDelegate
import org.mozilla.geckoview.GeckoSession.PromptDelegate.PromptResponse

/**
 * What Gecko leaves to the app: it draws no alert, confirm, text box or drop-down list of its own, and a prompt nobody
 * answers is a page that never goes on (a `<select>` that won't open). Plain dialogs for each, answered on the main thread.
 */
internal object GeckoPrompts {
    /**
     * Sets the prompt handling of [session]. [context] is the tab's, null once it is gone (the prompt is then dismissed);
     * [chooseFiles] picks files for an `<input type=file>` and completes with the answer.
     */
    fun install(
        session: GeckoSession,
        context: () -> Context?,
        chooseFiles: (PromptDelegate.FilePrompt) -> GeckoResult<PromptResponse>,
    ) {
        session.promptDelegate =
            object : PromptDelegate {
                override fun onAlertPrompt(
                    session: GeckoSession,
                    prompt: PromptDelegate.AlertPrompt,
                ): GeckoResult<PromptResponse>? {
                    val ctx = context() ?: return GeckoResult.fromValue(prompt.dismiss())
                    val result = GeckoResult<PromptResponse>()
                    AlertDialog
                        .Builder(ctx)
                        .setTitle(prompt.title)
                        .setMessage(prompt.message)
                        .setPositiveButton(android.R.string.ok) { _, _ -> result.complete(prompt.dismiss()) }
                        .setOnCancelListener { result.complete(prompt.dismiss()) }
                        .show()
                    return result
                }

                override fun onButtonPrompt(
                    session: GeckoSession,
                    prompt: PromptDelegate.ButtonPrompt,
                ): GeckoResult<PromptResponse>? {
                    val ctx = context() ?: return GeckoResult.fromValue(prompt.dismiss())
                    val result = GeckoResult<PromptResponse>()
                    AlertDialog
                        .Builder(ctx)
                        .setTitle(prompt.title)
                        .setMessage(prompt.message)
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            result.complete(prompt.confirm(PromptDelegate.ButtonPrompt.Type.POSITIVE))
                        }.setNegativeButton(android.R.string.cancel) { _, _ ->
                            result.complete(prompt.confirm(PromptDelegate.ButtonPrompt.Type.NEGATIVE))
                        }.setOnCancelListener { result.complete(prompt.dismiss()) }
                        .show()
                    return result
                }

                override fun onTextPrompt(
                    session: GeckoSession,
                    prompt: PromptDelegate.TextPrompt,
                ): GeckoResult<PromptResponse>? {
                    val ctx = context() ?: return GeckoResult.fromValue(prompt.dismiss())
                    val result = GeckoResult<PromptResponse>()
                    val input = EditText(ctx).apply { setText(prompt.defaultValue.orEmpty()) }
                    AlertDialog
                        .Builder(ctx)
                        .setTitle(prompt.title)
                        .setMessage(prompt.message)
                        .setView(input)
                        .setPositiveButton(android.R.string.ok) { _, _ -> result.complete(prompt.confirm(input.text.toString())) }
                        .setNegativeButton(android.R.string.cancel) { _, _ -> result.complete(prompt.dismiss()) }
                        .setOnCancelListener { result.complete(prompt.dismiss()) }
                        .show()
                    return result
                }

                override fun onChoicePrompt(
                    session: GeckoSession,
                    prompt: PromptDelegate.ChoicePrompt,
                ): GeckoResult<PromptResponse>? {
                    val ctx = context() ?: return GeckoResult.fromValue(prompt.dismiss())
                    val result = GeckoResult<PromptResponse>()
                    val choices = prompt.choices.filter { !it.separator }
                    val labels = choices.map { it.label.orEmpty() }.toTypedArray()
                    val builder = AlertDialog.Builder(ctx).setTitle(prompt.title)
                    if (prompt.type == PromptDelegate.ChoicePrompt.Type.MULTIPLE) {
                        val checked = BooleanArray(choices.size) { choices[it].selected }
                        builder
                            .setMultiChoiceItems(labels, checked) { _, which, on -> checked[which] = on }
                            .setPositiveButton(android.R.string.ok) { _, _ ->
                                val ids = choices.filterIndexed { i, _ -> checked[i] }.map { it.id }.toTypedArray()
                                result.complete(prompt.confirm(ids))
                            }.setNegativeButton(android.R.string.cancel) { _, _ -> result.complete(prompt.dismiss()) }
                    } else {
                        builder.setItems(labels) { _, which -> result.complete(prompt.confirm(choices[which].id)) }
                    }
                    builder.setOnCancelListener { result.complete(prompt.dismiss()) }.show()
                    return result
                }

                override fun onFilePrompt(
                    session: GeckoSession,
                    prompt: PromptDelegate.FilePrompt,
                ): GeckoResult<PromptResponse>? = chooseFiles(prompt)

                // A site may leave, reload or repost: the dialogs a WebView never showed either.
                override fun onBeforeUnloadPrompt(
                    session: GeckoSession,
                    prompt: PromptDelegate.BeforeUnloadPrompt,
                ): GeckoResult<PromptResponse>? = GeckoResult.fromValue(prompt.confirm(AllowOrDeny.ALLOW))

                override fun onRepostConfirmPrompt(
                    session: GeckoSession,
                    prompt: PromptDelegate.RepostConfirmPrompt,
                ): GeckoResult<PromptResponse>? = GeckoResult.fromValue(prompt.confirm(AllowOrDeny.ALLOW))

                // Popups the page opens without a tap stay shut, as the WebView's default had it.
                override fun onPopupPrompt(
                    session: GeckoSession,
                    prompt: PromptDelegate.PopupPrompt,
                ): GeckoResult<PromptResponse>? = GeckoResult.fromValue(prompt.confirm(AllowOrDeny.DENY))
            }
    }
}
