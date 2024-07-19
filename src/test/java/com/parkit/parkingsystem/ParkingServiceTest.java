package com.parkit.parkingsystem;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Classe de test pour ParkingService.
 */
@ExtendWith(MockitoExtension.class)
public class ParkingServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(ParkingServiceTest.class);

    @InjectMocks
    private ParkingService parkingService;

    @Mock
    private InputReaderUtil inputReaderUtil;
    @Mock
    private ParkingSpotDAO parkingSpotDAO;
    @Mock
    private TicketDAO ticketDAO;

    /**
     * Configuration avant chaque test.
     */
    @BeforeEach
    public void setUpPerTest() throws Exception {
        lenient().when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");

        ParkingSpot parkingSpot = createParkingSpot(1, ParkingType.CAR, false);
        Ticket ticket = createTicket(parkingSpot, "ABCDEF", new Date(System.currentTimeMillis() - (60 * 60 * 1000)));

        lenient().when(ticketDAO.getTicket(anyString())).thenReturn(ticket);
        lenient().when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true);
        lenient().when(ticketDAO.getNbTickets(anyString())).thenReturn(1);
        lenient().when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);
    }

    /**
     * Teste le processus de sortie d'un véhicule.
     */
    @Test
    public void processExitingVehicleTest() {
        parkingService.processExitingVehicle();
        verify(parkingSpotDAO, times(1)).updateParking(any(ParkingSpot.class));
        verify(ticketDAO, times(1)).updateTicket(any(Ticket.class));
        verify(ticketDAO, times(1)).getNbTickets("ABCDEF");
    }

    /**
     * Teste le processus d'entrée d'un véhicule.
     */
    @Test
    public void testProcessIncomingVehicle() throws Exception {
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(1);
        ParkingSpot parkingSpot = createParkingSpot(1, ParkingType.CAR, true);
        when(parkingSpotDAO.updateParking(parkingSpot)).thenReturn(true);
        when(ticketDAO.saveTicket(any(Ticket.class))).thenReturn(true);

        parkingService.processIncomingVehicle();

        verify(parkingSpotDAO, times(1)).updateParking(parkingSpot);
        verify(ticketDAO, times(1)).saveTicket(any(Ticket.class));
    }

    /**
     * Teste le processus de sortie d'un véhicule lorsque la mise à jour du ticket échoue.
     */
    @Test
    public void processExitingVehicleTestUnableUpdate() {
        when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(false);

        parkingService.processExitingVehicle();

        verify(ticketDAO, times(1)).updateTicket(any(Ticket.class));
        verify(parkingSpotDAO, never()).updateParking(any(ParkingSpot.class));
    }

    /**
     * Teste la récupération du prochain numéro de parking disponible.
     */
    @Test
    public void testGetNextParkingNumberIfAvailable() {
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(1);
        ParkingSpot expectedParkingSpot = createParkingSpot(1, ParkingType.CAR, true);

        ParkingSpot parkingSpot = parkingService.getNextParkingNumberIfAvailable();

        assertNotNull(parkingSpot);
        assertEquals(expectedParkingSpot.getId(), parkingSpot.getId());
        assertEquals(expectedParkingSpot.getParkingType(), parkingSpot.getParkingType());
        assertEquals(expectedParkingSpot.isAvailable(), parkingSpot.isAvailable());
    }

    /**
     * Teste la récupération du prochain numéro de parking disponible lorsque aucun emplacement n'est trouvé.
     */
    @Test
    public void testGetNextParkingNumberIfAvailableParkingNumberNotFound() {
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(0);

        ParkingSpot parkingSpot = parkingService.getNextParkingNumberIfAvailable();

        assertNull(parkingSpot);
    }

    /**
     * Teste la récupération du prochain numéro de parking disponible avec un argument incorrect.
     */
    @Test
    public void testGetNextParkingNumberIfAvailableParkingNumberWrongArgument() {
        when(inputReaderUtil.readSelection()).thenReturn(3);

        ParkingSpot parkingSpot = parkingService.getNextParkingNumberIfAvailable();

        assertNull(parkingSpot);
    }

    /**
     * Crée une nouvelle instance de ParkingSpot.
     *
     * @param id          l'identifiant de la place de parking.
     * @param parkingType le type de parking (voiture ou vélo).
     * @param isAvailable la disponibilité de la place de parking.
     * @return une instance de ParkingSpot.
     */
    private ParkingSpot createParkingSpot(int id, ParkingType parkingType, boolean isAvailable) {
        return new ParkingSpot(id, parkingType, isAvailable);
    }

    /**
     * Crée une nouvelle instance de Ticket.
     *
     * @param parkingSpot      la place de parking associée.
     * @param vehicleRegNumber le numéro d'immatriculation du véhicule.
     * @param inTime           l'heure d'entrée du véhicule.
     * @return une instance de Ticket.
     */
    private Ticket createTicket(ParkingSpot parkingSpot, String vehicleRegNumber, Date inTime) {
        Ticket ticket = new Ticket();
        ticket.setParkingSpot(parkingSpot);
        ticket.setVehicleRegNumber(vehicleRegNumber);
        ticket.setInTime(inTime);
        return ticket;
    }
}
