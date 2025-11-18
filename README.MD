# TradeFlow - Order Service

## Description

TradeFlow is a comprehensive trading engine web application that enables users to monitor real-time stock market trends, manage portfolios, and execute trades across multiple exchanges to achieve optimal market value. It was built with a micro-service architecture using Spring boot, Angular and Kafka for communication between services.

The Order Service  validates trade requests against account balances and risk
limits, and routes market and limit orders across multiple exchanges to achieve the best execution price

## Motivation

The motivation behind TradeFlow is to empower users with a secure, intuitive, and feature-rich interface for managing trades, portfolios, and market insights. By leveraging cutting-edge technologies and best practices, the project aims to deliver reliability, scalability, and a superior user experience in the financial technology space.

## Quick Start

### Visit the Live Demo OR Skip to Installation Guide

You can access the live demo version of Tradeflow here:  
[https://tf-client-app.vercel.app](https://tf-client-app.vercel.app/)

### Installation Guide

1. **Clone the repository:**

   ```bash
   git clone https://github.com/PatrickAsiedu/tf_client_app.git
   cd tf-client-app
   ```


2. **Setup other services**
   API gateway
   [https://github.com/PatrickAsiedu/tf-api-gateway-service](https://github.com/PatrickAsiedu/tf-api-gateway-service)

   User Service
   [https://github.com/PatrickAsiedu/tf-user-service](https://github.com/PatrickAsiedu/tf-user-service)

   Order Service
   [https://github.com/PatrickAsiedu/tf-order-service](https://github.com/PatrickAsiedu/tf-order-service)

   Market Data Service
   [https://github.com/PatrickAsiedu/tf-market-data-service](https://github.com/PatrickAsiedu/tf-market-data-service)

3. **Setup Client app**
   Client app
   [https://github.com/PatrickAsiedu/tf-client-app.git](https://github.com/PatrickAsiedu/tf-client-app.git)


## Usage

- **Sign Up / Sign In:** Create an account or log in to access personalized features.
- **Marketplace:** Browse and trade products with real-time updates.
- **Portfolio Management:** Track your investments.
- **Order Management:** Place, view, and manage orders efficiently.
- **Admin Panel:** (If authorized) Access administrative tools and user management.
