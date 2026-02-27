package Swiggy;

import java.util.*;

interface OrderObserver {
    void onOrderStatusUpdate(Order order);
}
class Person {
    String name;
    Person(String name) {
        this.name = name;
    }
}
class Customer extends Person implements OrderObserver{
    Customer(String name) {
        super(name);
    }

    @Override
    public void onOrderStatusUpdate(Order order) {
        System.out.println("Customer " + name + " received order status " + order.orderStatus);
    }
}
interface DeliveryPartnerInterface {
    void collectOrder(Order order);
    void deliverOrder(Order order);
    void acceptOrder(Order order);
    void getOrderRequest(Order order);
}
class DeliveryPartner extends Person implements OrderObserver, DeliveryPartnerInterface{
    Set<Order>activeOrder = new HashSet<>();

    DeliveryPartner(String name) {
        super(name);
    }

    @Override
    public void onOrderStatusUpdate(Order order) {
        System.out.println("Delivery Partner " + name + " received order status " + order.orderStatus);
    }

    @Override
    public void collectOrder(Order order) {
        System.out.println("DeliveryPartner " + name + " collected order : " + order.id);
        order.updateOrderStatus(OrderStatus.OUT_FOR_DELIVERY);
    }

    @Override
    public void deliverOrder(Order order) {
        System.out.println("DeliveryPartner " + name + " delivered order : " + order.id);
        order.updateOrderStatus(OrderStatus.DELIVERED);
        activeOrder.remove(order);
    }

    @Override
    public void acceptOrder(Order order) {
        if (!order.assignDeliveryPartner(this)) return;
        System.out.println("DeliveryPartner " + name + " is on your way to collect order : " + order.id);
        activeOrder.add(order);
        order.addObserver(this);
    }

    @Override
    public void getOrderRequest(Order order) {
        System.out.println("DeliveryPartner " + name + " requested to collect order : " + order.id);
    }
}

enum Location {
    HYD,
    PNQ,
    IXR
}
interface RestaurantInterface {
    void acceptOrder(Order order);
    // void rejectOrder(Order order);
}
class Restaurant implements OrderObserver, RestaurantInterface{
    String name;
    Location location;
    // metadata
    Menu menu ;
    List<Order> activeOrder = new ArrayList<>();

    @Override
    public void onOrderStatusUpdate(Order order) {
        System.out.println("Restaurant " + name + " Received order status : " + order.id + " : " + order.orderStatus);
    }

    @Override
    public void acceptOrder(Order order) {
        System.out.println("Restaurant " + name + " accepted order and is preparing it : " + order.id);
        order.updateOrderStatus(OrderStatus.PREPARING);
        order.addObserver(this);
    }
}
class Menu {
    Map<FoodItem, Double> foodItemDoubleMap;

    public Menu(Map<FoodItem, Double> foodItemDoubleMap) {
        this.foodItemDoubleMap = foodItemDoubleMap;
    }
}
class FoodItem {
    String name;
    double amount;
    // rating
    public FoodItem(String name, double amount) {
        this.name = name;
        this.amount = amount;
    }
}
interface RestaurantManager {
    void addRestaurant(Restaurant restaurant);
}
class RestaurantManagerImpl implements RestaurantManager{
    Map<Location, List<Restaurant>> restaurants;
    @Override
    public void addRestaurant(Restaurant restaurant) {
        this.restaurants.computeIfAbsent(restaurant.location,
                k-> new ArrayList<>()).add(restaurant);
    }
}

enum OrderStatus {
    PENDING,
    CONFIRMED,
    SENT_TO_RESTAURANT,
    PREPARING,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED
}
interface OrderObservable {
    void addObserver(OrderObserver orderObserver);
}
class Order implements OrderObservable{
    int id;
    Restaurant restaurant;
    Map<FoodItem, Integer>foodItemIntegerMap;
    double amount;
    OrderStatus orderStatus;
    Payment payment;
    List<OrderObserver>orderObservers = new ArrayList<>();
    String deliveryAddress;
    DeliveryPartner deliveryPartner;

    @Override
    public void addObserver(OrderObserver orderObserver) {
        orderObservers.add(orderObserver);
    }

    void updateOrderStatus(OrderStatus orderStatus) {
        this.orderStatus = orderStatus;
        for (OrderObserver orderObserver : orderObservers) {
            orderObserver.onOrderStatusUpdate(this);
        }
    }

    synchronized boolean assignDeliveryPartner (DeliveryPartner deliveryPartner) {
        if (this.deliveryPartner == null) {
            this.deliveryPartner = deliveryPartner;
            return true;
        }
        return false;
    }

}

// Payment
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

        if (paymentStrategy != null && paymentStrategy.makePayment(paymentDetail)) {
            paymentDetail.paymentStatus = PaymentStatus.PAID;
            return paymentDetail;
        }

        System.out.println("Payment Failed");
        return null;
    }
}
class Payment {
    Customer user;
    double amount;
    PaymentType paymentType;
    PaymentStatus paymentStatus;

    public Payment(Customer user, double amount, PaymentType paymentType) {
        this.user = user;
        this.amount = amount;
        this.paymentStatus = PaymentStatus.UNPAID;
        this.paymentType = paymentType;
    }
}
class UPIPaymentDetail extends Payment {
    String upiId;

