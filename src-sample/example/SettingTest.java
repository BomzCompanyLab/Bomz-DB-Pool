package example;

import kr.co.bomz.db.pool.DatabasePool;

public class SettingTest {

	public static void main(String[] args) throws Exception{
		DatabasePool pool = new DatabasePool("org.mariadb.jdbc.Driver", "jdbc:mariadb://localhost:3306/exam", "root", "1234");
		// Default value is null
		pool.setValidationQuery("SELECT 1");
		// Default value is 5, 15 (min, max)
		pool.setConnectionQuantity(15, 30);
		// Default value is 60. minimum value is 10. (unit:minute)
		pool.setValidationQueryTimeout(15);
	}

}
