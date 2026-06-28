package com.example.reservation.refactoring;

import java.time.LocalDateTime;

public class Reservation_Refactored {
    // 기존 코드: 모든 필드가 public이라 외부에서 자유롭게 변경할 수 있었다.
    // 수정 코드: 필드를 private으로 감추고 필요한 동작만 메서드로 열었다.
    private final long id;
    private final long userId;
    private final long roomId;
    private final int startHour;
    private final int endHour;
    // 기존 코드: status가 String이라 오타와 허용되지 않은 값에 취약했다.
    // 수정 코드: enum으로 허용 가능한 상태를 제한했다.
    private ReservationStatus_Refactored status;
    private final LocalDateTime createdAt;

    public Reservation_Refactored(long id, long userId, long roomId, int startHour, int endHour, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.roomId = roomId;
        this.startHour = startHour;
        this.endHour = endHour;
        this.status = ReservationStatus_Refactored.CONFIRMED;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public long getRoomId() {
        return roomId;
    }

    public int getStartHour() {
        return startHour;
    }

    public int getEndHour() {
        return endHour;
    }

    public ReservationStatus_Refactored getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean overlaps(long roomId, int startHour, int endHour) {
        // 기존 코드: 예약 겹침 조건이 Repository의 if문 안에 있었다.
        // 수정 코드: 예약 자신이 겹침 여부를 판단하게 해 정책 의미를 드러냈다.
        return this.roomId == roomId
                && status != ReservationStatus_Refactored.CANCELLED
                && this.startHour < endHour
                && startHour < this.endHour;
    }

    public void cancel() {
        // 기존 코드: Service가 문자열 비교로 이미 취소된 예약을 판단했다.
        // 수정 코드: Entity의 상태 변경 메서드 안에서 취소 규칙을 지킨다.
        if (status == ReservationStatus_Refactored.CANCELLED) {
            throw new ReservationException_Refactored(ErrorCode_Refactored.ALREADY_CANCELLED, "Reservation is already cancelled");
        }
        status = ReservationStatus_Refactored.CANCELLED;
    }
}
