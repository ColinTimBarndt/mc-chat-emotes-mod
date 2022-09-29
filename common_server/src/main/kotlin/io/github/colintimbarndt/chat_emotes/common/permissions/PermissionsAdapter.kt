package io.github.colintimbarndt.chat_emotes.common.permissions

interface PermissionsAdapter<in P, in S> {
    fun contextHasPermission(ctx: S, name: String): Boolean
    fun playerHasPermission(p: P, name: String): Boolean
}