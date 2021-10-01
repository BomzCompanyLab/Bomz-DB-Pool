package kr.co.bomz.db.pool;

import java.sql.SQLException;

/**
 * 
 * 커넥션 정보를 요청하였으나 사용할 수 있는 커넥션이 없을 경우 발생<br>
 * 해결책 : 최대 연결 수를 늘리거나 트랜잭션 사용중인 커넥션은 빠른 시간안에 커밋/롤백 시킨다
 * 
 * @author Bomz
 * @since 1.0
 * @version 1.0
 *
 */
public class NotConnectionCanUseException extends SQLException{

	private static final long serialVersionUID = 1912220674484900632L;

}
