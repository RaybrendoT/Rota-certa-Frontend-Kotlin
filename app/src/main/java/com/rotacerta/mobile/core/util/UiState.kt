package com.rotacerta.mobile.core.util

/** Estado genérico de uma tela que carrega dados da API. */
sealed interface UiState<out T> {
    data object Idle : UiState<Nothing>
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

/** Estado simples de uma ação disparada pelo usuário (ex: enviar formulário). */
sealed interface ActionState {
    data object Idle : ActionState
    data object InProgress : ActionState
    data object Success : ActionState
    data class Error(val message: String) : ActionState
}
