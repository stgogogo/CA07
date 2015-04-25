package pkg.trader;

import java.util.ArrayList;

import pkg.exception.StockMarketExpection;
import pkg.market.Market;
import pkg.order.BuyOrder;
import pkg.order.Order;
import pkg.order.OrderType;
import pkg.order.SellOrder;
import pkg.util.OrderUtility;

public class Trader {
	String name;
	double cashInHand;
	// Stocks owned by the trader
	ArrayList<Order> position;
	ArrayList<Order> ordersPlaced;

	public Trader(String name, double cashInHand) {
		super();
		this.name = name;
		this.cashInHand = cashInHand;
		this.position = new ArrayList<Order>();
		this.ordersPlaced = new ArrayList<Order>();
	}

	public void buyFromBank(Market m, String symbol, int volume)
			throws StockMarketExpection {
		Order order = null;
		double price = m.getStockForSymbol(symbol).getPrice();
		order = createBuyOrder(symbol, volume, price);
		position.add(order);
		cashInHand -= volume * price;

	}

	public void placeNewOrder(Market m, String symbol, int volume,
			double price, OrderType orderType) throws StockMarketExpection {
		Order order = null;
		if (orderType == OrderType.BUY) {
			order = createBuyOrder(symbol, volume, price);
			checkExistence(symbol, orderType, order);
		} else if (orderType == OrderType.SELL) {
			order = new SellOrder(symbol, volume, price, this);
			checkExistence(symbol, orderType, order);
			checkForSellOrders(symbol, orderType, order);
		} else
			throw new StockMarketExpection("Unknown order type for stock: "
					+ symbol);
		ordersPlaced.add(order);
		m.addOrder(order);
	}

	private Order createBuyOrder(String symbol, int volume, double price)
			throws StockMarketExpection {
		Order order;
		if (volume * price > cashInHand) {
			throw new StockMarketExpection("Cannot place order for stock: "
					+ symbol + " since there is not enough money. Trader: "
					+ name);
		}
		order = new BuyOrder(symbol, volume, price, this);
		return order;
	}

	private void checkExistence(String symbol, OrderType orderType, Order order)
			throws StockMarketExpection {
		if (OrderUtility.isAlreadyPresent(ordersPlaced, order)) {
			throw new StockMarketExpection(orderType + " Order for stock: " + symbol
					+ " already in place. Trader: " + name);
		}
	}

	public void placeNewMarketOrder(Market m, String symbol, int volume,
			double price, OrderType orderType) throws StockMarketExpection {
		double marketPrice = m.getStockForSymbol(symbol).getPrice();
		Order order = null;
		if (orderType == OrderType.BUY) {
			order = createMarketBuyOrder(symbol, volume, marketPrice);
			checkExistence(symbol, orderType, order);
		} else if (orderType == OrderType.SELL) {
			order = new SellOrder(symbol, volume, true, this);
			checkExistence(symbol, orderType, order);
			
			checkForSellOrders(symbol, orderType, order);
		} else
			throw new StockMarketExpection(
					"Unknown market order type for stock: " + symbol);
		ordersPlaced.add(order);
		m.addOrder(order);
	}

	private void checkForSellOrders(String symbol, OrderType orderType,
			Order order) throws StockMarketExpection {
		if (!OrderUtility.owns(position, symbol)) {
			throw new StockMarketExpection(
					orderType + " Order for stock: "
							+ symbol
							+ " that the owner does not have. No short sale is allowed. Trader: "
							+ name);
		}
		if (OrderUtility.ownedQuantity(position, symbol) < order.getSize()) {
			throw new StockMarketExpection(orderType + " Order for stock: "
					+ symbol + " that the owner does not have. Owns: "
					+ OrderUtility.ownedQuantity(position, symbol)
					+ " stocks, but attempted to sell: " + order.getSize()
					+ " stocks. Trader: " + name);
		}
	}

	private Order createMarketBuyOrder(String symbol, int volume,
			double marketPrice) throws StockMarketExpection {
		Order order;
		if (marketPrice * volume > cashInHand) {
			throw new StockMarketExpection(
					"Cannot place  order for stock: " + symbol
							+ " since there is not enough money. Trader: "
							+ name);
		}
		order = new BuyOrder(symbol, volume, true, this);
		return order;
	}

	public void tradePerformed(Order o, double matchPrice)
			throws StockMarketExpection {
		if (!ordersPlaced.contains(o))
			throw new StockMarketExpection(
					"Unknown trade performed notification by for stock: "
							+ o.getStockSymbol());
		else {
			ordersPlaced.remove(o);
			o.setPrice(matchPrice);
			if (o instanceof BuyOrder) {
				cashInHand -= matchPrice * o.getSize();
				Order alreadyOwn = OrderUtility.findAndExtractOrder(position,
						o.getStockSymbol());
				if (alreadyOwn != null) {
					alreadyOwn.setSize(alreadyOwn.getSize() + o.getSize());
					position.add(alreadyOwn);
				} else {
					position.add(o);
				}
			} else if (o instanceof SellOrder) {
				cashInHand += matchPrice * o.getSize();
				Order alreadyOwn = OrderUtility.findAndExtractOrder(position,
						o.getStockSymbol());
				if (alreadyOwn != null) {
					alreadyOwn.setSize(alreadyOwn.getSize() - o.getSize());
					if (alreadyOwn.getSize() == 0) {
						position.remove(alreadyOwn);
					} else {
						position.add(alreadyOwn);
					}

				} else {
					throw new StockMarketExpection(
							"Unknown sell trade for stock: "
									+ o.getStockSymbol());
				}

			}
		}
	}
	
	public double getCashInHand(){
		return this.cashInHand;
	}

	public void printTrader() {
		System.out.println("Trader Name: " + name);
		System.out.println("=====================");
		System.out.println("Cash: " + cashInHand);
		System.out.println("Stocks Owned: ");
		for (Order o : position) {
			o.printStockNameInOrder();
		}
		System.out.println("Stocks Desired: ");
		for (Order o : ordersPlaced) {
			o.printOrder();
		}
		System.out.println("+++++++++++++++++++++");
		System.out.println("+++++++++++++++++++++");
	}
}
