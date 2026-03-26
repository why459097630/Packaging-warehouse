package com.ndjc.feature.showcase

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val storeId: String,
    val role: String,
    val direction: String,   // "in" | "out"
    val text: String,
    val timeMs: Long,
    val status: String,      // "sending" | "sent" | "failed"
    val isRead: Boolean,
    val conversationId: String,
    val clientId: String
)

/**
 * ✅ 新增：会话 meta（置顶/删除）——不碰 UI，不碰云端，先保证本地闭环可用
 */
@Entity(
    tableName = "chat_thread_meta",
    primaryKeys = ["storeId", "conversationId"]
)
data class ChatThreadMetaEntity(
    val storeId: String,
    val conversationId: String,
    val pinnedAtMs: Long,     // 0 = 未置顶
    val isDeleted: Boolean,   // true = 当前在列表隐藏
    val deletedAtMs: Long = 0L, // 记录删除时间；只有“删除后新消息”才能让线程重新出现

    // ✅ 新增：商家给客户设置的别名/备注（优先显示）
    val alias: String? = null
)


@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY timeMs ASC")
    suspend fun listByConversation(conversationId: String): List<ChatMessageEntity>
    @Query("""
SELECT * FROM chat_messages
WHERE conversationId = :conversationId
  AND text LIKE '%' || :keyword || '%'
ORDER BY timeMs DESC
LIMIT :limit
""")
    suspend fun searchByConversationKeyword(
        conversationId: String,
        keyword: String,
        limit: Int = 80
    ): List<ChatMessageEntity>


    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY timeMs ASC")
    fun observeByConversation(conversationId: String): Flow<List<ChatMessageEntity>>

    @Query("""
SELECT COUNT(*) FROM chat_messages
WHERE conversationId = :conversationId AND direction = 'in' AND isRead = 0
""")
    fun observeUnread(conversationId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ChatMessageEntity)

    @Query("SELECT COUNT(*) FROM chat_messages")
    suspend fun countAll(): Int

    @Query("SELECT * FROM chat_messages ORDER BY timeMs DESC LIMIT 1")
    suspend fun latest(): ChatMessageEntity?

    @Query("SELECT * FROM chat_messages WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): ChatMessageEntity?

    @Query("UPDATE chat_messages SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("UPDATE chat_messages SET status = :status WHERE id IN (:ids)")
    suspend fun updateStatusByIds(ids: List<String>, status: String)

    @Query("DELETE FROM chat_messages WHERE storeId = :storeId")
    suspend fun clearStore(storeId: String)

    @Query("""
UPDATE chat_messages
SET isRead = 1
WHERE conversationId = :conversationId AND direction = 'in'
""")
    suspend fun markAllRead(conversationId: String)

    @Query("""
UPDATE chat_messages
SET isRead = 1
WHERE conversationId = :conversationId AND direction = 'out'
""")
    suspend fun markAllOutgoingRead(conversationId: String)

    @Query("""
SELECT COUNT(*) FROM chat_messages
WHERE conversationId = :conversationId AND direction = 'in' AND isRead = 0
""")
    suspend fun countUnread(conversationId: String): Int

    @Query("""
SELECT COUNT(*) FROM chat_messages
WHERE conversationId = :conversationId
AND role = 'merchant'
AND isRead = 0
""")
    suspend fun countUnreadForUserEntry(conversationId: String): Int

    @Query("""
SELECT conversationId
FROM chat_messages
WHERE storeId = :storeId AND clientId = :clientId
ORDER BY timeMs DESC
LIMIT 1
""")
    suspend fun findLatestConversationIdByStoreAndClient(
        storeId: String,
        clientId: String
    ): String?

    @Query("""
SELECT COUNT(*) FROM chat_messages
WHERE storeId = :storeId
AND clientId = :clientId
AND role = 'merchant'
AND isRead = 0
""")
    suspend fun countUnreadForUserEntryByStoreAndClient(
        storeId: String,
        clientId: String
    ): Int

    @Query("SELECT * FROM chat_messages WHERE storeId = :storeId ORDER BY timeMs ASC")
    suspend fun listByStore(storeId: String): List<ChatMessageEntity>

    @Query("SELECT * FROM chat_messages WHERE storeId = :storeId ORDER BY timeMs ASC")
    fun observeByStore(storeId: String): Flow<List<ChatMessageEntity>>

    @Query("DELETE FROM chat_messages WHERE storeId = :storeId AND id IN (:ids)")
    suspend fun deleteByIds(storeId: String, ids: List<String>)

    @Query("DELETE FROM chat_messages WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM chat_messages WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    // ✅ 新增：删除整条会话的所有消息（删除聊天）
    @Query("DELETE FROM chat_messages WHERE conversationId = :conversationId")
    suspend fun deleteByConversation(conversationId: String)
    // ✅ 全局搜索：按 storeId 搜消息内容（倒序取最近一些）
    @Query("""
SELECT * FROM chat_messages
WHERE storeId = :storeId
  AND text LIKE '%' || :keyword || '%'
ORDER BY timeMs DESC
LIMIT :limit
""")
    suspend fun searchByStoreKeyword(
        storeId: String,
        keyword: String,
        limit: Int = 80
    ): List<ChatMessageEntity>

}

@Dao
interface ChatThreadMetaDao {
    @Query("SELECT * FROM chat_thread_meta WHERE storeId = :storeId")
    suspend fun listByStore(storeId: String): List<ChatThreadMetaEntity>

    @Query("SELECT * FROM chat_thread_meta WHERE storeId = :storeId")
    fun observeByStore(storeId: String): Flow<List<ChatThreadMetaEntity>>

    @Query("""
SELECT * FROM chat_thread_meta
WHERE storeId = :storeId AND conversationId = :conversationId
LIMIT 1
""")
    suspend fun get(storeId: String, conversationId: String): ChatThreadMetaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ChatThreadMetaEntity)

    @Query("""
UPDATE chat_thread_meta
SET pinnedAtMs = :pinnedAtMs
WHERE storeId = :storeId AND conversationId = :conversationId
""")
    suspend fun updatePinned(storeId: String, conversationId: String, pinnedAtMs: Long)

    @Query("""
UPDATE chat_thread_meta
SET isDeleted = :isDeleted,
    deletedAtMs = :deletedAtMs
WHERE storeId = :storeId AND conversationId = :conversationId
""")
    suspend fun updateDeleted(
        storeId: String,
        conversationId: String,
        isDeleted: Boolean,
        deletedAtMs: Long
    )

    @Query("""
UPDATE chat_thread_meta
SET alias = :alias
WHERE storeId = :storeId AND conversationId = :conversationId
""")
    suspend fun updateAlias(storeId: String, conversationId: String, alias: String?)

    @Query("""
DELETE FROM chat_thread_meta
WHERE storeId = :storeId AND conversationId = :conversationId
""")
    suspend fun deleteByConversation(storeId: String, conversationId: String)
}


@Database(
    entities = [ChatMessageEntity::class, ChatThreadMetaEntity::class],
    version = 6,
    exportSchema = false
)

abstract class ShowcaseChatDb : RoomDatabase() {
    abstract fun chatDao(): ChatMessageDao
    abstract fun metaDao(): ChatThreadMetaDao

    companion object {
        @Volatile private var INSTANCE: ShowcaseChatDb? = null

        fun get(context: Context): ShowcaseChatDb =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?:
                Room.databaseBuilder(
                    context,
                    ShowcaseChatDb::class.java,
                    "showcase_chat_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
