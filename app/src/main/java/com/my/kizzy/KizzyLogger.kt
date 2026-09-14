package com.my.kizzy

/** Small logging abstraction used by the bundled Kizzy RPC compatibility layer. */
interface KizzyLogger {
    fun info(message: String)
    fun fine(message: String)
    fun warning(message: String)
    fun severe(message: String)
}

class DefaultKizzyLogger(private val tag: String = "Kizzy") : KizzyLogger {
    override fun info(message: String) = println("I/$tag: $message")
    override fun fine(message: String) = println("D/$tag: $message")
    override fun warning(message: String) = println("W/$tag: $message")
    override fun severe(message: String) = println("E/$tag: $message")
}
