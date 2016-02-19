package autonavi.online.framework.jdbc.dao.paging;


public interface Paging {

	public String getPagingSQL(String sql, long start, long limit);

	public String getCountSQL(String sql, String column);
}
