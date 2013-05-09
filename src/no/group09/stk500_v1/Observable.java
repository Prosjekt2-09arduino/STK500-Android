package no.group09.stk500_v1;

import java.beans.PropertyChangeListener;

public interface Observable {
	
	public void addStateChangedListener(PropertyChangeListener listener);
	
	public void removeStateChangedListener(PropertyChangeListener listener);
	
	public void fireStateChanged();
}
