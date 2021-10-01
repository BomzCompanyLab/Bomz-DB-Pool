package kr.co.bomz.db.pool;

import java.sql.SQLException;

/**
 * 	데이터베이스 접속 실패 시 발생
 * 
 * @author Bomz
 * @version 1.0
 * @since 1.0
 *
 */
public class DatabaseConnectException extends SQLException{

	private static final long serialVersionUID = -7649410773513995247L;

	public DatabaseConnectException(){}
}
