package com.rotacerta.mobile.core.util

enum class PasswordStrength(val label: String) {
    WEAK("Fraca"),
    MEDIUM("Média"),
    GOOD("Boa"),
    STRONG("Forte"),
}

/** Regras de senha mostradas ao usuário (o backend ainda aplica os validadores do Django). */
object PasswordRules {
    private val upper = Regex("[A-Z]")
    private val lower = Regex("[a-z]")
    private val digit = Regex("\\d")
    private val special = Regex("[^A-Za-z0-9]")

    const val REQUIREMENTS =
        "A senha deve ter ao menos 8 caracteres, com letra maiúscula, minúscula, número e caractere especial."

    /** Quantos dos 5 critérios (tamanho, maiúscula, minúscula, número, especial) a senha cumpre. */
    fun score(password: String): Int = listOf(
        password.length >= 8,
        upper.containsMatchIn(password),
        lower.containsMatchIn(password),
        digit.containsMatchIn(password),
        special.containsMatchIn(password),
    ).count { it }

    fun isValid(password: String): Boolean = score(password) == 5

    fun strength(password: String): PasswordStrength = when (score(password)) {
        5 -> PasswordStrength.STRONG
        4 -> PasswordStrength.GOOD
        3 -> PasswordStrength.MEDIUM
        else -> PasswordStrength.WEAK
    }
}

object Validators {
    private val email = Regex("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)*\\.[A-Za-z]{2,}$")

    fun isValidEmail(value: String) = email.matches(value.trim())

    /** Aceita 8 a 15 dígitos, com ou sem +, espaços, traços e parênteses. */
    fun isValidPhone(value: String): Boolean =
        Regex("^\\+?\\d{8,15}$").matches(value.replace(Regex("[\\s\\-()]"), ""))
}
