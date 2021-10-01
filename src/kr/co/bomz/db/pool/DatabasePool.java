package kr.co.bomz.db.pool;

import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 데이터베이스 커넥션 풀<p>
 * 사용 예)<br>
 * <code>
 * 		DatabasePool pool = new DatabasePool(driver, url, id, pw);<br>
 * 		pool.setValidationQuery("SELECT 1");<br>
 * 		ResultSet rs = pool.selectQueryToStatement("SELECT NAME, AGE, ADDR FROM USER");<br>
 * </code>
 * <p>
 * selectQueryTo... 메소드를 통해 얻은 ResultSet 의 경우 일정 시간이 지나면 데이터베이스 커넥션 풀에서
 * 자동으로 종료시켜주지만 좀 더 효율적인 메모리관리를 위해서 사용자가 직접 종료시켜주기를 권장합니다<p>
 * 또한 지속적인 데이터베이스 연결을 위해 setValidationQuery(String query) 메소드에 부하가 적은 테스트쿼리를
 * 설정하여주면 좀 더 좋은 성능을 발휘할 수 있습니다<p> 
 * 
 * @author Bomz
 * @version 1.0
 * @since 1.0
 *
 */
public class DatabasePool {
	
	private final Logger logger = LoggerFactory.getLogger(DatabasePool.class);
	
	
	/**	
	 * 모든 커넥션이 사용 중일 경우 사용할 수 있는 커넥션이 반환될 때 까지 대기 시간 (ms)
	 */
	private long returnConnectionWaitTime = 5000;
	
	/**
	 * 사용된 java.sql.ResultSet 객체의 자동 자원반납 처리 시간
	 * 기본 값 = 15초
	 */
	private static final long AUTO_CLOSE_RESULTSET_TIME = 15000L;
	
	/**		커넥션 최소 연결 수		*/
	private int minConnectionQuantity = 5;
	/**		커넥션 최대 연결 수		*/
	private int maxConnectionQuantity = 15;
	
	/**
	 * 사용 대기 중인 커넥션 관리 큐
	 */
	private Queue<DatabaseConnection> connectionQueue = new java.util.concurrent.LinkedBlockingDeque<DatabaseConnection>();
	
	/**
	 * 데이터베이스 트랜잭션 관리 맵
	 * KEY : Thread.currentThread.getId()
	 * VALUE : 아이디에 대한 커넥션 연결 객체
	 */
	private Map<Long, DatabaseConnection> transactionMap = new HashMap<Long, DatabaseConnection>();
	
	/**		데이터베이스 접속 URL		*/
	private String databaseUrl;
	/**		데이터베이스 접속 아이디		*/
	private String databaseId;
	/**		데이터베이스 접속 암호		*/
	private String databasePassword;
	
	/**		데이터베이스 커넥션 관리 아이디 생성기		*/
	private final IdGenerator idGenerator = new IdGenerator();
	
	/**		설정값에 따른 여러 처리를 담당하는 DBCP 매니저		*/
	private final DatabasePoolManager manager = new DatabasePoolManager(this);
	
	/**		지속적인 데이터베이스 접속 유지를 위한 쿼리 설정		*/
	private String validationQuery = null;
	
	/**		validationQuery 검사 주기 (기본값 : 1시간)		*/
	private long validationQueryTimeout = 3600000L;

	/**
	 * 데이터베이스 커넥션 풀
	 * 
	 * @param driverClass		데이터베이스 드라이버 클래스
	 * @param url					데이터베이스 접속 URL
	 * @param id					데이터베이스 아이디
	 * @param pw					데이터베이스 암호
	 * @throws DatabasePropertyException		드라이버클래스를 찾을 수 없을 경우 발생
	 * @throws NullPointerException					파라메터 값 중 null 이 존재할 경우 발생
	 * @throws DatabaseConnectException		데이터베이스 연결 실패 시 발생
	 */
	public DatabasePool(String driverClass, String url, String id, String pw) throws SQLException{
		if( driverClass == null || driverClass.trim().equals("") ){
			if( this.logger.isDebugEnabled())		this.logger.debug("database driverClass is null");
			throw new NullPointerException("driverClass is null");
		}
		
		if( url == null || url.trim().equals("") ){
			if( this.logger.isDebugEnabled())		this.logger.debug("database URL is null");
			throw new NullPointerException("url is null");
		}
		if( id == null || id.trim().equals("") ){
			if( this.logger.isDebugEnabled())		this.logger.debug("database ID is null");
			throw new NullPointerException("id is null");
		}
		if( pw == null || pw.trim().equals("") ){
			if( this.logger.isDebugEnabled())		this.logger.debug("database password is null"); 
			throw new NullPointerException("password is null");
		}
		
		try {
			Class.forName(driverClass.trim());
		} catch (ClassNotFoundException e) {
			if( this.logger.isDebugEnabled())		this.logger.debug("driverclass value is {}", driverClass, e);
			throw new DatabasePropertyException("driverClass value is " + driverClass);
		}
		
		this.databaseUrl = url.trim();
		this.databaseId = id.trim();
		this.databasePassword = pw.trim();
		
		this.init();		// 커넥션 초기화 실행
	}
	
