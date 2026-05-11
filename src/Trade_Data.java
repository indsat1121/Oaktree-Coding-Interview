import java.time.LocalDate;

public class Trade_Data {

    private String trade_id;
    private String symbol;
    private String side;
    private double quantity;
    private double price;
    private LocalDate trade_date;
    private LocalDate settlement_date;
    private String account_id;

    public Trade_Data() {
    }

    public String getTrade_id() {
        return trade_id;
    }

    public void setTrade_id(String trade_id) {
        this.trade_id = trade_id;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getSide() {
        return side;
    }

    public void setSide(String side) {
        this.side = side;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public LocalDate getTrade_date() {
        return trade_date;
    }

    public void setTrade_date(LocalDate trade_date) {
        this.trade_date = trade_date;
    }

    public LocalDate getSettlement_date() {
        return settlement_date;
    }

    public void setSettlement_date(LocalDate settlement_date) {
        this.settlement_date = settlement_date;
    }

    public String getAccount_id() {
        return account_id;
    }

    public void setAccount_id(String account_id) {
        this.account_id = account_id;
    }
}
