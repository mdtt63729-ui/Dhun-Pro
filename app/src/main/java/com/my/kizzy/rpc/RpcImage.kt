package com.my.kizzy.rpc

sealed class RpcImage {
    data class DiscordImage(val image: String) : RpcImage()
    data class ExternalImage(val image: String) : RpcImage()

    suspend fun resolveImage(repository: com.my.kizzy.repository.KizzyRepository): String? = when (this) {
        is DiscordImage -> image
        is ExternalImage -> repository.getImage(image) ?: image
    }
}
