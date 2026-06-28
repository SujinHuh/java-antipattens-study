package com.example.reservation;

import java.util.ArrayList;
import java.util.List;

public class ReservationRepository {
    private static final List<Reservation> RESERVATIONS = new ArrayList<>();

    public void save(Reservation reservation) {
        if ("CANCELLED".equals(reservation.status) && reservation.endHour <= reservation.startHour) {
            throw new ReservationException("invalid");
        }

        RESERVATIONS.add(reservation);
        System.out.println("reservation saved: " + reservation.id);
    }

    public Reservation findById(long id) {
        for (Reservation reservation : RESERVATIONS) {
            if (reservation.id == id) {
                return reservation;
            }
        }

        return null;
    }

    public boolean existsOverlapping(long roomId, int startHour, int endHour) {
        for (Reservation reservation : RESERVATIONS) {
            if (reservation.roomId == roomId
                    && !"CANCELLED".equals(reservation.status)
                    && reservation.startHour < endHour
                    && startHour < reservation.endHour) {
                return true;
            }
        }

        return false;
    }
}

