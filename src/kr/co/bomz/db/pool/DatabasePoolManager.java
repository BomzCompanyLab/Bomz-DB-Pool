package kr.co.bomz.db.pool;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * 데이터베이스 연결 관리 및 유지 설정 관리
 * 
 * @author Bomz
 * @version 1.0
 * @since 1.0
 *
 */
public class DatabasePoolManager extends Thread{

	/**		데이터베이스 상태 관리 반복 대기 시간		*/
	private static final long CHECK_SLEEP_TIME = 1000;
	
	/**		데이터베이스 VALIDATION_QUERY 검사 요청 주기(10분)		*/
	private static final int VALIDATION_COUNT = 600;
	
	/**
	 * 데이터베이스 연결 종료 시 발생하는 이벤트를 등록한 커넥션 정보를 관리하는 맵
	 * KEY : 데이터베이스 관리 아이디
	 * VALUE : 아이디에 따른 커넥션 객체 리스트
	 */
	private Map<Long, List<CloseEvent>> closeEventMap = new HashMap<Long, List<CloseEvent>>();
	
	/**		연결 종료된 커넥션 아이디 관리 큐		*/
	private Queue<Long> closeEventIdWaitQueue = new ArrayDeque<Long>();
	
	/**		자동종료 작업을 수행한 ResultSet / Statement 리스트		*/
	private List<AutoCloseResult> autoCloseResultList = new ArrayList<AutoCloseResult>();
		
	private final DatabasePool databasePool;
	
	
	/**
	 * 데이터베이스풀 관리자
	 * @param databasePool
	 */
	DatabasePoolManager(DatabasePool databasePool){
		this.databasePool = databasePool;
		super.setDaemon(true);
		start();
	} 
	
	public void run(){
		int count = 0;
		
		while(true){
			
			try{		Thread.sleep(CHECK_SLEEP_TIME);		}catch(Exception e){}
			
			// 대기중인 연결종료 아이디가 있을 경우
			while( !this.closeEventIdWaitQueue.isEmpty() )
				this.executeCloseEvent(this.closeEventIdWaitQueue.poll());
			
			// ResultSet 자동 자원 반납 처리
			this.executeAutoCloseResult();
			
			// 커넥션 연결유지 관리를 위한 validationQuery 실행
			if( count++ >= VALIDATION_COUNT ){
				this.databasePool.checkConnectionValidation();
				count = 0;
			}
		}
				
	}
	
	/**
	 * 일정 시간이 지난 ResultSet 을 강제로 자원반납 시킨다
	 */
	private void executeAutoCloseResult(){
		if( this.autoCloseResultList.isEmpty() )		return;
		
		long nowTime = System.currentTimeMillis();		// 현재 시간
		int size = this.autoCloseResultList.size();
		
		for(int i=0; i < size; i++){
			if( nowTime >= this.autoCloseResultList.get(i).getAutoCloseTime() ){
				// 자동 종료 시간이 되었을 경우
				this.autoCloseResultList.remove(i).closeResult();
				size--;		// remove 했으므로 전체 크기 감소
				i--;			// remove 했으므로 현재 위치 감소
			}else{
				break;		// 가장 오래된 데이터가 현재 시간을 넘지 않았으면 다음에 다시 처리
			}
		}
		
	}
	
	/**	
	 * 해당 아이디와 동일한 CloseEvent 객체의 close() 호출 작업을 수행
	 * @param id
	 */
	private void executeCloseEvent(long id){
		List<CloseEvent> list = this.closeEventMap.remove(id);
		
		if( list == null )		return;
		
		int size=list.size();
		for(int i=0; i < size; i++){
			list.get(i).close();
		}
		
		list = null;
	}
	
	/**
	 * 데이터베이스 연결 해제시 호출 받을 수 있도록 이벤트 등록
	 * @param event
	 */
	synchronized void addCloseEvent(CloseEvent event){
		
		long id = event.getId();
		
		List<CloseEvent> list = this.closeEventMap.get(id);
		
		if( list == null ){
			list = new java.util.ArrayList<CloseEvent>();
			this.closeEventMap.put(id, list);
		}
		
		list.add(event);
	}
	
	/**
	 * 연결이 종료된 커넥션 관리 아이디를 등록하여 자동 종료되게끔 한다
	 * @param id		데이터베이스 관리 아이디
	 */
	void runCloseEvent(long id){
		this.closeEventIdWaitQueue.offer(id);
	}
	
	/**
	 * SELECT 쿼리로 인해 사용된 ResultSet 의 자동 자원 반납 등록
	 * @param acrs		ResultSet 자동 종료 클레스
	 */
	void addAutoCloseResult(AutoCloseResult acrs){
		this.autoCloseResultList.add(acrs);
	}
	
}
