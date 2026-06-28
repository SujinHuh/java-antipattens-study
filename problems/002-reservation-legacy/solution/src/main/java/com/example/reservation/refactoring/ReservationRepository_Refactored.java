package com.example.reservation.refactoring;

import java.util.List;
import java.util.Optional;

public interface ReservationRepository_Refactored {
    // 기존 코드: save()가 취소 상태와 시간 조건을 검사하는 비즈니스 판단까지 했다.
    // 수정 코드: Repository는 저장/조회 계약만 담당한다.
    Reservation_Refactored save(Reservation_Refactored reservation);

    // 기존 코드: 찾지 못하면 null을 반환했다.
    // 수정 코드: Optional로 "없을 수 있음"을 타입에 드러냈다.
    Optional<Reservation_Refactored> findById(long id);

    List<Reservation_Refactored> findByRoomId(long roomId);
}
