package io.github.colintimbarndt.chat_emotes.common.permissions

object NoPermissionsAdapter : PermissionsAdapter<Any?, Any?> {
    override fun contextHasPermission(ctx: Any?, name: String) = false

    override fun playerHasPermission(p: Any?, name: String) = false
}