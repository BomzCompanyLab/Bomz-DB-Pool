package example;

import java.sql.ResultSet;
import java.sql.SQLException;

import kr.co.bomz.db.pool.DatabasePool;

/*
 * Transaction test
 * 
 * 1. commit test
 * 2. rollback test
 * 
 */
public class TransactionTest {

	private DatabasePool pool;
	
	public static void main(String[] args){
		TransactionTest test = new TransactionTest();
		
		if( !test.connection() ){		// database connect
			System.err.println("database connection exception");
			return;		
		}
		
		// 1. commit test
		test.commitOrRollbackTest(true);
		test.selectData();
		// 2. rollback test
		test.commitOrRollbackTest(true);
		test.selectData();
		// 3. delete data
		test.deleteData();
	}

	private boolean connection(){
		try {
			this.pool = new DatabasePool("org.mariadb.jdbc.Driver", "jdbc:mariadb://localhost:3306/exam", "root", "1234");
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	private void deleteData(){
		try {
			this.pool.queryToStatement("DELETE FROM USER");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	// select
	private void selectData(){
		ResultSet rs = null;
		try{
			rs = this.pool.selectQueryToStatement("SELECT NAME, AGE FROM USER");
			while( rs.next() ){
				System.out.println("name=" + rs.getString(1) + " , age=" + rs.getInt(2));
			}
		}catch(Exception e){
			/*
			 * ResultSet is auto closed
			 * However, I recommend that you close the ResultSet directly 
			 */
			if( rs != null )		try{	rs.close();	}catch(Exception e1){}
		}
	}
	
	// commit or rollback test
	private void commitOrRollbackTest(boolean isCommit){
		try {
			// transaction start
			this.pool.startTransaction();		
			
			// insert
			if(isCommit){
				this.pool.queryToPreparedStatement("INSERT INTO USER VALUES (?, ?)", "apple", 17);
			}else{
				this.pool.queryToPreparedStatement("INSERT INTO USER VALUES (?, ?)", "kiwi", 41);
				int a = 3/0;		// exception
				System.out.println("a=" + a);
			}
			
			// commit
			this.pool.commit();			
		} catch (Exception e) {
			try {		this.pool.rollback();		} catch (SQLException e1) {}
		}
	}
	
	
}
