package com.crm.repositories;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.crm.entities.Order;
import com.crm.entities.OrderItem;
import com.crm.entities.User;
import com.crm.models.OrderDaily;
import com.crm.models.OrderInfo;
import com.crm.models.OrderStatistics;
import com.crm.models.OrderUsersDetail;

@Repository
@Transactional
public class OrderRepository {

	@Autowired
	SessionFactory sessionFactory;

	/**
	 * adds new order record as well as attached orderItem records into the
	 * database.
	 * 
	 * @param order  entity that has to be saved in database
	 * @param carsId list of cars which was picked in the order
	 */
	@Transactional(rollbackFor = Exception.class)
	public void addOrder(Order order, List<Long> carsId) {

		Session session = this.sessionFactory.getCurrentSession();
		Long orderId = (Long) session.save(order);

		for (Long carId : carsId) {
			OrderItem item = new OrderItem(orderId, carId);
			session.save(item);
		}
	}

	/**
	 * this method updates status of certain order
	 * 
	 * @param id     identifier of entity to update
	 * @param status entity's parameter that has to be updated
	 */
	@Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
	public void updateOrderStatus(Long id, String status) {
		Session session = this.sessionFactory.getCurrentSession();
		Order order = session.get(Order.class, id);
		order.setStatus(status);
	}

	/**
	 * this method retrievs order entity from the database
	 * 
	 * @param id identifier of entity to retrieve
	 * @return Order entity
	 */
	public Order getOrder(Long id) {
		Session session = this.sessionFactory.getCurrentSession();
		return session.get(Order.class, id);

	}

	/**
	 * retrieves all orders from database without fetching OrderItem
	 * 
	 * @return List of Order entities
	 */
	public List<Order> getOrders() {
		List<Order> orderList = null;

		Session session = this.sessionFactory.getCurrentSession();
		String sql = "from " + Order.class.getName();
		Query<Order> query = session.createQuery(sql, Order.class);
		orderList = query.getResultList();

		return orderList;
	}

	/**
	 * pulls out all orders from database which was made by certain user
	 * 
	 * @param userId incoming parameter to filter out
	 * @return List of Order entities
	 */
	public List<Order> getOrdersByUser(Long userId) {
		List<Order> orderList = null;

		String sql = "SELECT e FROM " + Order.class.getName() + " e WHERE e.userId =:" + Order.ATTRIBUTE_USER_ID;
		Session session = this.sessionFactory.getCurrentSession();
		Query<Order> query = session.createQuery(sql, Order.class);
		query.setParameter(Order.ATTRIBUTE_USER_ID, userId);
		orderList = query.getResultList();


		return orderList;
	}

	/**
	 * pulls out short data about order from database as OrderUserDetail class
	 * 
	 * @return List of OrderUserDetail representation class
	 */
	public List<OrderUsersDetail> getOrdersDetails() {
		List<OrderUsersDetail> ordDetList = null;

		String sql = "Select new " + OrderUsersDetail.class.getName() + "(a.id, b.name, a.bill)" + " from "
				+ Order.class.getName() + " a inner join " + User.class.getName() + " b ON a.userId=b.id ";
		Session session = this.sessionFactory.getCurrentSession();
		Query<OrderUsersDetail> query = session.createQuery(sql, OrderUsersDetail.class);
		ordDetList = query.getResultList();

		return ordDetList;
	}

	/**
	 * pulls out statistic about orders that was made up by users. Such as total
	 * amount of money spend, user's id and user's name.
	 * 
	 * @return List of OrderStatistics POJO representation class
	 */
	public List<OrderStatistics> getOrdersStatisticByUsers() {
		List<OrderStatistics> ordStat = null;

		String sql = "SELECT NEW " + OrderStatistics.class.getName() + " (b.id, b.name, SUM(a.bill)) " + " FROM "
				+ Order.class.getName() + " a INNER JOIN " + User.class.getName() + " b ON a.userId=b.id "
				+ "GROUP BY b.id";
		Session session = this.sessionFactory.getCurrentSession();
		Query<OrderStatistics> query = session.createQuery(sql, OrderStatistics.class);
		ordStat = query.getResultList();

		return ordStat;
	}

	/**
	 * pulls out from database recursive statistic - order's id, user's id, bill of
	 * the order and average price of orders made up by user . This method uses
	 * window sql function to pull out required data. Requires modifications before
	 * using in the "production"!
	 */
	public void getCompToAvg() {

		String sql = "SELECT id, userId, bill, AVG(bill) " + " OVER (PARTITION BY userId) FROM car_rental.orders";
		Session session = this.sessionFactory.getCurrentSession();

		@SuppressWarnings("unchecked")

		List<Object[]> oList = session.createNativeQuery(sql).getResultList();
		for (Object[] objects : oList) {
			for (int i = 0; i < objects.length; i++) {
				System.out.print(objects[i].toString() + " | ");
			}
			System.out.println("");
		}
	}

	/**
	 * retrieves statistic how many orders and how much money was gained on each day
	 * 
	 * @return list of OrderDaily class entities
	 */
	public List<OrderDaily> getOrderReports() {

		String sql = "SELECT new " + OrderDaily.class.getName() + " (SUM(a.bill), COUNT(a.id),a.start_date) " + "FROM "
				+ Order.class.getName() + " a GROUP BY a.start_date ORDER BY a.start_date DESC";
		Session session = this.sessionFactory.getCurrentSession();
		Query<OrderDaily> query = session.createQuery(sql, OrderDaily.class);

		return query.getResultList();
	}

	/**
	 * Retrieves short info about orders from the database as OrderInfo
	 * representation class. This method uses SqlResultSetMaping from entity to POJO
	 * class.
	 * 
	 * @return List of OrderInfo objects
	 */
	public List<OrderInfo> getOrdersInfo() {
		// Mapping native query result to POJO model class
		String sql = "Select id, userId, bill FROM car_rental.orders ";
		Session session = this.sessionFactory.getCurrentSession();

		@SuppressWarnings("unchecked")
		Query<OrderInfo> query = session.createNativeQuery(sql, "OrderInfoMapping");
		List<OrderInfo> stats = query.getResultList();

		return stats;
	}
}
