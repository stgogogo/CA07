package pkg.util;

import java.util.ArrayList;

import pkg.order.BuyOrder;
import pkg.order.Order;
import pkg.order.SellOrder;

public class OrderUtility {
	public static boolean isAlreadyPresent(ArrayList<Order> ordersPlaced,
			Order newOrder) {
		for (Order orderPlaced : ordersPlaced) {
			if (((orderPlaced instanceof BuyOrder) && (newOrder instanceof BuyOrder))
					|| ((orderPlaced instanceof SellOrder) && (newOrder instanceof SellOrder))) {
				if (orderPlaced.getStockSymbol().equals(
						newOrder.getStockSymbol())) {
					return true;
				}
			}
		}
		return false;
	}

	// public static void insertBuyOrderIntoPosition(
	// ArrayList<Order> ordersPlaced, Order newOrder) {
	// // Add a market order
	// if (newOrder.isMarketOrder()) {
	// ordersPlaced.add(0, newOrder);
	// return;
	// }
	//
	// int position = 0;
	// for (; position < ordersPlaced.size(); position++) {
	// if (newOrder.compareTo((Order) ordersPlaced.get(position)) == 0) {
	// break;
	// } else if (newOrder.compareTo((Order) ordersPlaced.get(position)) == 1) {
	// break;
	// }
	// }
	// // Add at the end of array list
	// if (position >= ordersPlaced.size() - 1) {
	// ordersPlaced.add(newOrder);
	// } else {
	// ordersPlaced.add(position, newOrder);
	// }
	// }
	//
	// public static void insertSellOrderIntoPosition(
	// ArrayList<Order> ordersPlaced, Order newOrder) {
	// // Add a market order
	// if (newOrder.isMarketOrder()) {
	// ordersPlaced.add(0, newOrder);
	// return;
	// }
	//
	// int listPosition = 0;
	// for (; listPosition < ordersPlaced.size(); listPosition++) {
	// if (newOrder.compareTo((Order) ordersPlaced.get(listPosition)) == 0) {
	// break;
	// } else if (newOrder.compareTo((Order) ordersPlaced
	// .get(listPosition)) == -1) {
	// break;
	// }
	// }
	// // Add at the end of array list
	// if (listPosition >= ordersPlaced.size() - 1) {
	// ordersPlaced.add(newOrder);
	// } else {
	// ordersPlaced.add(listPosition, newOrder);
	// }
	// }

	public static boolean owns(ArrayList<Order> position, String symbol) {
		for (Order stock : position) {
			if (stock.getStockSymbol().equals(symbol)) {
				return true;
			}
		}
		return false;
	}

	public static Order findAndExtractOrder(ArrayList<Order> position,
			String symbol) {
		for (Order stock : position) {
			if (stock.getStockSymbol().equals(symbol)) {
				position.remove(stock);
				return stock;
			}
		}
		return null;
	}

	public static int ownedQuantity(ArrayList<Order> position, String symbol) {
		int ownedQuantity = 0;
		for (Order stock : position) {
			if (stock.getStockSymbol().equals(symbol)) {
				ownedQuantity += stock.getSize();
			}
		}
		return ownedQuantity;
	}

}
