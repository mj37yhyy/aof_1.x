package autonavi.online.framework.jdbc.transactional;

public interface ShardingTransactionCallback {

	public Object doInTransaction() throws Throwable;

}
