package kr.co.bomz.db.pool;

import java.sql.SQLException;

/**
 * 데이터베이스 설정값이 잘못 되었을 경우 발생
 * 
 * @author Bomz
 * @since 1.0
 * @version 1.0
 *
 */
public class DatabasePropertyException extends SQLException{

	private static final long serialVersionUID = -30591127992185993L;

	public DatabasePropertyException(){
		super();
	}
	
	public DatabasePropertyException(String errMsg){
		super(errMsg);
	}
	
}
