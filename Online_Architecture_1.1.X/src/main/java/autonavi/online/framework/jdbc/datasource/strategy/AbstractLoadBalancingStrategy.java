package autonavi.online.framework.jdbc.datasource.strategy;

import java.util.List;

public abstract class AbstractLoadBalancingStrategy implements
		LoadBalancingStrategy {
	List<String> dsNames;

	@Override
	public void init(List<String> dsNames) {
		this.dsNames = dsNames;
	}

}
