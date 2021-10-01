package kr.co.bomz.db.pool;

import java.sql.ResultSet;
import java.sql.Statement;

/**
 * 
 * java.sql.ResultSet �ڿ��ݳ��� �����ʾ� ����� �޸� ������ �������� Ŭ���� 
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
	 * ������ �ð��� ���� �� �ڵ����� �ڿ��� �ݳ���Ų��
	 * @param statement				Statement
	 * @param resultSet					ResultSet
	 * @param autoCloseTime		�ڵ� ���� �ð� (����:ms)
	 */
	AutoCloseResult(Statement statement, ResultSet resultSet, long autoCloseTime){
		this.statement = statement;
		this.resultSet = resultSet;
		this.autoCloseTime = autoCloseTime;
	}
	
	/**
	 * �ڿ� �ڵ� ����
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
	 * �ڿ� �ڵ� ���� �ð�
	 * @return		 �ڿ� �ڵ� ���� �ð� : ����(ms)
	 */
	public long getAutoCloseTime() {
		return autoCloseTime;
	}

	/**
	 * �ڿ� �ݳ� �� ResultSet
	 * @return		�ڿ� �ݳ� �� ResultSet
	 */
	public ResultSet getResultSet() {
		return resultSet;
	}
	
}
