package com.aichat.app.domain.model

/**
 * 对话角色。
 */
enum class Role {
    /** 用户。 */
    USER,

    /** 助手（模型）。 */
    ASSISTANT,

    /** 系统提示。 */
    SYSTEM
}