	/**
	 * 데이터베이스 풀 초기화 작업<br>
	 * 해당 메소드는 생략 가능하며, 미리 데이터베이스에 최소 접속 수만큼 접속 후 해당 풀을 사용하고 싶을 때 사용된다
	 * 
	 * @throws SQLException							데이터베이스 연결 후 쿼리 전송 오류 시 발생
	 * @throws DatabasePropertyException		DB URL, 아이디, 암호가 설정되지 않았을 경우 발생
	 * @throws DatabaseConnectException		데이터베이스 연결 실패 시 발생
	 */
	private void init() throws SQLException, DatabasePropertyException, DatabaseConnectException{
		for(int i=0; i < this.minConnectionQuantity; i++){
			if( this.connectionQueue.size() >= this.minConnectionQuantity )		break;
			
			try{
				this.returnConnection( this.newConnection() );
			}catch(DatabaseConnectException e){
				throw e;
			}catch(DatabasePropertyException e){
				throw e;
			}catch(SQLException e){
				// 커넥션 오류시 초기화 종료
				throw e;
			}
		}
	}
		
	/**
	 * 데이터베이서 접속 유지를 위한 쿼리 설정
	 * @param validationQuery
	 */
	public void setValidationQuery(String validationQuery){
		this.validationQuery = validationQuery;
		if( this.logger.isDebugEnabled())		this.logger.debug("setting validation query [{}]", this.validationQuery);
	}
	
	/**
	 * 커넥션 최소 연결 수와 최대 연결 수 설정
	 * @param minConnectionQuantity		최소 값 1
	 * @param maxConnectionQuantity		minConnectionQuantity 보다 작을 경우 minConnectionQuantity + 1
	 */
	public void setConnectionQuantity(int minConnectionQuantity, int maxConnectionQuantity) {
		if( minConnectionQuantity <= 0 )		minConnectionQuantity = this.minConnectionQuantity;
		if( maxConnectionQuantity <= minConnectionQuantity )	maxConnectionQuantity = minConnectionQuantity + 1;
		
		this.minConnectionQuantity = minConnectionQuantity;
		this.maxConnectionQuantity = maxConnectionQuantity;
		
		if( this.logger.isDebugEnabled())		this.logger.debug("setting connection quantity [min:{}, max:{}]", this.minConnectionQuantity, this.maxConnectionQuantity);
	}

	/**
	 * 유효성 검사 쿼리 검사 주기<br>
	 * 기본값 : 1시간
	 * 최소값 : 10분
	 * @param validationQueryTimeout 단위:분 (예:10분=10)
	 */
	public void setValidationQueryTimeout(long validationQueryTimeout) {
		if( validationQueryTimeout <= 0 )		return;
		if( validationQueryTimeout < 10 )		validationQueryTimeout = 10;
		this.validationQueryTimeout = validationQueryTimeout * 60000;
		if( this.logger.isDebugEnabled())		this.logger.debug("setting validation query timeout [{} minute]", validationQueryTimeout);
	}
	
	/**		데이터베이스 커넥션 반납		*/
	private void returnConnection(DatabaseConnection dc){
		if( dc == null )		return;
		
		if( this.idGenerator.getNowId() == dc.getId() ){
			// 삭제하지 않는 아이디 값일 경우
			
			if( dc.isStartTransaction() )		this.transactionMap.put(Thread.currentThread().getId(), dc);
			else											this.connectionQueue.offer(dc);
		}
//		else{
//			삭제해야 하는 아이디 값일 경우
//			별도의 처리가 필요 없으며 DbcpManager 에서 자동 종료 시킨다	
//		}
	}
	
