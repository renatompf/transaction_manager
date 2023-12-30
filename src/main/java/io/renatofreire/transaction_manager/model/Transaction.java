package io.renatofreire.transaction_manager.model;


import io.renatofreire.transaction_manager.enums.Currencies;
import jakarta.persistence.*;

import java.time.ZonedDateTime;

@Entity
@Table(name = "transaction_logs")
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "from_account_id")
    private BankAccount fromAccount;

    @ManyToOne
    @JoinColumn(name = "to_account_id")
    private BankAccount toAccount;

    @Column(name = "from_currency")
    private Currencies fromCurrency;

    @Column(name = "to_currency")
    private Currencies toCurrency;

    @Column(name = "original_amount")
    private Double amount;

    @Column(name = "exchange_rate")
    private Double exchangeRate;

    @Column(name = "timestamp")
    private ZonedDateTime timestamp;

    public Transaction () {
    }

    public Transaction(BankAccount fromAccount, BankAccount toAccount, Double amount, Double exchangeRate, ZonedDateTime timestamp) {
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.fromCurrency = fromAccount.getCurrency();
        this.toCurrency = toAccount.getCurrency();
        this.amount = amount;
        this.exchangeRate = exchangeRate;
        this.timestamp = timestamp;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BankAccount getFromAccount() {
        return fromAccount;
    }

    public void setFromAccount(BankAccount fromAccount) {
        this.fromAccount = fromAccount;
    }

    public BankAccount getToAccount() {
        return toAccount;
    }

    public void setToAccount(BankAccount toAccount) {
        this.toAccount = toAccount;
    }

    public Currencies getFromCurrency() {
        return fromCurrency;
    }

    public void setFromCurrency(Currencies fromCurrency) {
        this.fromCurrency = fromCurrency;
    }

    public Currencies getToCurrency() {
        return toCurrency;
    }

    public void setToCurrency(Currencies toCurrency) {
        this.toCurrency = toCurrency;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public Double getExchangeRate() {
        return exchangeRate;
    }

    public void setExchangeRate(Double exchangeRate) {
        this.exchangeRate = exchangeRate;
    }

    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(ZonedDateTime timestamp) {
        this.timestamp = timestamp;
    }
}