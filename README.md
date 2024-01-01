# Transaction Manager

-------------------------

## Project Description

The project is crafted in [Java 17](https://www.java.com/en/),
leveraging the [Spring Boot Framework](https://spring.io/projects/spring-boot/),
and using [PostgreSQL](https://www.postgresql.org/) as the database.
Also uses [Docker](https://www.docker.com/) to deploy both the application and the database within a cloud environment.

To access real-time exchange rates, I chose the [ExchangeRate-API](https://www.exchangerate-api.com/docs/overview) because it offered extensive usage for free and allowed me to specify the base currency without any charges. 
To make it simpler, only the following currencies were used: 

    USD: United States Dollar
    EUR: Euro
    GBP: British Pound Sterling
    JPY: Japanese Yen
    AUD: Australian Dollar
    CAD: Canadian Dollar
    CNY: Chinese Yuan
    INR: Indian Rupee
    CHF: Swiss Franc
    SEK: Swedish Krona
    NZD: New Zealand Dollar
    KRW: South Korean Won
    SGD: Singapore Dollar
    TRY: Turkish Lira
    RUB: Russian Ruble
    ZAR: South African Rand
    BRL: Brazilian Real
    KPW: North Korean Won

**Note**: The [ExchangeRate-API](https://www.exchangerate-api.com/docs/overview) does not support the `North Korean Won (KPW)`. 
However, for testing purposes and to ensure that a _fund transfer fails when the exchange rate cannot be retrieved_, 
this currency is supported within the application. 
You can view the list of supported and unsupported currencies [here](https://www.exchangerate-api.com/docs/supported-currencies).

----------------------
## Running the Application:

### Setup Instructions

Clone the project to your local machine:
```shell
git clone https://github.com/renatompf/transaction_manager.git
```

### Running the Project

Once the project is on your device, execute the following command to run it:
```shell
make run
```
This command initiates the database, imports the necessary data, and starts the program execution. You can access the application at `localhost:8080`.

### Running Tests

At last, to run the tests you can make:
 ```shell
 make test
 ```

### Stopping the Project

In case you want to stop everything, you can do the following:
 ```shell
 make stop
 ```

----------------------
## API Endpoints Overview:

## Account Endpoints

These endpoints are dedicated to managing User Accounts. Each user account is crucial for identifying the owner of respective bank accounts.

### Create Account
- **Endpoint:** `POST /accounts`
- **Description:** Creates a new user account based on the provided request data.
- **Request Body:** [`CreateAccountRequest`](src/main/java/io/renatofreire/transaction_manager/dto/CreateAccountRequest.java)
- **Response:** [`AccountDTO`](src/main/java/io/renatofreire/transaction_manager/dto/AccountDTO.java)
- **HTTP Response Code:** 201 (Created)

**Example:**

```
POST /accounts
Content-Type: application/json

{
  "firstName": "Albert",
  "lastName": "Einstein",
  "email": "albert.einstein@example.com",
  "dateOfBirth": "1879-03-14"
}
```

### Get All Accounts
- **Endpoint:** `GET /accounts`
- **Description:** Retrieves a paginated list of all user accounts.
- **Parameters:** `Pageable` (for pagination)
- **Response:** Paginated list of [`AccountDTO`](src/main/java/io/renatofreire/transaction_manager/dto/AccountDTO.java)
- **HTTP Response Code:** 200 (OK)

**Example:**

```
GET /accounts?page=0&size=10
```

### Get Account by ID
- **Endpoint:** `GET /accounts/{id}`
- **Description:** Retrieves an user account by its ID.
- **Path Variable:** `id` (Account ID)
- **Response:** [`AccountDTO`](src/main/java/io/renatofreire/transaction_manager/dto/AccountDTO.java)
- **HTTP Response Code:** 200 (OK)
  
**Example:**

```
GET /accounts/1
```

### Delete Account by ID
- **Endpoint:** `DELETE /accounts/{id}`
- **Description:** Deletes an user account by its ID.
- **Path Variable:** `id` (Account ID)
- **Response:** `Boolean` indicating successful deletion
- **HTTP Response Code:** 200 (OK)

**Example:**

```
DELETE /accounts/1
```

## Bank Accounts Endpoints

These endpoints are dedicated to managing Bank Accounts.

### Create Bank Account
- **Endpoint:** `POST /bank-accounts`
- **Description:** Creates a new bank account based on the provided request data.
- **Request Body:** [`CreateBankAccountRequest`](src/main/java/io/renatofreire/transaction_manager/dto/CreateBankAccountRequest.java)
- **Response:** [`BankAccountDTO`](src/main/java/io/renatofreire/transaction_manager/dto/BankAccountDTO.java)
- **HTTP Response Code:** 201 (Created)

**Example:**

```
POST /bank-accounts
Content-Type: application/json

{
  "ownerId": 1, // User Account ID
  "currency": "USD",
  "balance": 300000
}
```

### Get All Bank Accounts
- **Endpoint:** `GET /bank-accounts`
- **Description:** Retrieves a paginated list of all bank accounts.
- **Parameters:** `Pageable` (for pagination)
- **Response:** Paginated list of [`BankAccountDTO`](src/main/java/io/renatofreire/transaction_manager/dto/BankAccountDTO.java)
- **HTTP Response Code:** 200 (OK)

**Example:**

```
GET /bank-accounts?page=0&size=10
```

### Get Bank Account by ID
- **Endpoint:** `GET /bank-accounts/{id}`
- **Description:** Retrieves a bank account by its ID.
- **Path Variable:** `id` (Bank Account ID)
- **Response:** [`BankAccountDTO`](src/main/java/io/renatofreire/transaction_manager/dto/BankAccountDTO.java)
- **HTTP Response Code:** 200 (OK)
  
**Example:**

```
GET /bank-accounts/1
```

### Delete Bank Account by ID
- **Endpoint:** `DELETE /bank-accounts/{id}`
- **Description:** Deletes a bank account by its ID.
- **Path Variable:** `id` (Bank Account ID)
- **Response:** `Boolean` indicating successful deletion
- **HTTP Response Code:** 200 (OK)

**Example:**

```
DELETE /bank-accounts/1
```

## Transaction Endpoints

These endpoints are dedicated to managing Transactions.

### Create New Transaction
- **Endpoint:** `POST /transactions`
- **Description:** Creates a new transaction based on the provided request data.
- **Request Body:** [`CreateNewTransactionRequest`](src/main/java/io/renatofreire/transaction_manager/dto/CreateNewTransactionRequest.java)
- **Response:** [`TransactionDTO`](src/main/java/io/renatofreire/transaction_manager/dto/TransactionDTO.java)
- **HTTP Response Code:** 201 (Created)

**Example:**
```
POST /transactions
Content-Type: application/json

{
  "from": 2, // Source Bank Account ID
  "to": 1,  // Destination Bank Account ID
  "amount": 1000 // The amount will be considered to be in the same currency as the Souce Bank Account and will be converted if needed.
}
```

(Please check [Notes, point 1](#Notes))

### Get All Transactions
- **Endpoint:** `GET /transactions`
- **Description:** Retrieves a paginated list of all transactions.
- **Parameters:** `Pageable` (for pagination)
- **Response:** Paginated list of [`TransactionDTO`](src/main/java/io/renatofreire/transaction_manager/dto/TransactionDTO.java)
- **HTTP Response Code:** 200 (OK)

**Example:**

```
GET /transactions?page=0&size=10
```

### Get Transaction by ID
- **Endpoint:** `GET /transactions/{id}`
- **Description:** Retrieves a transaction by its ID.
- **Path Variable:** `id` (Transaction ID)
- **Response:** [`TransactionDTO`](src/main/java/io/renatofreire/transaction_manager/dto/TransactionDTO.java)
- **HTTP Response Code:** 200 (OK)

**Example:**
```http
GET /transactions/1
```

----------------------
## Notes:

1. When creating a new transaction, the [ExchangeRate-API](https://www.exchangerate-api.com/docs/overview) is called to check real-time data. That can only happen by using an **API KEY**. This **API KEY** is exposed in the [application.properties](src/main/resources/application.properties) file. If for some reason, the calls made to the **ExchangeRate-API** starting failing, even for the most common currencies. Try to generate one [here](https://www.exchangerate-api.com/) and replace it in the [application.properties](src/main/resources/application.properties) file.