	/**
	 * DatabasePoolManager 에서 호출한다</br>
	 * 관리중인 커넥션 중 일정 시간이 지난 커넥션은 validationQuery 를 통해 유효성 확인
	 */
	void checkConnectionValidation(){
		// 현재 시간에서 검사대기 시간을 제외한다
		long checkTime = System.currentTimeMillis() - this.validationQueryTimeout;
		CheckType checkType = null;
		
		// connectionQueue 에 보관중인 커넥션 검사
		int size = this.connectionQueue.size();
		DatabaseConnection dc;
		
		for(int i=0; i < size; i++){
			dc = this.connectionQueue.poll();
			checkType = this.checkConnectionValidation( checkTime, dc, true );
			
			if( checkType == CheckType.SUCCESS ) 	this.returnConnection(dc);		// 검사 완료 커넥션은 반납
			else if( checkType == CheckType.FAIL )		break;
			else  	 dc.close();		// return is CheckType.PASS
		}
		
		// false 가 설정되었을 경우는 연결이 해제되었을 경우임
		if( checkType != null && checkType == CheckType.FAIL )		return;
		
		// transactionMap 에 보관중인 커넥션 검사
		try{		// 스레드에 의한 처리이기때문에 Map 에서 동시 처리시 오류날 가망성이 있음
			java.util.Iterator<DatabaseConnection> connections = this.transactionMap.values().iterator();
			while( connections.hasNext() ){
				// fail 리턴 시 연결이 해제되었을 경우임
				if( this.checkConnectionValidation( checkTime, connections.next(), false ) == CheckType.FAIL)		break;
			}
		}catch(Exception e){}		// 특별한 예외 처리는 없음
	}
	
	/**		
	 * 		주기적으로 연결된 커넥션의 마지막 사용 시간을 확인 후 일정시간이상
	 * 		지났을 경우 validationQuery를 수행하여 연결 유지 관리를 수행
	 * @param checkTime		현재 시간 - validationQueryTimeout
	 * @param dc					커넥션 정보
	 * @param pass				검사 패스 여부
	 * @return						오류 발생 시 false
	 */
	private CheckType checkConnectionValidation(long checkTime, DatabaseConnection dc, boolean pass){
		if( dc == null )		return CheckType.SUCCESS;
		
		// 마지막 사용 시간이 검사대기 시간을 지나지 않았을 경우
		if( checkTime <= dc.getLastCallTime() )		return CheckType.SUCCESS;
		
		try{
			if( pass && (this.minConnectionQuantity < this.transactionMap.size() + this.connectionQueue.size())){
				// 현재 관리중인 커넥션 수가 최소 연결 수보다 많을 경우 해당 커넥션만 종료를 위해 PASS 리턴
				return CheckType.PASS;
			}
			this.testValidationQuery(dc);
			return CheckType.SUCCESS;
		}catch(SQLException e){
			// 에러가 났을 경우 연결 종료 처리
//			this.connectionQueue.offer(dc);		// 다시 큐에 넣지 않는다. DatabasePoolManager 가 정상종료시킴
			this.disconnectDatabase();
			return CheckType.FAIL;
		}
	}
	
	/**		데이터베이스 커넥션 요청		*/
	private DatabaseConnection requestConnection() throws SQLException, DatabaseConnectException{
		
		// 사용중인 트랜잭션 커넥션 리턴
		DatabaseConnection resultConn = this.transactionMap.remove(Thread.currentThread().getId());
		if( resultConn != null )		return resultConn;
				
		// 공동 사용 커넥션 리턴
		while( true ){
			resultConn = this.connectionQueue.poll();
			if( resultConn == null )		break;		// 새로 접속이나 사용 중인 커넥션 반납 대기
			if( resultConn.getId() == this.idGenerator.getNowId() ){
				// 연결 종료 커넥션 객체가 아닐 경우
				return resultConn;
			}
			// 연결 종료 커넥션으로 등록되어있는 경우 poll() 한 데이터를 버리고 새로 루프 순환
		}
		
		// 현재 연결 커넥션 수가 최대치를 넘지 않았다면 새로 접속하여 리턴
		if( this.transactionMap.size() + this.connectionQueue.size() < this.maxConnectionQuantity )
			return this.newConnection();		// 여기서 예외 발생 가능
				
		// 최대치까지 커넥션 접속중이라면 지정된 시간 동안 대기 후 반환된 커넥션 리턴
		resultConn = this.waitToReturnConnection();
		if( resultConn != null )		return resultConn; 
		
		// 사용할 수 있는 커넥션이 없을 경우 예외 발생
		throw new NotConnectionCanUseException();
	}
	
	
	/**		사용할 수 있는 커넥션이 반환될 때까지 일정시간동안 대기하면서 반환 커넥션 확인		*/
	private DatabaseConnection waitToReturnConnection(){
		int repeatNumber = (int)this.returnConnectionWaitTime / 100;
		
		DatabaseConnection result;
		
		for(int i=0; i < repeatNumber; i++){
			try{		Thread.sleep(100);		}catch(Exception e){}
			result = this.connectionQueue.poll();
			if( result != null )		return result;
		}
		
		return null;
	}
	
