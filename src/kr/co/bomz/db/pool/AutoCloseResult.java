package kr.co.bomz.db.pool;

import java.sql.ResultSet;
import java.sql.Statement;

/**
 * 
 * java.sql.ResultSet 자원반납을 하지않아 생기는 메모리 증가를 막기위한 클레스 
 * 
 * @author Bomz
 * @version 1.0
 * @since 1.0
 *
 */
public class AutoCloseResult {

	private Statement statement;
	
	private ResultSet resultSet;
	
	private long autoCloseTime;
	
	/**
	 * 지정한 시간이 지난 후 자동으로 자원을 반납시킨다
	 * @param statement				Statement
	 * @param resultSet					ResultSet
	 * @param autoCloseTime		자동 종료 시간 (단위:ms)
	 */
	AutoCloseResult(Statement statement, ResultSet resultSet, long autoCloseTime){
		this.statement = statement;
		this.resultSet = resultSet;
		this.autoCloseTime = autoCloseTime;
	}
	
	/**
	 * 자원 자동 종료
	 */
	void closeResult(){
		if( this.resultSet != null ){
			try{		this.resultSet.close();		}catch(Exception e){}
			this.resultSet = null;
		}
		
		if( this.statement != null ){
			try{		this.statement.close();		}catch(Exception e){}
			this.statement = null;
		}
	}

	/**
	 * 자원 자동 종료 시간
	 * @return		 자원 자동 종료 시간 : 단위(ms)
	 */
	public long getAutoCloseTime() {
		return autoCloseTime;
	}

	/**
	 * 자원 반납 할 ResultSet
	 * @return		자원 반납 할 ResultSet
	 */
	public ResultSet getResultSet() {
		return resultSet;
	}
	
}
