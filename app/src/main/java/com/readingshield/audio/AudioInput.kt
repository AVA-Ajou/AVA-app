package com.readingshield.audio

/** 화면이 표시하는 입력 상태. 분석 결과는 Phase 2 에서 별도 상태로 추가된다. */
sealed interface AudioInput {
    /** 아직 아무 파일도 받지 못한 초기 상태. */
    data object Empty : AudioInput

    /** 파일을 읽어 메타데이터까지 확보한 상태. */
    data class Loaded(val file: AudioFile) : AudioInput

    /** URI 는 받았지만 읽지 못한 상태. */
    data class Failed(val message: String) : AudioInput
}