	/**		
	 * 새로운 데이터베이스 커넥션 생성
	 * 
	 * @return	데이터베이스 커넥션 객체
	 * @throws SQLException	데이터베이스 연결 실패 시 발생
	 * @throws DatabasePropertyException		데이터베이스 접속 정보가 설정되지 않았을 경우 발생
	 */
	private DatabaseConnection newConnection() throws SQLException, DatabasePropertyException, DatabaseConnectException{
		// 설정 값 검사
		if( this.databaseUrl == null || this.databaseUrl.equals("") )		throw new DatabasePropertyException("데이터베이스 접속 URL 값이 설정되지 않았습니다");
		if( this.databaseId == null || this.databaseId.equals("") )		throw new DatabasePropertyException("데이터베이스 접속 아이디 값이 설정되지 않았습니다");
		if( this.databasePassword == null || this.databasePassword.equals("") )		throw new DatabasePropertyException("데이터베이스 접속 암호 값이 설정되지 않았습니다");

		Connection conn = null;
		try{
			conn = DriverManager.getConnection(this.databaseUrl, this.databaseId, this.databasePassword);
		}catch(SQLException e){
			throw new DatabaseConnectException();
		}
		
		try{
			DatabaseConnection result = new DatabaseConnection(this.idGenerator.getNowId(), conn);
			this.testValidationQuery(result);	// 접속 유지 쿼리 실행
			
			this.manager.addCloseEvent(result);		// 연결해제시 이벤트 처리할 수 있도록 매니저에 등록
			
			return result;
		}catch(SQLException e){
			// 이 부분에서 예외가 발생했을 경우 데이터베이스 접속이 종료된걸로 간주한다
			this.disconnectDatabase();
			if( conn != null )		try{	conn.close();		}catch(Exception e1){}
			throw e;
		}
	}
	
	/**		새로운 접속 시 테스트 쿼리 실행		*/
	private void testValidationQuery(DatabaseConnection dc) throws SQLException{
		if( this.validationQuery == null )		return;
		
		Statement st = null;
		ResultSet rs = null;
		try{
			st = dc.getStatement(null, StatementType.STATEMENT);
			rs = st.executeQuery(this.validationQuery);
		}catch(SQLException e){
			throw new SQLException("Validation Query : " + this.validationQuery, e);
		}finally{
			if( rs != null ){		try{		rs.close();		}catch(Exception e){}	}
			if( st != null ){		try{		st.close();		}catch(Exception e){}	}
		}
	}
	
	/**		데이터베이스 연결 종료		*/
	private void disconnectDatabase(){
		
		long id = this.idGenerator.getNowId();
		
		// 연결 종료시킬 아이디를 매니저에 등록
		this.manager.runCloseEvent(id);
		
		// 관리 아이디 값 변경
		this.idGenerator.next();
	}
	
	/**
	 * 트랜잭션 시작<br>
	 * 해당 메소드를 호출했으면 반드시 commit() 또는 rollback() 을 호출해야 함
	 * @throws SQLException 커넥션을 가져올 수 없거나 트랜잭션 시작 중 발생 가능
	 */
	public void startTransaction() throws SQLException{
		DatabaseConnection dc = this.requestConnection();
		dc.startTransaction();
		this.transactionMap.put(Thread.currentThread().getId(), dc);
	}
	
	/**
	 * 데이터베이스 커밋
	 * @throws NonTransactionException		트랜잭션이 선언되지 않은 상태에서 호출 시 발생
	 * @throws SQLException						데이터베이스 처리 중 오류 
	 */
	public void commit() throws NonTransactionException, SQLException{
		DatabaseConnection dc = this.requestConnection();
		dc.commit();
		this.returnConnection(dc);
	}
	
