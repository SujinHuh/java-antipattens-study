package com.example.reservation;

public class ReservationController {
    private final ReservationService reservationService = new ReservationService();

    public String createReservation(String userId, String roomId, String startHour, String endHour, String status) {
        try {
            ReservationRequest request = new ReservationRequest();
            request.userId = Long.parseLong(userId);
            request.roomId = Long.parseLong(roomId);
            request.startHour = Integer.parseInt(startHour);
            request.endHour = Integer.parseInt(endHour);
            request.status = status;

            if (request.startHour < 0 || request.endHour > 24 || request.startHour >= request.endHour) {
                return "400 BAD_REQUEST";
            }

            return reservationService.reserve(request);
        } catch (Exception e) {
            return "500 INTERNAL_SERVER_ERROR";
        }
    }

    public String cancelReservation(String reservationId) {
        return reservationService.cancel(Long.parseLong(reservationId));
    }
}

