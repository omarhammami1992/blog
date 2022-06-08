package com.blog.soat;

import static java.util.List.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.blog.soat.entity.BookingDetails;
import com.blog.soat.entity.FreeSeat;
import com.blog.soat.entity.Seat;
import com.blog.soat.fake.FakeBookingDetails;
import com.blog.soat.repository.BookingReferenceRepository;
import com.blog.soat.repository.TrainRepository;

@ExtendWith(MockitoExtension.class)
class BookTicketsUTest {

   private static final String BOOKING_REFERENCE = "00000000";
   private static final String TRAIN_ID = "9043-2018-05-24";

   @Mock
   private TrainRepository trainRepository;

   @Mock
   private BookingReferenceRepository bookingReferenceRepository;

   private BookTickets bookTickets;

   @BeforeEach
   void setUp() {
      bookTickets = new BookTickets(trainRepository, bookingReferenceRepository);
   }

   @Test
   void should_book_tickets_when_enough_free_seats() {
      // given
      Seat seatA1 = new Seat("A", 1, "11111111");
      Seat seatA2 = new Seat("A", 2, null);
      when(trainRepository.find(TRAIN_ID)).thenReturn(of(seatA1, seatA2));

      when(bookingReferenceRepository.generate()).thenReturn(BOOKING_REFERENCE);
      Integer seatRequest = 1;

      // then
      BookingDetails bookingDetails = bookTickets.execute(TRAIN_ID, seatRequest);

      // then
      BookingDetails expectedBookingDetails = new FakeBookingDetails(TRAIN_ID, BOOKING_REFERENCE, of(new FreeSeat("A", 2)));
      assertThat(bookingDetails).usingRecursiveComparison().isEqualTo(expectedBookingDetails);
   }

   @Test
   void should_not_book_tickets_when_not_enough_free_seats() {
      // given
      Seat seatA1 = new Seat("A", 1, "11111111");
      Seat seatA2 = new Seat("A", 2, "11111111");
      Seat seatA3 = new Seat("A", 3, null);
      when(trainRepository.find(TRAIN_ID)).thenReturn(of(seatA1, seatA2, seatA3));

      Integer seatRequest = 2;

      // then
      BookingDetails bookingDetails = bookTickets.execute(TRAIN_ID, seatRequest);

      // then
      BookingDetails expectedBookingDetails = new FakeBookingDetails(TRAIN_ID);
      assertThat(bookingDetails).usingRecursiveComparison().isEqualTo(expectedBookingDetails);
   }

   @Test
   void should_not_book_tickets_when_not_enough_free_seats_in_same_coach() {
      // given
      Seat seatA1 = new Seat("A", 1, "11111111");
      Seat seatA2 = new Seat("A", 2, null);

      Seat seatB1 = new Seat("B", 1, "22222222");
      Seat seatB2 = new Seat("B", 2, null);

      when(trainRepository.find(TRAIN_ID)).thenReturn(of(seatA1, seatA2, seatB1, seatB2));

      Integer seatRequest = 2;

      // then
      BookingDetails bookingDetails = bookTickets.execute(TRAIN_ID, seatRequest);

      // then
      BookingDetails expectedBookingDetails = new FakeBookingDetails(TRAIN_ID);
      assertThat(bookingDetails).usingRecursiveComparison().isEqualTo(expectedBookingDetails);
   }
}