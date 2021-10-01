package kr.co.bomz.db.pool;

import java.sql.SQLException;

/**
 * 
 * 데이터베이스 접속 종료 시 발생
 * 
 * @author Bomz
 * @version 1.0
 * @since 1.0
 *
 */
public class DatabaseDisconnectException extends SQLException{

	private static final long serialVersionUID = -5568562199422598691L;

	public DatabaseDisconnectException(){}
}
