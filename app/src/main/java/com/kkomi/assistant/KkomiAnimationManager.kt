package com.kkomi.assistant

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import android.widget.ImageView

/**
 * 꼬미 캐릭터 애니메이션 관리 클래스
 * 4가지 상태의 애니메이션을 제어합니다
 */
class KkomiAnimationManager(private val kkomiImageView: ImageView) {

    enum class AnimationState {
        IDLE,           // 기본 상태
        LISTENING,      // 명령어 인식 중
        PROCESSING,     // 처리 중
        EXECUTING,      // 명령 실행
        SLEEPING        // 대기 모드
    }

    private var currentState = AnimationState.IDLE
    private var animatorSet: AnimatorSet? = null

    /**
     * 애니메이션 상태 변경
     */
    fun changeState(newState: AnimationState) {
        if (currentState == newState) return

        // 기존 애니메이션 중지
        stopAnimation()

        currentState = newState

        // 새로운 애니메이션 시작
        when (newState) {
            AnimationState.IDLE -> playIdleAnimation()
            AnimationState.LISTENING -> playListeningAnimation()
            AnimationState.PROCESSING -> playProcessingAnimation()
            AnimationState.EXECUTING -> playExecutingAnimation()
            AnimationState.SLEEPING -> playSleepingAnimation()
        }
    }

    /**
     * 기본 상태 애니메이션 (부드러운 스케일)
     */
    private fun playIdleAnimation() {
        animatorSet = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(kkomiImageView, "scaleX", 1f, 1.05f, 1f).apply {
                    duration = 2000
                    repeatCount = -1
                },
                ObjectAnimator.ofFloat(kkomiImageView, "scaleY", 1f, 1.05f, 1f).apply {
                    duration = 2000
                    repeatCount = -1
                }
            )
            start()
        }
    }

    /**
     * 명령어 인식 중 애니메이션 (위아래 움직임)
     */
    private fun playListeningAnimation() {
        animatorSet = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(kkomiImageView, "translationY", 0f, -10f, 0f).apply {
                    duration = 600
                    repeatCount = -1
                },
                ObjectAnimator.ofFloat(kkomiImageView, "rotation", 0f, 5f, -5f, 0f).apply {
                    duration = 800
                    repeatCount = -1
                }
            )
            start()
        }
    }

    /**
     * 처리 중 애니메이션 (회전)
     */
    private fun playProcessingAnimation() {
        animatorSet = AnimatorSet().apply {
            play(
                ObjectAnimator.ofFloat(kkomiImageView, "rotation", 0f, 360f).apply {
                    duration = 2000
                    repeatCount = -1
                }
            )
            start()
        }
    }

    /**
     * 명령 실행 애니메이션 (춤/점프)
     */
    private fun playExecutingAnimation() {
        animatorSet = AnimatorSet().apply {
            playSequentially(
                // 점프 1
                AnimatorSet().apply {
                    playTogether(
                        ObjectAnimator.ofFloat(kkomiImageView, "translationY", 0f, -30f).apply {
                            duration = 300
                        },
                        ObjectAnimator.ofFloat(kkomiImageView, "scaleX", 1f, 0.95f).apply {
                            duration = 300
                        },
                        ObjectAnimator.ofFloat(kkomiImageView, "scaleY", 1f, 0.95f).apply {
                            duration = 300
                        }
                    )
                },
                AnimatorSet().apply {
                    playTogether(
                        ObjectAnimator.ofFloat(kkomiImageView, "translationY", -30f, 0f).apply {
                            duration = 300
                        },
                        ObjectAnimator.ofFloat(kkomiImageView, "scaleX", 0.95f, 1f).apply {
                            duration = 300
                        },
                        ObjectAnimator.ofFloat(kkomiImageView, "scaleY", 0.95f, 1f).apply {
                            duration = 300
                        }
                    )
                },
                // 춤 (좌우 흔들기)
                AnimatorSet().apply {
                    playTogether(
                        ObjectAnimator.ofFloat(kkomiImageView, "rotation", 0f, 10f, -10f, 0f).apply {
                            duration = 800
                        }
                    )
                }
            )
            start()
        }
    }

    /**
     * 대기 모드 애니메이션 (자고 있음)
     */
    private fun playSleepingAnimation() {
        animatorSet = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(kkomiImageView, "alpha", 1f, 0.7f, 1f).apply {
                    duration = 1500
                    repeatCount = -1
                },
                ObjectAnimator.ofFloat(kkomiImageView, "translationY", 0f, 5f, 0f).apply {
                    duration = 1500
                    repeatCount = -1
                }
            )
            start()
        }
    }

    /**
     * 애니메이션 중지
     */
    private fun stopAnimation() {
        animatorSet?.cancel()
        animatorSet = null
        
        // 상태 초기화
        kkomiImageView.apply {
            translationY = 0f
            rotation = 0f
            scaleX = 1f
            scaleY = 1f
            alpha = 1f
        }
    }

    /**
     * 리소스 정리
     */
    fun release() {
        stopAnimation()
    }
}
