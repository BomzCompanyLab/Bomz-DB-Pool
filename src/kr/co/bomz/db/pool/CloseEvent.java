package kr.co.bomz.db.pool;

/**
 * 데이터베이스 연결 종료 이벤트
 * 
 * @author Bomz
 * @version 1.0
 * @since 1.0
 *
 */
public interface CloseEvent {
	
	/**		고유 아이디 정보 리턴		*/
	long getId();
	
	/**		데이터베이스 연결 종료		*/
	void close();
}
