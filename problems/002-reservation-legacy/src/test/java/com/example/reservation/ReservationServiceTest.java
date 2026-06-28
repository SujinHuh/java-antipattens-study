package com.example.reservation;

public class ReservationServiceTest {
    public void reserveRoom() {
        ReservationService service = new ReservationService();

        ReservationRequest request = new ReservationRequest();
        request.userId = 1L;
        request.roomId = 10L;
        request.startHour = 10;
        request.endHour = 12;
        request.status = "CONFIRMED";

        String result = service.reserve(request);

        if (!result.contains("status=CONFIRMED")) {
            throw new AssertionError("reservation failed");
        }
    }
}

