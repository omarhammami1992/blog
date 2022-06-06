package com.blog.soat.fixture;

import java.util.List;
import com.blog.soat.entity.BookingDetails;
import com.blog.soat.entity.FreeSeat;

public class BookingDetailsFixture {

   public static BookingDetails createEmpty(String trainId) {
      return BookingDetails.builder().trainId(trainId).build();
   }

   public static BookingDetails create(String trainId, String bookingReference, List<FreeSeat> freeSeats) {
      return BookingDetails.builder().trainId(trainId).bookingReference(bookingReference).freeSeats(freeSeats).build();
   }

}
