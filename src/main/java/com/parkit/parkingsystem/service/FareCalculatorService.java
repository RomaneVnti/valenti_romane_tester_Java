package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.model.Ticket;

/**
 * Service de calcul des tarifs de stationnement.
 */
public class FareCalculatorService {

    private static final double DISCOUNT_RATE = 0.95;
    private static final double FREE_PARKING_DURATION = 0.5;

    private boolean isDiscount;

    public FareCalculatorService() {
        this.isDiscount = false;
    }

    // Constructeur avec option de remise
    public FareCalculatorService(boolean isDiscount) {
        this.isDiscount = isDiscount;
    }

    /**
     * Calcul le tarif de stationnement pour un ticket.
     * Applique une remise si indiquée.
     *
     * @param ticket le ticket de stationnement
     */
    public void calculateFare(Ticket ticket) {
        calculateFare(ticket, this.isDiscount);
    }

    /**
     * Calcul le tarif de stationnement pour un ticket en tenant compte de la remise spécifiée.
     *
     * @param ticket     le ticket de stationnement
     * @param isDiscount si une remise doit être appliquée
     */
    public void calculateFare(Ticket ticket, boolean isDiscount) {
        validateTicketTimes(ticket);

        double durationInHours = calculateDuration(ticket);

        if (durationInHours <= FREE_PARKING_DURATION) {
            ticket.setPrice(0);
            return;
        }

        double ratePerHour = getRatePerHour(ticket.getParkingSpot().getParkingType());
        double price = durationInHours * ratePerHour;

        if (isDiscount) {
            price *= DISCOUNT_RATE;
        }

        ticket.setPrice(price);
    }

    /**
     * Calcul la durée de stationnement en heures.
     *
     * @param ticket le ticket de stationnement
     * @return la durée en heures
     */
    private double calculateDuration(Ticket ticket) {
        long inTime = ticket.getInTime().getTime();
        long outTime = ticket.getOutTime().getTime();
        return (double) (outTime - inTime) / (60 * 60 * 1000);
    }

    /**
     * Obtient le tarif horaire en fonction du type de stationnement.
     *
     * @param parkingType le type de stationnement
     * @return le tarif horaire
     */
    private double getRatePerHour(ParkingType parkingType) {
        switch (parkingType) {
            case CAR:
                return Fare.CAR_RATE_PER_HOUR;
            case BIKE:
                return Fare.BIKE_RATE_PER_HOUR;
            default:
                throw new IllegalArgumentException("Unknown Parking Type");
        }
    }

    /**
     * Valide les heures du ticket pour s'assurer que l'heure de sortie est après l'heure d'entrée.
     *
     * @param ticket le ticket de stationnement
     * @throws IllegalArgumentException si les heures ne sont pas valides
     */
    private void validateTicketTimes(Ticket ticket) {
        if (ticket.getOutTime() == null || ticket.getInTime() == null) {
            throw new IllegalArgumentException("In time and out time must be provided");
        }

        if (ticket.getOutTime().before(ticket.getInTime())) {
            throw new IllegalArgumentException("Out time must be after in time. In time: " + ticket.getInTime() + ", Out time: " + ticket.getOutTime());
        }
    }

    public void setDiscount(boolean discount) {
        this.isDiscount = discount;
    }

    public boolean isDiscount() {
        return this.isDiscount;
    }
}
