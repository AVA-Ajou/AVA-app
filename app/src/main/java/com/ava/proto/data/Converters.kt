package com.ava.proto.data

import androidx.room.TypeConverter
import com.ava.proto.capture.Channel

/**
 * SessionEntity.channelsInvolved 는 코드에서는 Set<Channel> 로 다루고, 저장 형식만 여기서
 * 문자열로 감춘다 — 호출부(SessionEngine)가 split/join을 직접 하지 않게 하려고 만들었다.
 */
class Converters {
    @TypeConverter
    fun fromChannelSet(value: Set<Channel>): String = value.joinToString(",") { it.name }

    @TypeConverter
    fun toChannelSet(value: String): Set<Channel> =
        value.split(",").filter { it.isNotBlank() }.map { Channel.valueOf(it) }.toSet()
}
