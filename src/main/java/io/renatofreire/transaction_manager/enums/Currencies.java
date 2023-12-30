package io.renatofreire.transaction_manager.enums;

public enum Currencies {
    USD("United States Dollar"),
    EUR("Euro"),
    GBP("British Pound Sterling"),
    JPY("Japanese Yen"),
    AUD("Australian Dollar"),
    CAD("Canadian Dollar"),
    CNY("Chinese Yuan"),
    INR("Indian Rupee"),
    CHF("Swiss Franc"),
    SEK("Swedish Krona"),
    NZD("New Zealand Dollar"),
    KRW("South Korean Won"),
    SGD("Singapore Dollar"),
    TRY("Turkish Lira"),
    RUB("Russian Ruble"),
    ZAR("South African Rand"),
    BRL("Brazilian Real"),
    KPW("North Korean Won")
    ;
    private final String fullName;

    private static final Currencies[] VALUES = values();

    Currencies(String fullName) {
        this.fullName = fullName;
    }

    public static Currencies currencyExists(String currency){
        for(Currencies currencies : VALUES){
            if(currencies.name().equals(currency)){
                return currencies;
            }
        }
        return null;
    }

    public String getFullName() {
        return fullName;
    }
}