# FlexBid Backend 🚀

FlexBid is a real-time Auction and reverse bidding E-commerce platform built using **Spring Boot**.  
The system combines an auction marketplace and service marketplace where users can buy products through competitive bidding and hire service providers through reverse bidding.

This backend provides secure APIs, real-time bid updates, payment processing, user management, and complete marketplace workflows.

---

## 🌟 Key Features

## 👥 Role Based System

FlexBid supports three major roles:

### 🛒 Buyer

- Register and verify account using OTP email verification
- Browse auction products
- Participate in live product bidding
- Place competitive bids
- Win auctions
- Complete payments
- Manage orders
- Post service requirements
- Select service providers through reverse bidding

---

### 🏪 Seller

- Register and manage seller profile
- Create product auction listings
- Manage products
- Receive live bids from buyers
- Sell products to winning bidders
- View orders

Service Module:

- View buyer service requests
- Participate in reverse bidding
- Offer competitive lower prices
- Win service contracts
- Complete assigned services

---

### 🛡️ Admin

- Manage buyers and sellers
- Monitor platform activities
- Control bidding operations
- Manage auction timing
- Manage listed products
- Oversee transactions

---

# 🔥 Product Auction System

Traditional bidding workflow:

Seller creates product auction

```
Seller
   |
   ↓
Lists Product
   |
   ↓
Buyers Place Live Bids
   |
   ↓
Highest Bid Wins
   |
   ↓
Payment
   |
   ↓
Order Completed
```

Features:

- Product listing
- Auction start/end timing
- Real-time bidding
- Highest bid tracking
- Winner selection
- Order generation

---

# 🔄 Reverse Service Bidding

Unique service marketplace feature.

Buyer posts service requirement:

Example:

```
Need website development
Budget: ₹20,000
```

Sellers/service providers bid lower:

```
Seller A → ₹18000

Seller B → ₹15000

Seller C → ₹12000
```

Lowest suitable bidder wins the service.

Features:

- Service requirement posting
- Seller bidding
- Reverse price competition
- Winner selection
- Payment processing

---

# ⚡ Real-Time Communication

Implemented using:

- Spring WebSocket
- STOMP Messaging

Used for:

✔ Live product bidding  
✔ Instant bid updates  
✔ Auction notifications  

---

# 💳 Payment Integration

Integrated Razorpay Payment Gateway

Features:

- Payment order creation
- Secure transactions
- Payment verification
- Order processing after payment success

---

# 📧 Email System

Gmail SMTP Integration

Features:

- OTP generation
- Email verification
- User account verification

---

# 🛠️ Tech Stack

## Backend

- Java 17
- Spring Boot
- Spring MVC
- Spring Security
- Spring Data JPA
- Hibernate
- WebSocket
- Maven

## Database

- MySQL

## External Services

- Razorpay Payment Gateway
- Gmail SMTP

## API Testing

- Swagger UI
- Postman

---

# 📁 Backend Architecture


src/main/java/com/example/flexbid

```
├── controller
│   ├── UserController
│   ├── BuyerController
│   ├── SellerPortfolioController
│   ├── ProductController
│   ├── BidController
│   ├── ReverseBidController
│   ├── OrderController
│   ├── PaymentController
│   └── BiddingTimeController
│
├── service
│   ├── UserService
│   ├── BuyerService
│   ├── ProductService
│   ├── BidService
│   ├── ReverseBidService
│   ├── OrderService
│   ├── EmailService
│   └── WebSocketNotificationService
│
├── repository
│
├── model
│
├── dto
│
└── configuration
```

---

# ⚙️ Environment Variables

Create environment variables:

```env
DB_URL=

DB_USERNAME=

DB_PASSWORD=


MAIL_USERNAME=

MAIL_PASSWORD=


RAZORPAY_KEY_ID=

RAZORPAY_KEY_SECRET=
```

---

# ▶️ Run Locally

Clone repository

```bash
git clone https://github.com/Appu3115/FlexBid-Backend.git
```

Go inside project

```bash
cd FlexBid-Backend
```

Install dependencies

```bash
mvn clean install
```

Run application

```bash
mvn spring-boot:run
```

Backend starts:

```
http://localhost:8080
```

---

# 📚 API Documentation

Swagger UI:

```
http://localhost:8080/swagger-ui.html
```

---

# 🔒 Security Practices

- Environment based configuration
- Credentials hidden using environment variables
- Sensitive files ignored from Git
- Role based access handling

---

# 🚀 Future Improvements

- JWT Authentication
- Docker Deployment
- Cloud Hosting
- Notification System
- Review & Rating System
- Advanced Search Filters

---

# 👨‍💻 Developer

**Appu M**

Java Full Stack Developer

GitHub: https://github.com/Appu3115

LinkedIn: https://linkedin.com/in/appu-m
