@file:JvmName("PermissionsKt")

package io.github.colintimbarndt.chat_emotes.common.permissions

import com.google.common.base.Predicate
import com.mojang.brigadier.context.CommandContext
import io.github.colintimbarndt.chat_emotes.common.NAMESPACE
import net.minecraft.commands.CommandSourceStack

/**
 * Permission required to use the `/`[NAMESPACE] command (`/chat_emotes`)
 */
const val COMMAND_PERMISSION = "$NAMESPACE.command"

/**
 * Permission required to use the `reload` subcommand
 */
const val RELOAD_COMMAND_PERMISSION = "$COMMAND_PERMISSION.reload"

/**
 * Permission required to use emotes
 *
 * | Permission                       | Targets                     |
 * | -------------------------------- | --------------------------- |
 * | `chat_emotes.emotes`             | all emotes                  |
 * | `[…].<namespace>`                | all emotes in a namespace   |
 * | `[…].<namespace>.<pack>`         | all emotes in an emote pack |
 * | `[…].<namespace>.<pack>.<alias>` | a specific emote            |
 */
const val EMOTES_PERMISSION = "$NAMESPACE.emotes"

fun PermissionsAdapter<*, CommandSourceStack>.permissionPredicate(name: String): Predicate<CommandSourceStack> =
    Predicate { contextHasPermission(it, name) }
