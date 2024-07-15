package com.parkit.parkingsystem;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.FareCalculatorService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Classe de test pour FareCalculatorService.
 */
public class FareCalculatorServiceTest {

    private static FareCalculatorService fareCalculatorService;
    private Ticket ticket;

    /**
     * Configuration initiale avant tous les tests.
     */
    @BeforeAll
    private static void setUp() {
        fareCalculatorService = new FareCalculatorService();
    }

    /**
     * Configuration avant chaque test.
     */
    @BeforeEach
    private void setUpPerTest() {
        ticket = new Ticket();
    }

    /**
     * Teste le calcul du tarif pour une voiture pour une durée de stationnement d'une heure.
     */
    @Test
    public void calculateFareCar() {
        setTicketDetails(ParkingType.CAR, 60);
        fareCalculatorService.calculateFare(ticket);
        assertEquals(Fare.CAR_RATE_PER_HOUR, ticket.getPrice());
    }

    /**
     * Teste le calcul du tarif pour un vélo pour une durée de stationnement d'une heure.
     */
    @Test
    public void calculateFareBike() {
        setTicketDetails(ParkingType.BIKE, 60);
        fareCalculatorService.calculateFare(ticket);
        assertEquals(Fare.BIKE_RATE_PER_HOUR, ticket.getPrice());
    }

    /**
     * Teste le calcul du tarif pour un type de parking inconnu.
     */
    @Test
    public void calculateFareUnknownType() {
        setTicketDetails(null, 60);
        assertThrows(NullPointerException.class, () -> fareCalculatorService.calculateFare(ticket));
    }

    /**
     * Teste le calcul du tarif pour un vélo avec une heure d'entrée future.
     */
    @Test
    public void calculateFareBikeWithFutureInTime() {
        setTicketDetails(ParkingType.BIKE, -60);
        assertThrows(IllegalArgumentException.class, () -> fareCalculatorService.calculateFare(ticket));
    }

    /**
     * Teste le calcul du tarif pour un vélo avec une durée de stationnement de 45 minutes.
     */
    @Test
    public void calculateFareBikeWithLessThanOneHourParkingTime() {
        setTicketDetails(ParkingType.BIKE, 45);
        fareCalculatorService.calculateFare(ticket);
        assertEquals(0.75 * Fare.BIKE_RATE_PER_HOUR, ticket.getPrice());
    }

    /**
     * Teste le calcul du tarif pour une voiture avec une durée de stationnement de 45 minutes.
     */
    @Test
    public void calculateFareCarWithLessThanOneHourParkingTime() {
        setTicketDetails(ParkingType.CAR, 45);
        fareCalculatorService.calculateFare(ticket);
        assertEquals(0.75 * Fare.CAR_RATE_PER_HOUR, ticket.getPrice());
    }

    /**
     * Teste le calcul du tarif pour une voiture avec une durée de stationnement de plus d'une journée.
     */
    @Test
    public void calculateFareCarWithMoreThanADayParkingTime() {
        setTicketDetails(ParkingType.CAR, 24 * 60);
        fareCalculatorService.calculateFare(ticket);
        assertEquals(24 * Fare.CAR_RATE_PER_HOUR, ticket.getPrice());
    }

    /**
     * Teste le calcul du tarif pour une voiture avec une durée de stationnement de moins de 30 minutes.
     */
    @Test
    public void calculateFareCarWithLessThan30MinutesParkingTime() {
        setTicketDetails(ParkingType.CAR, 25);
        fareCalculatorService.calculateFare(ticket);
        assertEquals(0, ticket.getPrice());
    }

    /**
     * Teste le calcul du tarif pour un vélo avec une durée de stationnement de moins de 30 minutes.
     */
    @Test
    public void calculateFareBikeWithLessThan30MinutesParkingTime() {
        setTicketDetails(ParkingType.BIKE, 25);
        fareCalculatorService.calculateFare(ticket);
        assertEquals(0, ticket.getPrice());
    }

    /**
     * Teste le calcul du tarif pour un vélo avec une remise.
     */
    @Test
    public void calculateFareBikeWithDiscount() {
        setTicketDetails(ParkingType.BIKE, 45);
        ticket.setDiscount(true);
        fareCalculatorService.calculateFare(ticket, true);
        assertEquals(0.95 * Fare.BIKE_RATE_PER_HOUR * 0.75, ticket.getPrice(), 0.01);
    }

    /**
     * Teste le calcul du tarif pour une voiture avec une remise.
     */
    @Test
    public void calculateFareCarWithDiscount() {
        setTicketDetails(ParkingType.CAR, 45);
        ticket.setDiscount(true);
        fareCalculatorService.calculateFare(ticket, true);
        assertEquals(0.95 * Fare.CAR_RATE_PER_HOUR * 0.75, ticket.getPrice(), 0.01);
    }

    /**
     * Défini les détails du ticket.
     *
     * @param parkingType le type de parking.
     * @param duration    la durée en minutes.
     */
    private void setTicketDetails(ParkingType parkingType, int duration) {
        Date inTime = new Date();
        inTime.setTime(System.currentTimeMillis() - (duration * 60 * 1000));
        Date outTime = new Date();
        ParkingSpot parkingSpot = new ParkingSpot(1, parkingType, false);

        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);
    }
}
