package com.bellatrixos.wayland

/** zwp_text_input_manager_v3: hands out text inputs, through which clients say where their text cursor is. */
class TextInputManager(
    client: Client,
    id: Int,
    version: Int,
) : Resource(client, id, version) {
    override val iface = "zwp_text_input_manager_v3"

    override fun request(
        opcode: Int,
        args: Args,
    ) {
        when (opcode) {
            0 -> destroy()
            1 -> client.server.input.addTextInput(client.register(TextInput(client, args.newId(), version)))
        }
    }
}

/**
 * One client's zwp_text_input_v3. Only whether it is on and its cursor rectangle are used, in surface-local logical
 * pixels: foot reports the terminal's cursor cell there, Chromium and Electron often report an empty one. No text is
 * ever sent back; keys still reach the client through wl_keyboard. Every commit is answered with `done`, which the
 * protocol requires and Chromium waits for before it treats the input as set up.
 */
class TextInput(
    client: Client,
    id: Int,
    version: Int,
) : Resource(client, id, version) {
    override val iface = "zwp_text_input_v3"

    private var pendingEnabled: Boolean? = null
    private var pendingCursor: IntArray? = null

    /** Whether the client has text input on, as of its last commit. */
    private var enabled = false

    /** Commit requests received so far: the serial `done` carries. */
    private var commits = 0

    val isEnabled: Boolean get() = enabled

    /** x, y, width, height of the text cursor, as of the last commit while enabled; null when unknown. */
    var cursor: IntArray? = null
        private set

    override fun request(
        opcode: Int,
        args: Args,
    ) {
        when (opcode) {
            0 -> destroy()
            // A rectangle set before enable in the same batch is kept: Chromium orders them either way round.
            1 -> pendingEnabled = true
            2 -> pendingEnabled = false
            6 -> pendingCursor = intArrayOf(args.int(), args.int(), args.int(), args.int())
            7 -> {
                commits++
                pendingEnabled?.let { enabled = it }
                // The last rectangle stays when a commit doesn't set one, even an empty 0,0,0,0 one.
                cursor = if (enabled) pendingCursor ?: cursor else null
                pendingEnabled = null
                pendingCursor = null
                // done(serial), event 5: the client has had its state applied.
                send(event(5).uint(commits))
            }
            // set_surrounding_text, set_text_change_cause, set_content_type: nothing to do with them.
        }
    }

    fun enter(surface: Surface) = send(event(0).obj(surface.id))

    fun leave(surface: Surface) {
        send(event(1).obj(surface.id))
        cursor = null
    }
}
