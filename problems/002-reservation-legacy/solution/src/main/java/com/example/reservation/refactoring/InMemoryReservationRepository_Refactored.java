package com.example.reservation.refactoring;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InMemoryReservationRepository_Refactored implements ReservationRepository_Refactored {
    // 기존 코드: static List를 사용해 테스트 간 상태가 공유될 수 있었다.
    // 수정 코드: 인스턴스 필드로 바꿔 테스트마다 새 Repository를 만들 수 있게 했다.
    private final List<Reservation_Refactored> reservations = new ArrayList<>();

    @Override
    public Reservation_Refactored save(Reservation_Refactored reservation) {
        // 기존 코드: 같은 id 저장 시 중복 데이터가 쌓일 수 있었다.
        // 수정 코드: 같은 id는 교체해 저장소 상태가 하나의 예약을 가리키게 했다.
        reservations.removeIf(saved -> saved.getId() == reservation.getId());
        reservations.add(reservation);
        return reservation;
    }

    @Override
    public Optional<Reservation_Refactored> findById(long id) {
        return reservations.stream()
                .filter(reservation -> reservation.getId() == id)
                .findFirst();
    }

    @Override
    public List<Reservation_Refactored> findByRoomId(long roomId) {
        return reservations.stream()
                .filter(reservation -> reservation.getRoomId() == roomId)
                .toList();
    }
}
