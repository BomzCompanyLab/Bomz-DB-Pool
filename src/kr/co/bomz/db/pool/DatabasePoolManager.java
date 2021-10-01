package kr.co.bomz.db.pool;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * �����ͺ��̽� ���� ���� �� ���� ���� ����
 * 
 * @author Bomz
 * @version 1.0
 * @since 1.0
 *
 */
public class DatabasePoolManager extends Thread{

	/**		�����ͺ��̽� ���� ���� �ݺ� ��� �ð�		*/
	private static final long CHECK_SLEEP_TIME = 1000;
	
	/**		�����ͺ��̽� VALIDATION_QUERY �˻� ��û �ֱ�(10��)		*/
	private static final int VALIDATION_COUNT = 600;
	
	/**
	 * �����ͺ��̽� ���� ���� �� �߻��ϴ� �̺�Ʈ�� ����� Ŀ�ؼ� ������ �����ϴ� ��
	 * KEY : �����ͺ��̽� ���� ���̵�
	 * VALUE : ���̵� ���� Ŀ�ؼ� ��ü ����Ʈ
	 */
	private Map<Long, List<CloseEvent>> closeEventMap = new HashMap<Long, List<CloseEvent>>();
	
	/**		���� ����� Ŀ�ؼ� ���̵� ���� ť		*/
	private Queue<Long> closeEventIdWaitQueue = new ArrayDeque<Long>();
	
	/**		�ڵ����� �۾��� ������ ResultSet / Statement ����Ʈ		*/
	private List<AutoCloseResult> autoCloseResultList = new ArrayList<AutoCloseResult>();
		
	private final DatabasePool databasePool;
	
	
	/**
	 * �����ͺ��̽�Ǯ ������
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
			
			// ������� �������� ���̵� ���� ���
			while( !this.closeEventIdWaitQueue.isEmpty() )
				this.executeCloseEvent(this.closeEventIdWaitQueue.poll());
			
			// ResultSet �ڵ� �ڿ� �ݳ� ó��
			this.executeAutoCloseResult();
			
			// Ŀ�ؼ� �������� ������ ���� validationQuery ����
			if( count++ >= VALIDATION_COUNT ){
				this.databasePool.checkConnectionValidation();
				count = 0;
			}
		}
				
	}
	
	/**
	 * ���� �ð��� ���� ResultSet �� ������ �ڿ��ݳ� ��Ų��
	 */
	private void executeAutoCloseResult(){
		if( this.autoCloseResultList.isEmpty() )		return;
		
		long nowTime = System.currentTimeMillis();		// ���� �ð�
		int size = this.autoCloseResultList.size();
		
		for(int i=0; i < size; i++){
			if( nowTime >= this.autoCloseResultList.get(i).getAutoCloseTime() ){
				// �ڵ� ���� �ð��� �Ǿ��� ���
				this.autoCloseResultList.remove(i).closeResult();
				size--;		// remove �����Ƿ� ��ü ũ�� ����
				i--;			// remove �����Ƿ� ���� ��ġ ����
			}else{
				break;		// ���� ������ �����Ͱ� ���� �ð��� ���� �ʾ����� ������ �ٽ� ó��
			}
		}
		
	}
	
	/**	
	 * �ش� ���̵�� ������ CloseEvent ��ü�� close() ȣ�� �۾��� ����
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
	 * �����ͺ��̽� ���� ������ ȣ�� ���� �� �ֵ��� �̺�Ʈ ���
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
	 * ������ ����� Ŀ�ؼ� ���� ���̵� ����Ͽ� �ڵ� ����ǰԲ� �Ѵ�
	 * @param id		�����ͺ��̽� ���� ���̵�
	 */
	void runCloseEvent(long id){
		this.closeEventIdWaitQueue.offer(id);
	}
	
	/**
	 * SELECT ������ ���� ���� ResultSet �� �ڵ� �ڿ� �ݳ� ���
	 * @param acrs		ResultSet �ڵ� ���� Ŭ����
	 */
	void addAutoCloseResult(AutoCloseResult acrs){
		this.autoCloseResultList.add(acrs);
	}
	
}
