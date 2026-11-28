package com.bellatrixos.system

import com.bellatrixos.R
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.os.Build
import com.bellatrixos.MainActivity
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

/**
 * A notification that opens the home screen, on [channel] (named [channelName] in Android's settings). False when it
 * can't be posted: notifications off for the launcher, or the channel. Needs the notification permission.
 */
fun Context.postNotification(
    channel: String,
    channelName: String,
    title: String,
    text: String?,
): Boolean {
    val nm = notificationManager(channel, channelName) ?: return false
    val n =
        notificationBuilder(channel, title, text)
            .setAutoCancel(true)
            .build()
    nm.notify(channel, NotificationIds.next(), n)
    return true
}

/** What came of [postReplyNotification]. */
sealed interface NotifyReply {
    /** What was typed into the Reply button. */
    class Typed(
        val text: String,
    ) : NotifyReply

    /** Dismissed, or not answered in time. */
    object Unanswered : NotifyReply

    /** Not posted: notifications off for the launcher, or the channel. */
    object Off : NotifyReply
}

/**
 * [postNotification] with a Reply button ([replyLabel]), as a messaging app's notification has: what the user types
 * into it is what this returns, once it is typed. Without a reply within [ms], or if the notification is swiped away,
 * [NotifyReply.Unanswered]. The notification is gone when this returns. Tapping its body opens the home screen and
 * leaves it, so a reply can still come.
 */
suspend fun Context.postReplyNotification(
    channel: String,
    channelName: String,
    title: String,
    text: String?,
    replyLabel: String,
    ms: Long,
): NotifyReply {
    val nm = notificationManager(channel, channelName) ?: return NotifyReply.Off
    val id = NotificationIds.next()
    // Its own actions, so a reply to this one is not taken for another's, and nothing else answers it.
    val replyAction = "$packageName.NOTIFY_REPLY.$id"
    val dismissAction = "$packageName.NOTIFY_DISMISS.$id"
    val answer = CompletableDeferred<NotifyReply>()
    val filter =
        IntentFilter().apply {
            addAction(replyAction)
            addAction(dismissAction)
        }
    val receiver =
        registerNotExported(filter) { intent ->
            if (intent.action == dismissAction) {
                answer.complete(NotifyReply.Unanswered)
            } else {
                // An empty one is no answer: the button stays until a real one or the time is up.
                RemoteInput.getResultsFromIntent(intent)?.getCharSequence(REPLY_KEY)?.toString()?.takeIf { it.isNotBlank() }?.let { answer.complete(NotifyReply.Typed(it)) }
            }
        }
    try {
        // Mutable: Android fills the typed text into the intent it sends.
        val mutable = if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0
        val reply = PendingIntent.getBroadcast(this, id, Intent(replyAction).setPackage(packageName), PendingIntent.FLAG_UPDATE_CURRENT or mutable)
        val dismissed = PendingIntent.getBroadcast(this, id, Intent(dismissAction).setPackage(packageName), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val input = RemoteInput.Builder(REPLY_KEY).setLabel(replyLabel).build()
        val button = Notification.Action.Builder(Icon.createWithResource(this, R.drawable.ic_notification), replyLabel, reply).addRemoteInput(input).build()
        val n =
            notificationBuilder(channel, title, text)
                .addAction(button)
                .setDeleteIntent(dismissed)
                .build()
        nm.notify(channel, id, n)
        return withTimeoutOrNull(ms) { answer.await() } ?: NotifyReply.Unanswered
    } finally {
        runCatching { unregisterReceiver(receiver) }
        nm.cancel(channel, id)
    }
}

/** The notification manager once [channel] exists and may post; null when notifications, or the channel, are off. */
private fun Context.notificationManager(
    channel: String,
    channelName: String,
): NotificationManager? {
    val nm = getSystemService(NotificationManager::class.java)
    if (!nm.areNotificationsEnabled()) return null
    nm.createNotificationChannel(NotificationChannel(channel, channelName, NotificationManager.IMPORTANCE_DEFAULT))
    if (nm.getNotificationChannel(channel)?.importance == NotificationManager.IMPORTANCE_NONE) return null
    return nm
}

/** A notification on [channel] that opens the home screen when tapped. */
private fun Context.notificationBuilder(
    channel: String,
    title: String,
    text: String?,
): Notification.Builder {
    val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
    return Notification
        .Builder(this, channel)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(title)
        .setContentText(text)
        // All of a longer text as it's pulled open, not its first line.
        .setStyle(text?.let { Notification.BigTextStyle().bigText(it) })
        .setContentIntent(open)
}

/** The key the typed text comes back under. */
private const val REPLY_KEY = "reply"

/** Its own id for each notification, so none replaces one still showing, from before a restart either. */
private object NotificationIds {
    private var last = 0

    @Synchronized
    fun next(): Int {
        last = maxOf(last + 1, (System.currentTimeMillis() / 1000 % Int.MAX_VALUE).toInt())
        return last
    }
}
