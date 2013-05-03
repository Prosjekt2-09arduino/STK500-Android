package no.group09.stk500_v1;

public interface IReader {
	public IReaderState getState();
	//TODO: consider startRead();
	public int getResult();
	public int read();
	public void stop();
	public void start();
}
