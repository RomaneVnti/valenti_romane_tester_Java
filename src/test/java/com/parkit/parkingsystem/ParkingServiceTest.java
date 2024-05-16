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

import java.util.Date;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ParkingServiceTest {

    @InjectMocks
    private ParkingService parkingService;

    @Mock
    private InputReaderUtil inputReaderUtil;
    @Mock
    private ParkingSpotDAO parkingSpotDAO;
    @Mock
    private TicketDAO ticketDAO;

    @BeforeEach
    public void setUpPerTest() {
        try {
            // Mocking only for processExitingVehicleTest
            lenient().when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");

            ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);
            Ticket ticket = new Ticket();
            ticket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000)));
            ticket.setParkingSpot(parkingSpot);
            ticket.setVehicleRegNumber("ABCDEF");
            lenient().when(ticketDAO.getTicket(anyString())).thenReturn(ticket);
            lenient().when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true);
            lenient().when(ticketDAO.getNbTickets(anyString())).thenReturn(1);  // Mocking getNbTickets

            lenient().when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);

            parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to set up test mock objects");
        }
    }

    @Test
    public void processExitingVehicleTest() {
        // Act
        parkingService.processExitingVehicle();

        // Assert
        verify(parkingSpotDAO, times(1)).updateParking(any(ParkingSpot.class));
        verify(ticketDAO, times(1)).updateTicket(any(Ticket.class));
        verify(ticketDAO, times(1)).getNbTickets("ABCDEF");  // Verifying getNbTickets is called
    }

    @Test
    public void testProcessIncomingVehicle() {
        try {
            // Given
            when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
            when(inputReaderUtil.readSelection()).thenReturn(1); // Mocking vehicle type selection (1 for CAR)
            when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(1);
            ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, true);
            when(parkingSpotDAO.updateParking(parkingSpot)).thenReturn(true);
            when(ticketDAO.saveTicket(any(Ticket.class))).thenReturn(true);

            // When
            parkingService.processIncomingVehicle();

            // Then
            verify(parkingSpotDAO, times(1)).updateParking(parkingSpot);
            verify(ticketDAO, times(1)).saveTicket(any(Ticket.class));
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to test processIncomingVehicle");
        }
    }
}
