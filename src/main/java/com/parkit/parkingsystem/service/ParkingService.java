package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Date;

/**
 * Service de gestion du parking.
 */
public class ParkingService {

    private static final Logger logger = LogManager.getLogger("ParkingService");
    private static final FareCalculatorService fareCalculatorService = new FareCalculatorService();

    private final InputReaderUtil inputReaderUtil;
    private final ParkingSpotDAO parkingSpotDAO;
    private final TicketDAO ticketDAO;

    /**
     * Constructeur du service de gestion du parking.
     *
     * @param inputReaderUtil  Utilitaire de lecture des entrées utilisateur.
     * @param parkingSpotDAO   DAO pour la gestion des places de parking.
     * @param ticketDAO        DAO pour la gestion des tickets.
     */
    public ParkingService(InputReaderUtil inputReaderUtil, ParkingSpotDAO parkingSpotDAO, TicketDAO ticketDAO) {
        this.inputReaderUtil = inputReaderUtil;
        this.parkingSpotDAO = parkingSpotDAO;
        this.ticketDAO = ticketDAO;
    }

    /**
     * Traite l'entrée d'un véhicule.
     */
    public void processIncomingVehicle() {
        try {
            ParkingSpot parkingSpot = getNextParkingNumberIfAvailable();
            if (parkingSpot != null && parkingSpot.getId() > 0) {
                String vehicleRegNumber = getVehicleRegNumber();
                int numTickets = ticketDAO.getNbTickets(vehicleRegNumber);

                parkingSpot.setAvailable(false);
                parkingSpotDAO.updateParking(parkingSpot);

                Date inTime = new Date();
                Ticket ticket = createTicket(parkingSpot, vehicleRegNumber, inTime, numTickets > 0);

                ticketDAO.saveTicket(ticket);

                printWelcomeMessage(parkingSpot, vehicleRegNumber, inTime, numTickets > 0);
            }
        } catch (Exception e) {
            logger.error("Unable to process incoming vehicle", e);
        }
    }

    /**
     * Obtient le numéro d'immatriculation du véhicule de l'utilisateur.
     *
     * @return Le numéro d'immatriculation du véhicule.
     * @throws Exception En cas d'erreur de lecture.
     */
    private String getVehicleRegNumber() throws Exception {
        System.out.println("Please type the vehicle registration number and press enter key");
        return inputReaderUtil.readVehicleRegistrationNumber();
    }

    /**
     * Obtient le prochain numéro de place de parking disponible.
     *
     * @return La prochaine place de parking disponible ou null si aucune place n'est disponible.
     */
    public ParkingSpot getNextParkingNumberIfAvailable() {
        try {
            ParkingType parkingType = getVehicleType();
            int parkingNumber = parkingSpotDAO.getNextAvailableSlot(parkingType);

            if (parkingNumber > 0) {
                return new ParkingSpot(parkingNumber, parkingType, true);
            } else {
                throw new Exception("Parking slots might be full");
            }
        } catch (IllegalArgumentException ie) {
            logger.error("Error parsing user input for type of vehicle", ie);
        } catch (Exception e) {
            logger.error("Error fetching next available parking slot", e);
        }
        return null;
    }

    /**
     * Obtient le type de véhicule de l'utilisateur.
     *
     * @return Le type de véhicule.
     */
    private ParkingType getVehicleType() {
        System.out.println("Please select vehicle type from menu");
        System.out.println("1 CAR");
        System.out.println("2 BIKE");
        int input = inputReaderUtil.readSelection();
        switch (input) {
            case 1:
                return ParkingType.CAR;
            case 2:
                return ParkingType.BIKE;
            default:
                throw new IllegalArgumentException("Entered input is invalid");
        }
    }

    /**
     * Traite la sortie d'un véhicule.
     */
    public void processExitingVehicle() {
        try {
            String vehicleRegNumber = getVehicleRegNumber();
            Ticket ticket = ticketDAO.getTicket(vehicleRegNumber);
            if (ticket == null) {
                logger.error("Ticket not found for vehicleRegNumber: " + vehicleRegNumber);
                return;
            }

            Date outTime = new Date();
            ticket.setOutTime(outTime);
            logger.info("Setting outTime: " + outTime);

            boolean isRecurrent = ticketDAO.getNbTickets(vehicleRegNumber) > 1;
            fareCalculatorService.calculateFare(ticket, isRecurrent);
            ticket.setDiscount(isRecurrent);

            if (ticketDAO.updateTicket(ticket)) {
                ParkingSpot parkingSpot = ticket.getParkingSpot();
                parkingSpot.setAvailable(true);
                if (parkingSpotDAO.updateParking(parkingSpot)) {
                    printExitMessage(ticket, isRecurrent, outTime);
                } else {
                    System.out.println("Unable to update parking spot availability. Error occurred");
                }
            } else {
                System.out.println("Unable to update ticket information. Error occurred");
            }
        } catch (Exception e) {
            logger.error("Unable to process exiting vehicle", e);
        }
    }

    /**
     * Crée un ticket de parking.
     *
     * @param parkingSpot       La place de parking.
     * @param vehicleRegNumber  Le numéro d'immatriculation du véhicule.
     * @param inTime            L'heure d'entrée.
     * @param hasDiscount       Indique si une remise est applicable.
     * @return Le ticket de parking créé.
     */
    private Ticket createTicket(ParkingSpot parkingSpot, String vehicleRegNumber, Date inTime, boolean hasDiscount) {
        Ticket ticket = new Ticket();
        ticket.setParkingSpot(parkingSpot);
        ticket.setVehicleRegNumber(vehicleRegNumber);
        ticket.setPrice(0);
        ticket.setInTime(inTime);
        ticket.setOutTime(null);
        ticket.setDiscount(hasDiscount);
        return ticket;
    }

    /**
     * Affiche un message de bienvenue pour l'utilisateur.
     *
     * @param parkingSpot       La place de parking.
     * @param vehicleRegNumber  Le numéro d'immatriculation du véhicule.
     * @param inTime            L'heure d'entrée.
     * @param isRecurring       Indique si l'utilisateur est récurrent.
     */
    private void printWelcomeMessage(ParkingSpot parkingSpot, String vehicleRegNumber, Date inTime, boolean isRecurring) {
        System.out.println("Generated Ticket and saved in DB");
        System.out.println("Please park your vehicle in spot number:" + parkingSpot.getId());
        System.out.println("Recorded in-time for vehicle number:" + vehicleRegNumber + " is:" + inTime);
        if (isRecurring) {
            System.out.println("Welcome back! As a recurring user, you are eligible for discounts.");
        } else {
            System.out.println("Welcome! If you visit again, you may be eligible for discounts.");
        }
    }

    /**
     * Affiche un message de sortie pour l'utilisateur.
     *
     * @param ticket       Le ticket de parking.
     * @param isRecurrent  Indique si l'utilisateur est récurrent.
     * @param outTime      L'heure de sortie.
     */
    private void printExitMessage(Ticket ticket, boolean isRecurrent, Date outTime) {
        System.out.println("Please pay the parking fare: " + ticket.getPrice());
        if (isRecurrent) {
            System.out.println("You received a 5% discount!");
        }
        System.out.println("Recorded out-time for vehicle number: " + ticket.getVehicleRegNumber() + " is: " + outTime);
    }
}
