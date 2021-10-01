package kr.co.bomz.db.pool;

/**
 * 아이디 정보를 생성 관리
 * 
 * @author Bomz
 * @since 1.0
 * @version 1.0
 *
 */
public class IdGenerator {

	private long id = System.currentTimeMillis();
	
	long next(){
		return ++this.id;
	}
	
	long getNowId(){
		return this.id;
	}
	
	long getNextId(){
		return this.id + 1;
	}
}
