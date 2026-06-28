package com.example.reservation.refactoring;

public record ReservationResponse_Refactored(
        long reservationId,
        ReservationStatus_Refactored status
) {
}
