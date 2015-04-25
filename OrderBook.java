package pkg.order;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeMap;

import pkg.client.Pair;
import pkg.exception.StockMarketExpection;
import pkg.market.Market;
import pkg.market.api.PriceSetter;
import pkg.util.OrderUtility;

public class OrderBook {
	Market market;
	private HashMap<String, ArrayList<Order>> buyOrders;
	private HashMap<String, ArrayList<Order>> sellOrders;

	public OrderBook(Market m) {
		this.market = m;
		setBuyOrders(new HashMap<String, ArrayList<Order>>());
		setSellOrders(new HashMap<String, ArrayList<Order>>());
	}

	public void addToOrderBook(Order order) {
		String stockSymbol = order.getStockSymbol();
		if (order instanceof BuyOrder) {
			ArrayList<Order> buyOrderList = null;
			if (buyOrders.containsKey(stockSymbol)) {
				buyOrderList = buyOrders.remove(stockSymbol);
			} else {
				buyOrderList = new ArrayList<Order>();
			}
			buyOrderList.add(order);
			buyOrders.put(stockSymbol, buyOrderList);
		} else if (order instanceof SellOrder) {
			ArrayList<Order> sellOrderList = null;
			if (sellOrders.containsKey(stockSymbol)) {
				sellOrderList = sellOrders.remove(stockSymbol);
			} else {
				sellOrderList = new ArrayList<Order>();
			}
			sellOrderList.add(order);
			sellOrders.put(stockSymbol, sellOrderList);
		}
	}

	public ArrayList<Order> sortOrderList(ArrayList<Order> sortedList,OrderType type){
		if (type==OrderType.BUY){
			Collections.sort(sortedList, new Comparator<Order>(){
				public int compare(Order order1, Order order2){
					if(order1.getPrice()>order2.getPrice()||order1.isMarketOrder()){
						return -1;
					}
					else{
						return 1;
					}
				}
			});
		}
		else if(type==OrderType.SELL){
			Collections.sort(sortedList, new Comparator<Order>(){
				public int compare(Order order1, Order order2){
					if(order1.getPrice()<order2.getPrice()||order1.isMarketOrder()){
						return -1;
					}
					else{
						return 1;
					}
				}
			});
		}
		return sortedList;
	}
	
	public void trade() {
		Set<String> stocksToBuy = getBuyOrders().keySet();

		for (String stockToBuy : stocksToBuy) {
			if (!getSellOrders().containsKey(stockToBuy)) {
				continue;
			}
			ArrayList<Order> buyOrderList = getBuyOrders().remove(stockToBuy);
			ArrayList<Order> sellOrderList = getSellOrders().remove(stockToBuy);
			ArrayList<Order> buyOrderSortedArrayList = sortOrderList(buyOrderList,OrderType.BUY);
			ArrayList<Order> sellOrderSortedArrayList = sortOrderList(sellOrderList,OrderType.SELL);
			
			TreeMap<Double, Pair<Integer, Integer>> orderCumulative = new TreeMap<Double, Pair<Integer, Integer>>();
			calculateCumulative(buyOrderSortedArrayList, sellOrderSortedArrayList, orderCumulative);
			
			Pair<Double, Integer> resultPair=findMatchPrice(orderCumulative);
			double matchPrice= (Double) resultPair.getLeft();
			int buyQuantity= (Integer) resultPair.getRight();
			int sellQuantity=buyQuantity;
			
			
			// Perform trade
			ArrayList<Order> buyOrderTempArrayList = (ArrayList<Order>) buyOrderSortedArrayList
					.clone();
			ArrayList<Order> sellOrderTempArrayList = (ArrayList<Order>) sellOrderSortedArrayList
					.clone();
			comfirmMartketPrice(stockToBuy, matchPrice);
			performBuyOrders(stockToBuy, buyOrderList,
					matchPrice, buyQuantity, buyOrderTempArrayList);
			performSellOrders(stockToBuy, sellOrderList,
					matchPrice, sellQuantity, sellOrderTempArrayList);
			
		}
	}

	private void comfirmMartketPrice(String stockToBuy, double matchPrice) {
		if (matchPrice > 0.0) {
			PriceSetter priceSetter = new PriceSetter();
			priceSetter.registerObserver(market.getMarketHistory());
			market.getMarketHistory().setSubject(priceSetter);
			priceSetter.setNewPrice(market, stockToBuy, matchPrice);
		}
	}


	private void performSellOrders(String stockToBuy,
			ArrayList<Order> sellOrderList, double matchPrice, int sellQuantity,
			ArrayList<Order> sellOrderTempArrayList) {
		for (Order sellOrder : sellOrderTempArrayList) {
			if ((sellOrder.getPrice() <= matchPrice
					|| sellOrder.isMarketOrder())&& sellQuantity>0) {
				// May have to redo this
				if (sellQuantity>sellOrder.getSize()){
					sellOrderList.remove(sellOrder);
					try {
						sellOrder.getTrader().tradePerformed(sellOrder,
								matchPrice);
					} catch (StockMarketExpection e) {
						e.printStackTrace();
					}
					sellQuantity=sellQuantity-sellOrder.getSize();
				}
				else{
					int remainSize=sellOrder.getSize()-sellQuantity;
					sellOrder.setSize(sellQuantity);
					try {
						sellOrder.getTrader().tradePerformed(sellOrder,
								matchPrice);
					} catch (StockMarketExpection e) {
						e.printStackTrace();
					}
					sellOrder.setSize(remainSize);
					sellQuantity=0;
				}
			}
		}
		sellOrders.put(stockToBuy, sellOrderList);
	}

