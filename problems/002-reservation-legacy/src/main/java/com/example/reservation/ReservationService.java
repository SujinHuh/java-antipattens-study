package com.example.reservation;

import java.time.LocalDateTime;

public class ReservationService {
    private final ReservationRepository reservationRepository = new ReservationRepository();

    @Transactional
    public String reserve(ReservationRequest request) {
        if ("BLOCKED".equals(request.status)) {
            throw new ReservationException("invalid reservation");
        }

        if (reservationRepository.existsOverlapping(request.roomId, request.startHour, request.endHour)) {
            throw new ReservationException("already reserved");
        }

        Reservation reservation = new Reservation();
        reservation.id = System.currentTimeMillis();
        reservation.userId = request.userId;
        reservation.roomId = request.roomId;
        reservation.startHour = request.startHour;
        reservation.endHour = request.endHour;
        reservation.status = request.status;
        reservation.createdAt = LocalDateTime.now().toString();

        saveReservation(reservation);

        return "reservationId=" + reservation.id + ", status=" + reservation.status;
    }

    @Transactional
    public void saveReservation(Reservation reservation) {
        reservationRepository.save(reservation);
    }

    @Transactional
    public String cancel(long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId);

        if (reservation == null) {
            return "404 NOT_FOUND";
        }

        if ("CANCELLED".equals(reservation.status)) {
            return "409 ALREADY_CANCELLED";
        }

        reservation.status = "CANCELLED";
        reservationRepository.save(reservation);

        return "200 OK";
    }
}

