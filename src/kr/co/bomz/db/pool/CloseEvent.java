package kr.co.bomz.db.pool;

/**
 * �����ͺ��̽� ���� ���� �̺�Ʈ
 * 
 * @author Bomz
 * @version 1.0
 * @since 1.0
 *
 */
public interface CloseEvent {
	
	/**		���� ���̵� ���� ����		*/
	long getId();
	
	/**		�����ͺ��̽� ���� ����		*/
	void close();
}