    public UPIPaymentDetail(Customer user, double amount, PaymentType paymentType, String upiId) {
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
        if (paymentType.equals(PaymentType.UPI)) {
            return new UPIPaymentStrategy();
        }
        // Easy to extend for CARD or NETBANKING here
        return null;
    }
}

// Pricing
interface PricingStrategy {
    double calculatePrice(double amount);
}
class RainSurgePricingStrategy implements PricingStrategy{

    @Override
    public double calculatePrice(double amount) {
        return amount + 100;
    }
}
class DefaultSurgePricingStrategy implements PricingStrategy{

    @Override
    public double calculatePrice(double amount) {
        return amount;
    }
}

interface OrderService {
    Order createOrder(Restaurant restaurant, Map<FoodItem, Integer>foodItemIntegerMap,
                      Customer customer, String address, PricingStrategy pricingStrategy);
    Order confirmOrder(Order order) throws InterruptedException;
}
class OrderServiceImpl implements OrderService {

    @Override
    public Order createOrder(Restaurant restaurant, Map<FoodItem, Integer> foodItemIntegerMap,
                             Customer customer, String address, PricingStrategy pricingStrategy) {
        Order order = new Order();
        order.restaurant = restaurant;
        order.deliveryAddress = address;
        double totalAmount = foodItemIntegerMap.entrySet().stream()
                .mapToDouble(entry -> entry.getKey().amount * entry.getValue())
                .sum();
        order.amount = pricingStrategy.calculatePrice(totalAmount);
        order.id = 123;
        order.updateOrderStatus(OrderStatus.PENDING);
        order.addObserver(customer);
        System.out.println("Order placed ");
        return order;
    }

    @Override
    public Order confirmOrder(Order order) throws InterruptedException {
        Payment payment = order.payment;
        if (payment.paymentStatus == PaymentStatus.PAID) {
            order.updateOrderStatus(OrderStatus.CONFIRMED);
            System.out.println("Order confirmed. Sending to restaurant...");
            Thread.sleep(1000);
            order.updateOrderStatus(OrderStatus.SENT_TO_RESTAURANT);
            return order;
        }
        order.updateOrderStatus(OrderStatus.CANCELLED);
        System.out.println("Order cancelled. try again!");
        return order;
    }
}

interface DeliveryPartnerFindingStrategy {
    List<DeliveryPartner> findDeliveryPartner(List<DeliveryPartner> deliveryPartners);
}
class DeliveryPartnerFindingStrategyImpl implements DeliveryPartnerFindingStrategy {

    @Override
    public List<DeliveryPartner> findDeliveryPartner(List<DeliveryPartner> deliveryPartners) {
        return deliveryPartners;
    }
}
interface DeliveryPartnerService {
    void addDeliveryPartner(DeliveryPartner deliveryPartner);
    void sendOrderDeliveryRequest(Order order, List<DeliveryPartner> deliveryPartners);
    List<DeliveryPartner> findDeliveryPartner(Order order, DeliveryPartnerFindingStrategy deliveryPartnerFindingStrategy);
}
class DeliveryPartnerSeviceImpl implements  DeliveryPartnerService{
    List<DeliveryPartner> deliveryPartners = new ArrayList<>();

    @Override
    public void addDeliveryPartner(DeliveryPartner deliveryPartner) {
        deliveryPartners.add(deliveryPartner);
    }

    @Override
    public void sendOrderDeliveryRequest(Order order, List<DeliveryPartner> deliveryPartners) {
        for (DeliveryPartner deliveryPartner : deliveryPartners) {
            if (order.deliveryPartner != null) return;
            deliveryPartner.getOrderRequest(order);
        }
    }

    @Override
    public List<DeliveryPartner> findDeliveryPartner(Order order, DeliveryPartnerFindingStrategy deliveryPartnerFindingStrategy) {
        return deliveryPartnerFindingStrategy.findDeliveryPartner(deliveryPartners);
    }
}

public class SwiggyLLD {

    public static void main(String[] args) throws InterruptedException {
        Restaurant restaurant = new Restaurant();
        restaurant.name = "CHAAYOS";
        FoodItem dhokla = new FoodItem("dhokla", 340.0);
        Customer customer = new Customer("Prerna")  ;
        DeliveryPartner deliveryPartner = new DeliveryPartner("Sanu");
        DeliveryPartnerService deliveryPartnerService = new DeliveryPartnerSeviceImpl();
        OrderService orderService = new OrderServiceImpl();
        Map<FoodItem, Integer> map = new HashMap<>();
        map.put(dhokla, 4);
        Order order = orderService.createOrder(restaurant, map, customer, "ghar" , new RainSurgePricingStrategy());
        PaymentManager paymentManager = new PaymentManagerImpl();
        order.payment = new UPIPaymentDetail(customer, order.amount, PaymentType.UPI, "prerna.oksbi");
        paymentManager.makePayment(order.payment);
        orderService.confirmOrder(order);
        deliveryPartnerService.addDeliveryPartner(deliveryPartner);
        deliveryPartnerService.sendOrderDeliveryRequest(order,
                deliveryPartnerService.findDeliveryPartner(order, new DeliveryPartnerFindingStrategyImpl()));
        deliveryPartner.acceptOrder(order);
        restaurant.acceptOrder(order);
        deliveryPartner.collectOrder(order);
        deliveryPartner.deliverOrder(order);

    }
}
