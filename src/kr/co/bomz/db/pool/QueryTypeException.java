package kr.co.bomz.db.pool;

import java.sql.SQLException;

/**
 * 정의되지 않은 QueryType 값이 사용되었을 경우 발생
 * 
 * @author Bomz
 * @version 1.0
 * @since 1.0
 *
 */
public class QueryTypeException extends SQLException{

	private static final long serialVersionUID = -5980858742103847200L;

	public QueryTypeException(String errMsg){
		super(errMsg);
	}
}
