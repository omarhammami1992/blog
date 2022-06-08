package com.blog.soat.entity;

import java.util.List;

public class BookingDetailsDto implements BookingDetails {
   private final String trainId;
   private final String bookingReference;
   private final List<FreeSeat> freeSeats;

   private BookingDetailsDto(String trainId, String bookingReference, List<FreeSeat> freeSeats) {
      this.trainId = trainId;
      this.bookingReference = bookingReference;
      this.freeSeats = freeSeats;
   }

   public static Builder builder() {
      return new Builder();
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

      public BookingDetailsDto build() {
         return new BookingDetailsDto(trainId, bookingReference, freeSeats);
      }
   }

}
