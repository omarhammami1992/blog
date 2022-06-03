package com.blog.soat.entity;

import java.util.List;

public class BookingDetails {
   private final String trainId;
   private final String bookingReference;
   private final List<FreeSeat> freeSeats;

   private BookingDetails(String trainId, String bookingReference, List<FreeSeat> freeSeats) {
      this.trainId = trainId;
      this.bookingReference = bookingReference;
      this.freeSeats = freeSeats;
   }

   public static Builder builder() {
      return new Builder();
   }

   public String getTrainId() {
      return trainId;
   }

   public String getBookingReference() {
      return bookingReference;
   }

   public List<FreeSeat> getFreeSeats() {
      return freeSeats;
   }

   public static final class Builder {
      private String trainId;
      private String bookingReference;
      private List<FreeSeat> freeSeats;


      public Builder trainId(String trainId) {
         this.trainId = trainId;
         return this;
      }

      public Builder bookingReference(String bookingReference) {
         this.bookingReference = bookingReference;
         return this;
      }

      public Builder freeSeats(List<FreeSeat> freeSeats) {
         this.freeSeats = freeSeats;
         return this;
      }

      public BookingDetails build() {
         return new BookingDetails(trainId, bookingReference, freeSeats);
      }
   }

}
