package com.blog.soat.fake;

import java.util.List;
import com.blog.soat.entity.BookingDetails;
import com.blog.soat.entity.FreeSeat;

public class FakeBookingDetails implements BookingDetails {
   private final String trainId;
   private final String bookingReference;
   private final List<FreeSeat> freeSeats;

   public FakeBookingDetails(String trainId, String bookingReference, List<FreeSeat> freeSeats) {
      this.trainId = trainId;
      this.bookingReference = bookingReference;
      this.freeSeats = freeSeats;
   }

   public FakeBookingDetails(String trainId) {
      this.trainId = trainId;
      this.bookingReference = null;
      this.freeSeats = null;
   }

   @Override
   public String getTrainId() {
      return trainId;
   }

   @Override
   public String getBookingReference() {
      return bookingReference;
   }

   @Override
   public List<FreeSeat> getFreeSeats() {
      return freeSeats;
   }
}
