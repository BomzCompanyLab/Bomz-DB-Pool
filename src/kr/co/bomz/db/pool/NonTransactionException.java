package kr.co.bomz.db.pool;

import java.sql.SQLException;

/**
 * Ʈ������� ���۵��� �ʾҴµ� Ŀ���̳� �ѹ��۾��� ��û�Ͽ��� ��� �߻�
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
