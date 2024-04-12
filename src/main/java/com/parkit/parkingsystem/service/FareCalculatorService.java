package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

public class FareCalculatorService {

        public void calculateFare(Ticket ticket){
            if( (ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime())) ){
                throw new IllegalArgumentException("Out time provided is incorrect:" + ticket.getOutTime().toString());
            }

            // Calculating duration in milliseconds
            long durationInMillis = ticket.getOutTime().getTime() - ticket.getInTime().getTime();

            // Convert milliseconds to hours (1 hour = 3600000 millis)
            double durationInHours = durationInMillis / 3600000.0;

            // If duration is less than or equal to 30 minutes, set price to 0
            if (durationInHours <= 0.5) {
                ticket.setPrice(0);
                return;
            }

            switch (ticket.getParkingSpot().getParkingType()){
                case CAR: {
                    ticket.setPrice(durationInHours * Fare.CAR_RATE_PER_HOUR);
                    break;
                }
                case BIKE: {
                    ticket.setPrice(durationInHours * Fare.BIKE_RATE_PER_HOUR);
                    break;
                }
                default:
                    throw new IllegalArgumentException("Unknown Parking Type");
            }
        }
    }
