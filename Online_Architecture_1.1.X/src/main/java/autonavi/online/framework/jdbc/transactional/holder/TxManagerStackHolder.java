package autonavi.online.framework.jdbc.transactional.holder;

import autonavi.online.framework.jdbc.transactional.TransactionAttribute;
import autonavi.online.framework.util.ConcurrentStack;

public class TxManagerStackHolder {
	private TransactionAttribute ta;
	private ConcurrentStack<TxManagerHolder> stack;
	public TxManagerStackHolder(TransactionAttribute ta,ConcurrentStack<TxManagerHolder> stack){
		this.ta=ta;
		this.stack=stack;
	}
	public TransactionAttribute getTa() {
		return ta;
	}
	public ConcurrentStack<TxManagerHolder> getStack() {
		return stack;
	}
	
	
	

}