	/**
	 * 데이터베이스 롤백
	 * @throws NonTransactionException		트랜잭션이 선언되지 않은 상태에서 호출 시 발생
	 * @throws SQLException						데이터베이스 처리 중 오류 
	 */
	public void rollback() throws NonTransactionException, SQLException{
		DatabaseConnection dc = this.requestConnection();
		dc.rollback();
		this.returnConnection(dc);
	}
	
	/**
	 * java.sql.Statement 를 이용한 UPDATE / INSERT / DELETE 쿼리 수행
	 * @param sql		쿼리
	 * @return			실행성공 로우 수
	 * @throws SQLException
	 */
	public int queryToStatement(String sql) throws SQLException{
		return (Integer)this.executeQuery(StatementType.STATEMENT, false, sql);
	}
	
	/**
	 * java.sql.PraparedStatement 를 이용한 UPDATE / INSERT / DELETE 쿼리 수행
	 * @param sql			쿼리
	 * @param param		쿼리 설정 파라메터
	 * @return				실행성공 로우 수
	 * @throws SQLException
	 */
	public int queryToPreparedStatement(String sql, Object ... param) throws SQLException{
		return (Integer)this.executeQuery(StatementType.PREPARED_STATEMENT, false, sql, param);
	}
	
	/**
	 * java.sql.CallableStatement 를 이용한 UPDATE / INSERT / DELETE 쿼리 수행
	 * @param sql			쿼리
	 * @param param		쿼리 설정 파라메터
	 * @return				실행성공 로우 수
	 * @throws SQLException
	 */
	public int queryToCallableStatement(String sql, Object ... param) throws SQLException{
		return (Integer)this.executeQuery(StatementType.CALLABLE_STATEMENT, false, sql, param);
	}
	
	/**
	 * java.sql.Statement 를 이용한 셀렉트 쿼리 수행
	 * @param sql			쿼리
	 * @return				ResutSet
	 * @throws SQLException
	 */
	public ResultSet selectQueryToStatement(String sql) throws SQLException{
		return this.selectQuery(StatementType.STATEMENT, sql);
	}

	/**
	 * java.sql.PreparedStatement 를 이용한 셀렉트 쿼리 수행
	 * @param sql			쿼리
	 * @param param		쿼리 설정 파라메터
	 * @return				ResutSet
	 * @throws SQLException
	 */
	public ResultSet selectQueryToPreparedStatement(String sql, Object ... param) throws SQLException{
		return this.selectQuery(StatementType.PREPARED_STATEMENT, sql, param);
	}
	
	/**
	 * java.sql.CallableStatement 를 이용한 셀렉트 쿼리 수행
	 * @param sql			쿼리
	 * @param param		쿼리 설정 파라메터
	 * @return				ResutSet
	 * @throws SQLException
	 */	
	public ResultSet selectQueryToCallableStatement(String sql, Object ... param) throws SQLException{
		return this.selectQuery(StatementType.CALLABLE_STATEMENT, sql, param);
	}
	
	/**		셀렉트 쿼리 수행		*/
	private ResultSet selectQuery(StatementType sType, String sql, Object ... param) throws SQLException{
		try{
			return ((AutoCloseResult)this.executeQuery(sType, true, sql, param)).getResultSet();
		}catch(DatabaseDisconnectException e){
			// 연결 종료 오류 시 한번 더 시도하여 재접속 처리 후 검색쿼리를 수행할 수 있게 함
			return ((AutoCloseResult)this.executeQuery(sType, true, sql, param)).getResultSet();
		}
	}
	
