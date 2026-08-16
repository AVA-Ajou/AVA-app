package com.ava.proto.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [EventEntity::class, SessionEntity::class],
    // 3: EventEntity 에 risk / stage / stageLabel 추가 (백엔드 모델 판정 경로)
    // 4: RiskSignal.LOW → CAUTION. 스키마는 그대로지만 저장된 문자열이 바뀌어, 옛 행을
    //    읽으면 valueOf 가 터진다. 버전을 올려 파괴적 마이그레이션으로 비운다.
    version = 4,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
    abstract fun sessionDao(): SessionDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "proto.db",
                )
                    // 아직 실사용자 데이터가 없는 프로토타입 단계 — 스키마가 바뀌면 새로 만든다.
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
    }
}