	private void performBuyOrders(String stockToBuy,
			ArrayList<Order> buyOrderList, double matchPrice, int buyQuantity,
			ArrayList<Order> buyOrderTempArrayList) {
		for (Order buyOrder : buyOrderTempArrayList) {
			if ((buyOrder.getPrice() >= matchPrice
					|| buyOrder.isMarketOrder())&&(buyQuantity>0)) {
				// May have to redo this
				if (buyQuantity>buyOrder.getSize()){
					buyOrderList.remove(buyOrder);
					try {
						buyOrder.getTrader().tradePerformed(buyOrder,
								matchPrice);
					} catch (StockMarketExpection e) {
						e.printStackTrace();
					}
					buyQuantity=buyQuantity-buyOrder.getSize();
				}
				else{
					int remainSize=buyOrder.getSize()-buyQuantity;
					buyOrder.setSize(buyQuantity);
					try {
						buyOrder.getTrader().tradePerformed(buyOrder,
								matchPrice);
					} catch (StockMarketExpection e) {
						e.printStackTrace();
					}
					buyOrder.setSize(remainSize);
					buyQuantity=0;
				}
			}
		}
		buyOrders.put(stockToBuy, buyOrderList);
	}

	private Pair findMatchPrice(
			TreeMap<Double, Pair<Integer, Integer>> orderCumulative) {
		double matchPrice = initialzeMatchPrice();
		int matchQuantity = 0;
		for (double orderPrice : orderCumulative.keySet()) {
			Pair<Integer, Integer> orderPair = orderCumulative.get(orderPrice);
			if (orderPair.getLeft() <= orderPair.getRight()) {
				if (orderPair.getLeft() > matchQuantity) {
					matchPrice = orderPrice;
					matchQuantity = orderPair.getLeft();
				}
			} else if (orderPair.getLeft() > orderPair.getRight()) {
				if (orderPair.getRight() > matchQuantity) {
					matchPrice = orderPrice;
					matchQuantity = orderPair.getRight();
				}
			}
		}
		Pair<Double, Integer> pair=new Pair(matchPrice,matchQuantity);
		return pair;
	}

	private double initialzeMatchPrice() {
		double matchPrice = (float) 0.0;
		return matchPrice;
	}

	private void calculateCumulative(ArrayList<Order> buyOrderList,
			ArrayList<Order> sellOrderList,
			TreeMap<Double, Pair<Integer, Integer>> orderCumulative) {
		
		enterBuyCumulativeOrders(buyOrderList, orderCumulative);

		int totalSells = 0;
		for (Order sellOrder : sellOrderList) {
			totalSells += sellOrder.getSize();
			double sellPrice = sellOrder.getPrice();
			if (orderCumulative.containsKey(sellPrice)) {
				moveMarketOrder(orderCumulative, totalSells, sellPrice);
			} else {
				if (sellOrder.isMarketOrder()) {
					orderCumulative.put(sellPrice, new Pair<Integer, Integer>(
							0, totalSells));
				} else {
					Double nearestPrice = getNearestPrice(orderCumulative,
							sellPrice);
					if (nearestPrice != null) {
						Pair<Integer, Integer> cumulativePair = orderCumulative
								.remove(nearestPrice);
						orderCumulative.put(
								nearestPrice,
								new Pair<Integer, Integer>(cumulativePair
										.getLeft(), totalSells));
					} else {
						orderCumulative.put(sellPrice,
								new Pair<Integer, Integer>(0, totalSells));
					}
				}
			}
		}
	}

	private Double getNearestPrice(
			TreeMap<Double, Pair<Integer, Integer>> orderCumulative,
			double sellPrice) {
		Double nearestPrice = orderCumulative
				.ceilingKey(sellPrice);
		return nearestPrice;
	}

	private void moveMarketOrder(
			TreeMap<Double, Pair<Integer, Integer>> orderCumulative,
			int totalSells, double sellPrice) {
		Pair<Integer, Integer> cumulativePair = orderCumulative
				.remove(sellPrice);
		orderCumulative.put(sellPrice, new Pair<Integer, Integer>(
				cumulativePair.getLeft(), totalSells));
	}

	

	private void enterBuyCumulativeOrders(ArrayList<Order> buyOrderList,
			TreeMap<Double, Pair<Integer, Integer>> orderCumulative) {
		int totalBuys = 0;
		for (Order buyOrder : buyOrderList) {
			totalBuys += buyOrder.getSize();
			Pair<Integer, Integer> cumulativePair = new Pair<Integer, Integer>(
					totalBuys, 0);
			double price = buyOrder.getPrice();
			orderCumulative.put(price, cumulativePair);
		}
		
		resetBuyMarketOrder(orderCumulative);
	}

	private void resetBuyMarketOrder(
			TreeMap<Double, Pair<Integer, Integer>> orderCumulative) {
		if (orderCumulative.containsKey(0.0)) {
			Pair<Integer, Integer> cumulativePair = orderCumulative.remove(0.0);
			orderCumulative.put(maxPrice(orderCumulative.lastKey()),
					new Pair<Integer, Integer>(cumulativePair.getLeft(), 0));
		}
	}

	private Double maxPrice(Double d) {
		return (Double) (d + 1.00);
	}

	public HashMap<String, ArrayList<Order>> getSellOrders() {
		return sellOrders;
	}

	public void setSellOrders(HashMap<String, ArrayList<Order>> sellOrders) {
		this.sellOrders = sellOrders;
	}

	public HashMap<String, ArrayList<Order>> getBuyOrders() {
		return buyOrders;
	}

	public void setBuyOrders(HashMap<String, ArrayList<Order>> buyOrders) {
		this.buyOrders = buyOrders;
	}
}
