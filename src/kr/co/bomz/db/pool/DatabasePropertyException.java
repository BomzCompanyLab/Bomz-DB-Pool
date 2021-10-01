package kr.co.bomz.db.pool;

import java.sql.SQLException;

/**
 * �����ͺ��̽� �������� �߸� �Ǿ��� ��� �߻�
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
