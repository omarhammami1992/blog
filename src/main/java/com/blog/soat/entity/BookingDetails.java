package com.blog.soat.entity;

import java.util.List;

public interface BookingDetails {
   String getTrainId();

   String getBookingReference();

   List<FreeSeat> getFreeSeats();
}
