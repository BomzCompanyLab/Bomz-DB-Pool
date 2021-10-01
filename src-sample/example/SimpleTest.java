package example;

import java.sql.ResultSet;

import kr.co.bomz.db.pool.DatabasePool;

/*
 * DatabaseConnectionPooling Simple Test
 * 
 * 1. insert
 * 2. select
 * 3. delete
 *
 */
public class SimpleTest {

	public static void main(String[] args) throws Exception{
		DatabasePool pool = new DatabasePool("org.mariadb.jdbc.Driver", "jdbc:mariadb://localhost:3306/exam", "root", "1234");
	
		// test data insert
		pool.queryToPreparedStatement("INSERT INTO USER VALUES (?, ?)", "apple", 17);
		pool.queryToPreparedStatement("INSERT INTO USER VALUES (?, ?)", "kiwi", 41);
		pool.queryToPreparedStatement("INSERT INTO USER VALUES (?, ?)", "banana", 36);
		
		// insert data select
		ResultSet rs = null;
		try{
			rs = pool.selectQueryToStatement("SELECT NAME, AGE FROM USER");
			while( rs.next() ){
				System.out.println("name=" + rs.getString(1) + " , age=" + rs.getInt(2));
			}
		}catch(Exception e){
			/*
			 * ResultSet is auto closed
			 * However, I recommend that you close the ResultSet directly 
			 */
			if( rs != null )		rs.close();
		}
		
		// delete data
		pool.queryToStatement("DELETE FROM USER");
	}

}
