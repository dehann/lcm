package lcm.lcm;

import java.io.*;
import java.util.*;

/**
 * Accumulates received LCM messages in a queue.
 *
 * The aggregator has a configurable maximum buffer size.  If too many messages
 * are aggregated without having been retrieved, then older messages are
 * discarded until the maximum buffer size is no longer exceeded.
 */
public class MessageAggregator
    implements LCMSubscriber
{
    public class Message {
	final public byte[] data;
	final public String channel;
	public Message(String channel_, byte[] data_)
	{
	    data = data_;
	    channel = channel_;
	}
    }

    ArrayDeque<Message> messages = new ArrayDeque<Message>();
    long queue_data_size = 0;
    long max_queue_data_size = 10 * (1 << 20); // 10 megabytes

    public synchronized void messageReceived(LCM lcm, String channel, 
	    DataInputStream dins)
    {
	try {
	    byte data[] = new byte[dins.available()];
	    dins.readFully(data);
	    messages.addLast(new Message(channel, data));

	    queue_data_size += data.length;
	    while(queue_data_size > max_queue_data_size) {
		Message to_remove = messages.removeFirst();
		queue_data_size -= to_remove.data.length;
	    }

	    notify();
	} catch (IOException xcp) {}
    }

    public synchronized void setMaxBufferSize(long val)
    {
	max_queue_data_size = val;
    }

    public synchronized long getMaxBufferSize()
    {
	return max_queue_data_size;
    }

    public synchronized Message getNextMessage(long timeout_ms)
    {
	if(!messages.isEmpty())
	    return messages.removeFirst();

	if(timeout_ms == 0)
	    return null;

	try {
	    if(timeout_ms > 0)
		wait(timeout_ms);
	    else
		wait();

	    if(!messages.isEmpty())
		return messages.removeFirst();
	} catch (InterruptedException xcp) { }

	return null;
    }

    public synchronized int numMessagesAvailable()
    {
	return messages.size();
    }
}