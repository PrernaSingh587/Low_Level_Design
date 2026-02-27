package BookMyShow;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

class User {
    String name;
    List<Ticket>ticketHistory;
}

class Movie {
    String name;
}
class Theatre {
    List<Screen>screenList = new ArrayList<>();
    Map<LocalDate, List<Show>> showList = new HashMap<>();
    String name;
    Location location;

    public Theatre(List<Screen> screenList, String name, Location location) {
        this.screenList = screenList;
        this.name = name;
        this.location = location;
    }
    void addShow(LocalDate localDate, Show show) {
        List<Show> shows = showList.get(localDate);

        if (shows == null) {
            shows = new ArrayList<>();
            showList.put(localDate, shows);
        }
        shows.add(show);
    }
    List<Show> getShowByMovieNameAndDate(LocalDate localDate, Movie movie) {
        return showList.get(localDate);
    }
}
enum Location {
    HYD,
    IXR
}

class Seat {
    String seatNumber;
}
interface SeatBooking {
    boolean bookSeat();
    void vacateSeat();
    boolean isLockExpired();
    boolean isAvailable();
}
class ShowSeat implements SeatBooking {
    boolean isAvailable = true;
    Seat seat;
    LocalDateTime lockAcquireTime;

    ShowSeat(Seat seat) {
        this.seat = seat;
    }
    @Override
    public synchronized boolean bookSeat() {
        if (!isAvailable || !isLockExpired()) {
            System.out.println("Sorry the seat " + seat.seatNumber + " has already been taken");
            return false;
        }
        isAvailable = false;
        lockAcquireTime = LocalDateTime.now();
        System.out.println("Locked Seat " + seat.seatNumber);
        return true;
    }

    @Override
    public void vacateSeat() {
        isAvailable = true;
    }

    @Override
    public boolean isLockExpired() {
        LocalDateTime currentTime = LocalDateTime.now();
        boolean flag = Duration.between(lockAcquireTime,currentTime).toMinutes() > 5;
        if (flag) {
            this.lockAcquireTime = null;
            return true;
        }
        return false;
    }

    @Override
    public boolean isAvailable() {
        return this.isAvailable || isLockExpired();
    }

    public String getSeatNumber() {
        return this.seat.seatNumber;
    }
}
class Show {
    Movie movie;
    Screen screen;
    LocalDateTime startTime;
    int durationInMinutes;
    List<SeatBooking> showSeats;

    public List<SeatBooking> showAvailableSeatMap() {
        return showSeats;
    }
}
class Screen {
    List<Seat>seatList;
    Theatre theatre;
}

//Payment
enum PaymentStatus {
    PAID,
    UNPAID,
    REFUNDED,
    FAILED
}
enum PaymentType {
    UPI,
    CARD,
    NETBANKING
}
interface PaymentManager {
    Payment makePayment(Payment paymentDetail);
}
class PaymentManagerImpl implements PaymentManager {
    PaymentFactory paymentFactory = new PaymentFactoryImpl();
    @Override
    public Payment makePayment(Payment paymentDetail) {
       PaymentStrategy paymentStrategy = paymentFactory.getPaymentStrategy(paymentDetail.paymentType);
       if(paymentStrategy.makePayment(paymentDetail)) {
           paymentDetail.paymentStatus = PaymentStatus.PAID;
           return paymentDetail;
       }
       System.out.println("Payment Failed");
       return null;
    }
}
class Payment {
    User user;
    double amount;
    PaymentType paymentType;
    PaymentStatus paymentStatus;
    public Payment(User user, double amount, PaymentType paymentType) {
        this.user = user;
        this.amount = amount;
        this.paymentType = paymentType;
        this.paymentStatus = PaymentStatus.UNPAID;
    }
}
class UPIPaymentDetail extends Payment {
    String upiId;
    public UPIPaymentDetail(User user, double amount, PaymentType paymentType, String upiId) {
        super(user, amount, paymentType);
        this.upiId = upiId;
    }
}
interface PaymentStrategy {
    boolean makePayment(Payment payment);
}
class UPIPaymentStrategy implements PaymentStrategy {

