package kr.co.bomz.db.pool;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;

/**
 * 
 * 데이터베이스 커낵션 정보 관리
 * 
 * @author Bomz
 * @version 1.0
 * @since 1.0
 *
 */
public class DatabaseConnection implements CloseEvent{

	/**		커넥션 관리 아이디		*/
	private final long id;
	
	/**		데이터베이스 커넥션		*/
	private Connection conn;
	
	/**		트랜잭션 설정값		*/
	private Savepoint savepoint = null;
		
	/**		마지막 커넥션 호출 시간		*/
	private long lastCallTime = System.currentTimeMillis();
	
	/**
	 * 데이터베이스 커넥션 정보
	 * @param id			커넥션 아이디
	 * @param conn		커넥션 객체
	 */
	DatabaseConnection(long id, Connection conn){
		this.id = id;
		this.conn = conn;
	}
	
	@Override
	public void close() {
		if( this.conn == null )		return;
		
		// 강제 종료이므로 만약 트랜잭션이 설정되어 있을 경우 롤백 처리
		if( this.savepoint != null ){
			try{
				this.conn.rollback(this.savepoint);
			}catch(Exception e){}
		}
		
		// 데이터베이스 연결 해제
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
	 * 트랜잭션 시작 여부
	 * @return
	 */
	boolean isStartTransaction() {
		return this.savepoint != null;
	}
	
	/**
	 * 트랜잭션 시작
	 * @throws SQLException		트랜잭션 시작 오류
	 */
	void startTransaction() throws SQLException{
		this.conn.setAutoCommit(false);
		this.savepoint = this.conn.setSavepoint();
	}
	
	/**
	 * 데이터베이스 커밋
	 * @throws NonTransactionException		트랜잭션이 선언되지 않은 상태에서 호출 시 발생
	 * @throws SQLException						데이터베이스 처리 중 오류 
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
	 * 데이터베이스 롤백
	 * @throws NonTransactionException		트랜잭션이 선언되지 않은 상태에서 호출 시 발생
	 * @throws SQLException						데이터베이스 처리 중 오류 
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
	 * 커넥션 종료 여부
	 * @return		종료되었을 경우 true
	 * @throws SQLException		커넥션 연결여부 검사 오류 시 발생
	 */
	boolean isClosed() throws SQLException{
		return this.conn.isClosed();
	}
	
	/**
	 * 쿼리 실행
	 * @param sql		데이터베이스 쿼리
	 * @param type	Statement type
	 * @return			쿼리 실행 결과
	 * @throws SQLException
	 */
	Statement getStatement(String sql, StatementType type) throws SQLException{
		// 마지막 호출 시간 변경
		this.lastCallTime = System.currentTimeMillis();
		
		switch(type){
		case STATEMENT :						return this.conn.createStatement();
		case PREPARED_STATEMENT : return this.conn.prepareStatement(sql);
		case CALLABLE_STATEMENT :	return this.conn.prepareCall(sql);
		default :										throw new QueryTypeException(type.name());
		}
		
	}
		
	/**
	 * 마지막 호출 시간
	 * @return
	 */
	long getLastCallTime(){
		return this.lastCallTime;
	}
}
