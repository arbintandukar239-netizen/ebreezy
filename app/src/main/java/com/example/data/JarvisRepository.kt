package com.example.data

import kotlinx.coroutines.flow.Flow

class JarvisRepository(private val dao: JarvisDao) {

    val allMessages: Flow<List<MessageEntity>> = dao.getAllMessagesFlow()

    suspend fun getMessagesList(): List<MessageEntity> {
        return dao.getAllMessages()
    }

    suspend fun insertMessage(message: MessageEntity) {
        dao.insertMessage(message)
    }

    suspend fun clearHistory() {
        dao.clearHistory()
    }

    suspend fun getUserName(): String {
        return dao.getSetting("user_name")?.value ?: ""
    }

    suspend fun setUserName(name: String) {
        dao.insertSetting(SettingEntity("user_name", name))
    }

    suspend fun getUserTitle(): String {
        return dao.getSetting("user_title")?.value ?: "Sir"
    }

    suspend fun setUserTitle(title: String) {
        dao.insertSetting(SettingEntity("user_title", title))
    }
}
