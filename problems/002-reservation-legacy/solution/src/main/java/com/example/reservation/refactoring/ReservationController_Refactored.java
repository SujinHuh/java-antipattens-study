package com.example.reservation.refactoring;

public class ReservationController_Refactored {
    // 기존 코드: Controller가 Service를 직접 new로 생성했다.
    // 수정 코드: 생성자 주입 형태로 바꿔 의존성을 외부에서 주입받게 했다.
    private final ReservationService_Refactored reservationService;

    public ReservationController_Refactored(ReservationService_Refactored reservationService) {
        this.reservationService = reservationService;
    }

    // 기존 코드: String 파라미터를 직접 parseLong/parseInt로 변환했다.
    // 수정 코드: 요청 객체를 받아 Controller의 수동 파싱 책임을 줄였다.
    public ReservationResponse_Refactored createReservation(ReservationRequest_Refactored request) {
        return reservationService.reserve(request);
    }

    // 기존 코드: Service가 "200 OK", "404 NOT_FOUND" 같은 문자열을 반환했다.
    // 수정 코드: Service 결과 객체를 그대로 반환해 HTTP 표현과 비즈니스 결과를 분리했다.
    public ReservationResponse_Refactored cancelReservation(long reservationId) {
        return reservationService.cancel(reservationId);
    }
}
