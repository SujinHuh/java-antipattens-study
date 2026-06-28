package com.example.reservation.refactoring;

import java.time.LocalDateTime;

public class ReservationService_Refactored {
    // 기존 코드: Service가 Repository를 직접 new로 생성했다.
    // 수정 코드: Repository를 주입받아 테스트에서 대체 구현을 넣을 수 있게 했다.
    private final ReservationRepository_Refactored reservationRepository;

    public ReservationService_Refactored(ReservationRepository_Refactored reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    // 기존 코드: saveReservation() 내부 호출에 별도 @Transactional 효과를 기대할 수 있는 구조였다.
    // 수정 코드: 예약 생성 유스케이스인 reserve()를 트랜잭션 경계로 삼았다.
    @Transactional_Refactored
    public ReservationResponse_Refactored reserve(ReservationRequest_Refactored request) {
        // 기존 코드: Repository.existsOverlapping() 안에 상태 문자열 비교와 겹침 판단이 섞여 있었다.
        // 수정 코드: Repository는 조회만 하고, 예약 겹침 정책은 도메인 메서드(overlaps)로 표현했다.
        boolean alreadyReserved = reservationRepository.findByRoomId(request.roomId()).stream()
                .anyMatch(reservation -> reservation.overlaps(
                        request.roomId(),
                        request.startHour(),
                        request.endHour()
                ));

        if (alreadyReserved) {
            throw new ReservationException_Refactored(ErrorCode_Refactored.ALREADY_RESERVED, "Room is already reserved");
        }

        // 기존 코드: 요청 DTO의 status 값을 Entity에 그대로 넣었다.
        // 수정 코드: 예약 생성 시 상태는 서버가 CONFIRMED로 결정한다.
        Reservation_Refactored reservation = new Reservation_Refactored(
                System.currentTimeMillis(),
                request.userId(),
                request.roomId(),
                request.startHour(),
                request.endHour(),
                LocalDateTime.now()
        );

        Reservation_Refactored saved = reservationRepository.save(reservation);
        // 기존 코드: Service가 "reservationId=..., status=..." 문자열을 만들었다.
        // 수정 코드: 응답 DTO를 반환해 표현 형식과 비즈니스 결과를 분리했다.
        return new ReservationResponse_Refactored(saved.getId(), saved.getStatus());
    }

    // 기존 코드: cancel()이 "404 NOT_FOUND", "409 ALREADY_CANCELLED" 같은 HTTP 문자열을 반환했다.
    // 수정 코드: 실패는 예외/에러 코드로, 성공은 응답 DTO로 표현한다.
    @Transactional_Refactored
    public ReservationResponse_Refactored cancel(long reservationId) {
        Reservation_Refactored reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ReservationException_Refactored(ErrorCode_Refactored.NOT_FOUND, "Reservation not found"));

        reservation.cancel();
        Reservation_Refactored saved = reservationRepository.save(reservation);
        return new ReservationResponse_Refactored(saved.getId(), saved.getStatus());
    }
}
