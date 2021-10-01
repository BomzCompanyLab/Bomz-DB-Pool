package kr.co.bomz.db.pool;

import java.sql.SQLException;

/**
 * 트랜잭션이 시작되지 않았는데 커밋이나 롤백작업을 요청하였을 경우 발생
 * 
 * @author Bomz
 * @since 1.0
 * @version 1.0
 *
 */
public class NonTransactionException extends SQLException{

	private static final long serialVersionUID = -8342462174629627707L;

	public NonTransactionException(){}
}
