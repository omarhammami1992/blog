package com.blog.soat.entity;

public record Seat(String coach, int seatNumber, String bookingReference) {
   public boolean isAvailable() {
      return bookingReference == null;
   }
}
