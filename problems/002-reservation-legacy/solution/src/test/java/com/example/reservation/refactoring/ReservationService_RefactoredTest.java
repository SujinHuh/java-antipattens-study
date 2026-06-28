package com.example.reservation.refactoring;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReservationService_RefactoredTest {
    @Test
    void reserveRoom() {
        // 기존 코드: JUnit @Test가 없어 테스트로 실행되지 않았다.
        // 수정 코드: 성공 케이스를 실행 가능한 테스트로 고정했다.
        ReservationService_Refactored service = new ReservationService_Refactored(new InMemoryReservationRepository_Refactored());

        ReservationResponse_Refactored response = service.reserve(new ReservationRequest_Refactored(1L, 10L, 10, 12));

        assertEquals(ReservationStatus_Refactored.CONFIRMED, response.status());
    }

    @Test
    void rejectOverlappingReservation() {
        // 기존 코드: 중복 예약 실패 케이스가 없었다.
        // 수정 코드: 겹치는 예약이 거절되는지 검증한다.
        ReservationService_Refactored service = new ReservationService_Refactored(new InMemoryReservationRepository_Refactored());
        service.reserve(new ReservationRequest_Refactored(1L, 10L, 10, 12));

        ReservationException_Refactored exception = assertThrows(
                ReservationException_Refactored.class,
                () -> service.reserve(new ReservationRequest_Refactored(2L, 10L, 11, 13))
        );

        assertEquals(ErrorCode_Refactored.ALREADY_RESERVED, exception.getErrorCode());
    }

    @Test
    void cancelReservation() {
        // 기존 코드: 취소 성공 케이스가 없었다.
        // 수정 코드: 예약 생성 후 취소하면 상태가 CANCELLED가 되는지 검증한다.
        ReservationService_Refactored service = new ReservationService_Refactored(new InMemoryReservationRepository_Refactored());
        ReservationResponse_Refactored created = service.reserve(new ReservationRequest_Refactored(1L, 10L, 10, 12));

        ReservationResponse_Refactored cancelled = service.cancel(created.reservationId());

        assertEquals(ReservationStatus_Refactored.CANCELLED, cancelled.status());
    }
}
