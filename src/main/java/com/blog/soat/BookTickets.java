package com.blog.soat;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import com.blog.soat.entity.BookingDetails;
import com.blog.soat.entity.FreeSeat;
import com.blog.soat.entity.Seat;
import com.blog.soat.repository.BookingReferenceRepository;
import com.blog.soat.repository.TrainRepository;

public class BookTickets {
   private final TrainRepository trainRepository;
   private final BookingReferenceRepository bookingReferenceRepository;

   public BookTickets(TrainRepository trainRepository, BookingReferenceRepository bookingReferenceRepository) {
      this.trainRepository = trainRepository;
      this.bookingReferenceRepository = bookingReferenceRepository;
   }

   public BookingDetails execute(String trainId, Integer seatRequest) {
      List<Seat> seats = trainRepository.find(trainId);
      Map<String, List<Seat>> freeSeatsByCoach = filterFreeSeatsAndGroupByCoach(seats);

      Optional<List<Seat>> coachSeats = findEligibleCoach(seatRequest, freeSeatsByCoach);

      if (coachSeats.isPresent()) {
         List<FreeSeat> seatsToBook = buildSeatsToBook(seatRequest, coachSeats.get());
         String bookingReference = bookingReferenceRepository.generate();
         return BookingDetails.builder().trainId(trainId).bookingReference(bookingReference).freeSeats(seatsToBook).build();
      } else {
         return BookingDetails.builder().trainId(trainId).build();
      }
   }

   private Optional<List<Seat>> findEligibleCoach(Integer seatRequest, Map<String, List<Seat>> freeSeatsByCoach) {
      return freeSeatsByCoach.values().stream()
            .filter(trainSeats -> trainSeats.size() >= seatRequest)
            .findFirst();
   }

   private Map<String, List<Seat>> filterFreeSeatsAndGroupByCoach(List<Seat> seats) {
      return seats.stream()
            .filter(Seat::isAvailable)
            .collect(Collectors.groupingBy(Seat::coach));
   }

   private List<FreeSeat> buildSeatsToBook(Integer seatRequest, List<Seat> seats) {
      return seats.stream()
            .limit(seatRequest)
            .map(seat -> new FreeSeat(seat.coach(), seat.seatNumber()))
            .toList();
   }
}
