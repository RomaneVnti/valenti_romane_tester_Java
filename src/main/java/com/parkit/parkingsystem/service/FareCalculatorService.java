package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.model.Ticket;

public class FareCalculatorService {

    public void calculateFare(Ticket ticket) {
        calculateFare(ticket, false);
    }

    public void calculateFare(Ticket ticket, boolean discount) {
        validateTicketTimes(ticket);

        long durationInMillis = ticket.getOutTime().getTime() - ticket.getInTime().getTime();
        double durationInHours = durationInMillis / 3600000.0;

        if (durationInMillis < 1800000) {
            ticket.setPrice(0);
        } else {
            double rate = getRateByParkingType(ticket.getParkingSpot().getParkingType());
            if (discount) {
                rate *= 0.95; // Apply a 5% discount
            }
            ticket.setPrice(durationInHours * rate);
        }
    }

    private void validateTicketTimes(Ticket ticket) {
        if (ticket.getInTime() == null || ticket.getOutTime() == null) {
            throw new IllegalArgumentException("In time or out time cannot be null.");
        }

        if (ticket.getOutTime().before(ticket.getInTime())) {
            throw new IllegalArgumentException("Out time must be after in time. In time: " + ticket.getInTime() + ", Out time: " + ticket.getOutTime());
        }
    }

    private double getRateByParkingType(ParkingType parkingType) {
        switch (parkingType) {
            case CAR:
                return Fare.CAR_RATE_PER_HOUR;
            case BIKE:
                return Fare.BIKE_RATE_PER_HOUR;
            default:
                throw new IllegalArgumentException("Unknown Parking Type");
        }
    }
}
