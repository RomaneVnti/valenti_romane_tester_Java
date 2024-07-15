package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import com.parkit.parkingsystem.model.Ticket;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;

    @Mock
    private static InputReaderUtil inputReaderUtil;

    @BeforeAll
    public static void setUp() throws Exception {
        parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
        ticketDAO = new TicketDAO();
        ticketDAO.dataBaseConfig = dataBaseTestConfig;
        dataBasePrepareService = new DataBasePrepareService();
    }

    @BeforeEach
    public void setUpPerTest() throws Exception {
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        dataBasePrepareService.clearDataBaseEntries();
    }

    @AfterAll
    public static void tearDown() {
        dataBasePrepareService.clearDataBaseEntries();
    }

    @Test
    public void testParkingACar() {
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processIncomingVehicle();

        // Vérifier qu'un ticket est effectivement enregistré dans la base de données
        Ticket ticket = ticketDAO.getTicket("ABCDEF");
        assertNotNull(ticket, "Ticket should be saved in the database");

        // Vérifier que la table Parking est mise à jour avec la disponibilité
        int parkingSpotId = ticket.getParkingSpot().getId();
        boolean isAvailable = parkingSpotDAO.getParkingSpot(parkingSpotId).isAvailable();
        assertFalse(isAvailable, "Parking spot should be marked as not available");
    }

    @Test
    public void testParkingLotExit() {
        testParkingACar();
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processExitingVehicle();

        // Vérifier que le prix et l'heure de sortie sont bien remplis dans la base de données
        Ticket ticket = ticketDAO.getTicket("ABCDEF");
        assertNotNull(ticket.getOutTime(), "Out time should be populated in the database");
        assertNotNull(ticket.getPrice(), "Fare should be generated and populated in the database");

        // Vérifier que la place de parking est de nouveau disponible
        int parkingSpotId = ticket.getParkingSpot().getId();
        boolean isAvailable = parkingSpotDAO.getParkingSpot(parkingSpotId).isAvailable();
        assertTrue(isAvailable, "Parking spot should be marked as available");
    }

    @Test
    public void testParkingLotExitRecurringUser() {
        // Première entrée et sortie du véhicule pour l'enregistrer comme utilisateur récurrent
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processIncomingVehicle();

        // Simuler un temps d'attente pour que l'heure de sortie soit différente de l'heure d'entrée
        try {
            Thread.sleep(1000); // Attendre 1 seconde
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        parkingService.processExitingVehicle();

        // Deuxième entrée du même véhicule
        parkingService.processIncomingVehicle();

        // Simuler un temps d'attente pour que l'heure de sortie soit différente de l'heure d'entrée
        try {
            Thread.sleep(1000); // Attendre 1 seconde
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        parkingService.processExitingVehicle();

        // Vérifier que le prix avec remise a été appliqué
        Ticket ticket = ticketDAO.getTicket("ABCDEF");
        assertNotNull(ticket.getOutTime(), "Out time should be populated in the database");
        assertNotNull(ticket.getPrice(), "Fare should be generated and populated in the database");

        // Calculer le prix attendu avec la remise
        double expectedFare = calculateExpectedFareWithDiscount(ticket);
        assertEquals(expectedFare, ticket.getPrice(), 0.01, "Fare should include a 5% discount for recurring users");
    }

    private double calculateExpectedFareWithDiscount(Ticket ticket) {
        // Implémenter la logique de calcul du prix avec la remise de 5 %
        double duration = (ticket.getOutTime().getTime() - ticket.getInTime().getTime()) / (1000.0 * 60.0 * 60.0); // durée en heures
        double regularFare = duration * 1.5; // Utiliser le tarif par heure approprié
        return regularFare * 0.95; // appliquer une remise de 5 %
    }
}