    @Override
    public boolean makePayment(Payment payment) {
        if (payment instanceof UPIPaymentDetail) {
            UPIPaymentDetail upiPaymentDetail = (UPIPaymentDetail) payment;
            System.out.println("PAID " + payment.amount + " through UPI " + upiPaymentDetail.upiId);
            return true;
        }
        System.out.println("INVALID PAYMENT OBJECT FOR UPI");
        return false;
    }
}
interface PaymentFactory {
    PaymentStrategy getPaymentStrategy(PaymentType paymentType);
}
class PaymentFactoryImpl implements PaymentFactory {

    @Override
    public PaymentStrategy getPaymentStrategy(PaymentType paymentType) {
        if(paymentType.equals(PaymentType.UPI)) return new UPIPaymentStrategy();
        return null;
    }
}

enum Status {
    CANCELLED,
    BOOKED,
    PENDING
}
class Ticket {
    Show show;
    List<SeatBooking> showSeat;
    User user;
    Status status;
    Payment payment;

    public Ticket(Show show, List<SeatBooking> showSeat, User user) {
        this.show = show;
        this.showSeat = showSeat;
        this.user = user;
        this.status = Status.PENDING;
    }
}

interface BookingService {
    Ticket bookTicket(Show show, List<SeatBooking> showSeats, User user);
    boolean cancelTicket(Ticket ticket);
    Ticket confirmBooking(Ticket ticket);
}
class BookingServiceImpl implements BookingService {

    @Override
    public Ticket bookTicket(Show show, List<SeatBooking> showSeats, User user) {
        // bookSeat - make sure partially booked seats are not there
        List<SeatBooking> lockedSeats = new ArrayList<>();

        for (SeatBooking seat : showSeats) {
            if (!seat.bookSeat()) {
                for (SeatBooking locked : lockedSeats) {
                    locked.vacateSeat();
                }
                return null;
            }
            lockedSeats.add(seat);
        }

        System.out.println("Pending Ticket");
        return new Ticket(show, showSeats, user);
    }

    @Override
    public boolean cancelTicket(Ticket ticket) {
        ticket.status = Status.CANCELLED;
        List<SeatBooking> showSeats = ticket.showSeat;
        for (SeatBooking seatBooking : showSeats) {
            seatBooking.vacateSeat();
        }
        System.out.println("Ticket cancelled");
        return true;
    }

    @Override
    public Ticket confirmBooking(Ticket ticket) {
        PaymentStatus paymentStatus = ticket.payment.paymentStatus;
        if (paymentStatus == PaymentStatus.PAID) {
            ticket.status = Status.BOOKED;
            return ticket;
        }
        ticket.status = Status.CANCELLED;
        System.out.println("Booking Confirmation Failed. Retry Booking.");
        return ticket;
    }
}

interface BookMyShow {
   // List<Show> getShows();
    Ticket bookShowSeat(Show show, List<SeatBooking>showSeat, User user);
    Ticket makePayment(Ticket ticket);
    Ticket confirmBooking(Ticket ticket);
    void cancelBooking(Ticket ticket);
}

public class BookMyShowLLD implements BookMyShow{
    PaymentManager paymentManager = new PaymentManagerImpl();
    BookingService bookingService = new BookingServiceImpl();

    @Override
    public Ticket bookShowSeat(Show show, List<SeatBooking> showSeat, User user) {
        return bookingService.bookTicket(show, showSeat, user);
    }

    @Override
    public Ticket makePayment(Ticket ticket) {
        Payment payment = paymentManager.makePayment(ticket.payment);
        if (payment == null) {
            cancelBooking(ticket);
            System.out.println("Ticket Cancelled");
        } else {
            System.out.println("Ticket CONFIRMED");
            confirmBooking(ticket);
        }
        return ticket;

    }

    @Override
    public Ticket confirmBooking(Ticket ticket) {
        return bookingService.confirmBooking(ticket);
    }

    @Override
    public void cancelBooking(Ticket ticket) {
        bookingService.cancelTicket(ticket);
    }

}
