# Découpler les tests de leurs implémentations

## Contexte
Le test logiciel est une discipline qui vise à s'assurer que le logiciel répond à ses objectifs fonctionnels et qualitatifs. Il existe différentes techniques de tests pour vérifier que le logiciel est conforme aux besoins et aux attentes du client et personnellement j'utilise le Test Driven Development (TDD) depuis plus de 3 ans et j'ai vu les avantages qu'il offre.

Cette technique permet de maîtriser le coût des évolutions logicielles et s'approprier plus facilement à n'importe quel changement et ce grâce aux tests de non régression et à la conception du code perméable au changement. Elle permet aussi d'éviter les accidents de parcours et les modifications de code sans lien avec le but recherché vu qu'on se focalise étape par étape sur la satisfaction d'un besoin.

Force est de constater, qu'en se limitant à la définition du TDD un inconvénient peut se présenter: **un couplage entre les tests et l'implémentation**. Cela se traduit souvent par la maintenance des tests à chaque changement dans les choix techniques d'implémentation.

Pour mieux expliquer cette problématique j'ai pris l'exemple du code ci-dessous, développé en TDD et qui permet de **réserver des billets de train au sein d'un même wagon**.

##### Dans les tests:
```
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
      BookingDetails expectedBookingDetails = BookingDetailsFixture.create(TRAIN_ID, BOOKING_REFERENCE, of(new FreeSeat("A", 2)));
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
      BookingDetails expectedBookingDetails = BookingDetailsFixture.createEmpty(TRAIN_ID);
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
      BookingDetails expectedBookingDetails = BookingDetailsFixture.createEmpty(TRAIN_ID);
      assertThat(bookingDetails).usingRecursiveComparison().isEqualTo(expectedBookingDetails);
   }
}
```

##### Dans l'implémentation:
```
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

```

```
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
```

```
public record FreeSeat(String coach, int seatNumber) {
}
```

```
public record Seat(String coach, int seatNumber, String bookingReference) {
   public boolean isAvailable() {
      return bookingReference == null;
   }
}
```

```
public interface BookingReferenceRepository {
   String generate();
}
```

```
public interface TrainRepository {
   List<Seat> find(String trainId);
}
```

Comme vous l’avez remarqué, j’ai utilisé le pattern **builder** pour instancier les objets de la classe  **"BookingDetails”**.  Vous avez aussi pu constater, que ce pattern de construction est partagé entre le code de test et l'implémentation.

Pour une raison ou une autre, les choix d'implémentation peuvent changer. Je vous propose, par exemple, de refactorer de façon à utiliser un autre  pattern de construction pour la classe **“BookingDetails”** (avec new  ou une fabrique statique) ou de la  transformer en record.
Dans ces cas, le développeur est  obligé de parcourir non seulement les instances de l'implémentation mais aussi celle des tests  pour adapter le code.
C’est là que se trouve  le couplage.

Cet inconvénient n’est pas lié au TDD, mais plutôt à la façon avec laquelle nous avons écrit nos tests. Beaucoup de développeurs ne le détectent pas par manque de temps ou par ignorance.
Pour contourner ce problème, je vous propose deux solutions.

## Les fixture
Cette solution consiste à créer une classe responsable de l'instanciation et de l'initialisation des entités (**"BookingDetails”** dans notre cas)  et sera utilisée **que dans les tests**.

##### Dans les tests:
```
public class BookingDetailsFixture {

   public static BookingDetails createEmpty(String trainId) {
      return BookingDetails.builder().trainId(trainId).build();
   }

   public static BookingDetails create(String trainId, String bookingReference, List<FreeSeat> freeSeats) {
      return BookingDetails.builder().trainId(trainId).bookingReference(bookingReference).freeSeats(freeSeats).build();
   }
}
```

```
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
      BookingDetails expectedBookingDetails = BookingDetailsFixture.create(TRAIN_ID, BOOKING_REFERENCE, of(new FreeSeat("A", 2)));
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
      BookingDetails expectedBookingDetails = BookingDetailsFixture.createEmpty(TRAIN_ID);
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
      BookingDetails expectedBookingDetails = BookingDetailsFixture.createEmpty(TRAIN_ID);
      assertThat(bookingDetails).usingRecursiveComparison().isEqualTo(expectedBookingDetails);
   }
}
```

Cette classe réduit le couplage **mais ne le supprime pas**: en cas de changement du pattern de construction il n’y a que la fixture qui devra être maintenue vu que c’est la seule classe dans la partie des tests qui utilise le constructeur de l'implémentation.

## Les Interfaces et les fake classes

Le principe de cette solution est de passer par les interfaces pour définir ce qu’on souhaite exposer comme attribut et de faire une double implémentation : une pour le code de production et une pour le code de test.

**L'implémentation de code de tests** est partielle. Elle renvoie généralement les arguments fournis en paramètre du constructeur. Ce dernier ne contient aucune règle métier. Son rôle se limite à l'initialisation des attributs.

**L'implémentation du code de production** pourrait contenir les règles métier qui permettent d'initialiser les attributs de notre objet. Le constructeur de cette classe n'est pas utilisé dans les tests.

##### Dans l'implémentation:
```
public interface BookingDetails {
   String getTrainId();

   String getBookingReference();

   List<FreeSeat> getFreeSeats();
}
```

```
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
```

##### Dans les tests:
```
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
```

```
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
```
Grâce à l’interface **“BookingDetail”**, on a réussi à  implémenter deux constructeurs district : un pour les tests l’autre pour l'implémentation. D'où un découpage entre les tests et le code de production.

## Conclusion
Une bonne couverture de code ne doit pas cacher les défaillances de nos tests. Le couplage entre les tests et leurs implémentations  nous empêche de faire du refactoring, c’est-à-dire modifier le code de production sans changer le comportement ni les tests.
C’est pour cela, il est important que dans  de refactoring  (3ème étape du cycle de TDD) de ne pas se limiter sur l’implémentation mais aussi les tests pour mettre une base de tests évolutive,  flexible et indépendante des choix de l'implémentation.