	/**		쿼리 수행		*/
	private Object executeQuery(StatementType sType, boolean select, String sql, Object ... param) throws SQLException{
		DatabaseConnection dc = this.requestConnection();

		Statement st = dc.getStatement(sql, sType);
		
		Object result;
		
		try{
			if( sType == StatementType.STATEMENT ){
				// statement
				if( select )		result = new AutoCloseResult(st, st.executeQuery(sql), System.currentTimeMillis() + AUTO_CLOSE_RESULTSET_TIME);		// SELECT
				else				result = st.executeUpdate(sql);	// INSERT, UPDATE, DELETE
			}else{
				// preparedStatement or callableStatement
				PreparedStatement pst = (PreparedStatement)st;
				// 파라메터 셋팅
				this.settingParameter(pst, param);
				// 쿼리 수행
				if( select )		result = new AutoCloseResult(st, pst.executeQuery(), System.currentTimeMillis() + AUTO_CLOSE_RESULTSET_TIME);		// SELECT
				else				result = pst.executeUpdate();		// INSERT, UPDATE, DELETE
			}
			
			/*
			 * 사용자가 resultSet.close() , st.close() 를 하지 않을 경우 메모리가 무한 증가하므로
			 * 자동으로 close() 를 호출하기 위해 매니저에 등록시킨다
			 */
			if( select )		this.manager.addAutoCloseResult((AutoCloseResult)result);
			
			return result;
		}catch(SQLException e){
			// 예외 발생 시 쿼리 오류인지 접속종료 오류인지 확인한다
			try{		st.close();		}catch(Exception e1){}		// 오류 났을 경우 자원반납
			if( this.checkConnectionClosed(dc) )		throw new DatabaseDisconnectException();		// 정상적으로 연결이 끊겼을 경우 처리
			else			throw e;		// 다른 오류로 인한 경우
		}finally{
			if( !select )		try{		st.close();		}catch(Exception e1){}		// ResultSet 리턴이 아닐 경우 자원반납
			this.returnConnection(dc);			// 커넥션 반납
		}
	}
	
	/**		데이터베이스 접속 종료 검사		*/
	private boolean checkConnectionClosed(DatabaseConnection dc){
		try{
			if( !dc.isClosed() )		return false;
			
			// 데이터베이스 연결이 끊겼을 경우
			this.disconnectDatabase();
			
			return true;
		}catch(Exception e){
			// 에러가 날 경우에도 연결이 끊긴걸로 처리
			this.disconnectDatabase();
			return false;
		}
	}
	
	/**
	 * 파라메터 타입에 맞는 setMethod를 호출하여 설정값 대입
	 * @param pst			데이터베이스 객체
	 * @param param		쿼리에 사용될 파라메터
	 * @throws SQLException		setMethod 호출 시 발생 가능
	 */
	private void settingParameter(PreparedStatement pst, Object ... param) throws SQLException{
		int length = param.length;
		for(int i=0; i < length; i++){
			if( param[i] == null )							pst.setString(i+1, null);
			else if( param[i] instanceof String )	pst.setString(i+1, (String)param[i]);
			else if( param[i] instanceof Integer)	pst.setInt(i+1, (Integer)param[i]);
			else if( param[i] instanceof Boolean)	pst.setBoolean(i+1,  (Boolean)param[i]);
			else if( param[i] instanceof Float)	pst.setFloat(i+1,  (Float)param[i]);
			else if( param[i] instanceof Long)		pst.setLong(i+1,  (Long)param[i]);
			else if( param[i] instanceof Double)	pst.setDouble(i+1,  (Double)param[i]);
			else if( param[i] instanceof Short)	pst.setShort(i+1,  (Short)param[i]);
			else if( param[i] instanceof Byte)		pst.setByte(i+1,  (Byte)param[i]);
			else if( param[i] instanceof Byte[])	pst.setBytes(i+1,  (byte[])param[i]);
			else if( param[i] instanceof Date)		pst.setDate(i+1,  (Date)param[i]);
			else if( param[i] instanceof Time)		pst.setTime(i+1,  (Time)param[i]);
			else if( param[i] instanceof Timestamp)		pst.setTimestamp(i+1,  (Timestamp)param[i]);
			else if( param[i] instanceof Object)	pst.setObject(i+1,  param[i]);
			else if( param[i] instanceof URL)		pst.setURL(i+1,  (URL)param[i]);
			else if( param[i] instanceof SQLXML)		pst.setSQLXML(i+1,  (SQLXML)param[i]);
			else if( param[i] instanceof Array)	pst.setArray(i+1,  (Array)param[i]);
			else if( param[i] instanceof BigDecimal)		pst.setBigDecimal(i+1,  (BigDecimal)param[i]);
			else if( param[i] instanceof Blob)		pst.setBlob(i+1,  (Blob)param[i]);
			else if( param[i] instanceof Clob)		pst.setClob(i+1,  (Clob)param[i]);
			else if( param[i] instanceof Ref)		pst.setRef(i+1,  (Ref)param[i]);
			else if( param[i] instanceof RowId)	pst.setRowId(i+1,  (RowId)param[i]);
			else if( param[i] instanceof NClob)	pst.setNClob(i+1,  (NClob)param[i]);
			else													pst.setObject(i+1, param[i]);
		}
	}
			
}
