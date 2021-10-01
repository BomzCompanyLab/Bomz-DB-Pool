package kr.co.bomz.db.pool;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;

/**
 * 
 * �����ͺ��̽� Ŀ���� ���� ����
 * 
 * @author Bomz
 * @version 1.0
 * @since 1.0
 *
 */
public class DatabaseConnection implements CloseEvent{

	/**		Ŀ�ؼ� ���� ���̵�		*/
	private final long id;
	
	/**		�����ͺ��̽� Ŀ�ؼ�		*/
	private Connection conn;
	
	/**		Ʈ����� ������		*/
	private Savepoint savepoint = null;
		
	/**		������ Ŀ�ؼ� ȣ�� �ð�		*/
	private long lastCallTime = System.currentTimeMillis();
	
	/**
	 * �����ͺ��̽� Ŀ�ؼ� ����
	 * @param id			Ŀ�ؼ� ���̵�
	 * @param conn		Ŀ�ؼ� ��ü
	 */
	DatabaseConnection(long id, Connection conn){
		this.id = id;
		this.conn = conn;
	}
	
	@Override
	public void close() {
		if( this.conn == null )		return;
		
		// ���� �����̹Ƿ� ���� Ʈ������� �����Ǿ� ���� ��� �ѹ� ó��
		if( this.savepoint != null ){
			try{
				this.conn.rollback(this.savepoint);
			}catch(Exception e){}
		}
		
		// �����ͺ��̽� ���� ����
		try{
			this.conn.close();
		}catch(Exception e){}
		
		this.savepoint = null;
	}

	@Override
	public long getId() {
		return this.id;
	}

	/**
	 * Ʈ����� ���� ����
	 * @return
	 */
	boolean isStartTransaction() {
		return this.savepoint != null;
	}
	
	/**
	 * Ʈ����� ����
	 * @throws SQLException		Ʈ����� ���� ����
	 */
	void startTransaction() throws SQLException{
		this.conn.setAutoCommit(false);
		this.savepoint = this.conn.setSavepoint();
	}
	
	/**
	 * �����ͺ��̽� Ŀ��
	 * @throws NonTransactionException		Ʈ������� ������� ���� ���¿��� ȣ�� �� �߻�
	 * @throws SQLException						�����ͺ��̽� ó�� �� ���� 
	 */
	void commit() throws NonTransactionException, SQLException{
		if( this.savepoint == null )		throw new NonTransactionException();
		try{
			this.conn.commit();
		}finally{
			this.savepoint = null;
			if( !this.conn.getAutoCommit() )
				try{		this.conn.setAutoCommit(true);	}catch(Exception e){}
		}
	}

	/**
	 * �����ͺ��̽� �ѹ�
	 * @throws NonTransactionException		Ʈ������� ������� ���� ���¿��� ȣ�� �� �߻�
	 * @throws SQLException						�����ͺ��̽� ó�� �� ���� 
	 */
	void rollback() throws NonTransactionException, SQLException{
		if( this.savepoint == null )		throw new NonTransactionException();
		try{
			this.conn.rollback(this.savepoint);
		}finally{
			this.savepoint = null;
			if( !this.conn.getAutoCommit() )
				try{		this.conn.setAutoCommit(true);	}catch(Exception e){}
		}
	}
	
	/**
	 * Ŀ�ؼ� ���� ����
	 * @return		����Ǿ��� ��� true
	 * @throws SQLException		Ŀ�ؼ� ���Ῡ�� �˻� ���� �� �߻�
	 */
	boolean isClosed() throws SQLException{
		return this.conn.isClosed();
	}
	
	/**
	 * ���� ����
	 * @param sql		�����ͺ��̽� ����
	 * @param type	Statement type
	 * @return			���� ���� ���
	 * @throws SQLException
	 */
	Statement getStatement(String sql, StatementType type) throws SQLException{
		// ������ ȣ�� �ð� ����
		this.lastCallTime = System.currentTimeMillis();
		
		switch(type){
		case STATEMENT :						return this.conn.createStatement();
		case PREPARED_STATEMENT : return this.conn.prepareStatement(sql);
		case CALLABLE_STATEMENT :	return this.conn.prepareCall(sql);
		default :										throw new QueryTypeException(type.name());
		}
		
	}
		
	/**
	 * ������ ȣ�� �ð�
	 * @return
	 */
	long getLastCallTime(){
		return this.lastCallTime;
	}
}
