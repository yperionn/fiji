package archipelago.network;

import archipelago.FijiArchipelago;
import archipelago.data.ClusterMessage;
import archipelago.listen.MessageType;
import archipelago.listen.TransceiverExceptionListener;
import archipelago.listen.TransceiverListener;

import java.io.*;
import java.util.ConcurrentModificationException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Message transciever class
 */
public class MessageXC
{

    private class RXThread extends Thread
    {
        public void run()
        {
            while (active.get())
            {
                try
                {
                    ClusterMessage message = (ClusterMessage)objectInputStream.readObject();
                    // Don't debug beats, or they'll fill your log
                    if (message.type != MessageType.BEAT)
                    {
                        FijiArchipelago.debug("RX: Recieved message " + message + " from " + hostName);
                    }
                    xcListener.handleMessage(message);
                    objectInputStream = new ObjectInputStream(inStream);
                }
                catch (ClassCastException cce)
                {
                    xcExceptionListener.handleRXThrowable(cce, xc);
//                    FijiArchipelago.err("RX: " + hostName + " Got ClassCastException: " + cce);
//                    queueMessage(MessageType.ERROR, cce);
                }
                /*catch (EOFException eofe)
                {
                    xcExceptionListener.handleRXThrowable(eofe);
                }
                catch (StreamCorruptedException sce)
                {
                    // When this happens, nothing else to do but close
                    FijiArchipelago.err("RX: " + hostName + " Got StreamCorruptedException: " + sce);
                    sce.printStackTrace();
                    close();
                }*/
                catch (IOException ioe)
                {
                    xcExceptionListener.handleRXThrowable(ioe, xc);
//                    FijiArchipelago.err("RX: " + hostName + " Got IOException: " + ioe);
//                    queueMessage(MessageType.ERROR,  ioe);
                    //close();
                }
                catch (ClassNotFoundException cnfe)
                {
                    xcExceptionListener.handleRXThrowable(cnfe, xc);
//                    FijiArchipelago.err("RX: " + hostName + " Got ClassNotFoundException: " + cnfe);
//                    queueMessage(MessageType.ERROR, cnfe);
                }
            }
        }
    }
    
    private class TXThread extends Thread
    {
        public void run()
        {
            while (active.get())
            {
                ClusterMessage nextMessage = null;
                try
                {
                    nextMessage = messageQ.poll(waitTime, tUnit);
                }
                catch (InterruptedException ie)
                {
                    active.set(false);
                }

                if (nextMessage != null)
                {
                    try
                    {
                        objectOutputStream.writeObject(nextMessage);
                        objectOutputStream.flush();
                        if (nextMessage.type != MessageType.BEAT)
                        {
                            FijiArchipelago.debug("TX: Successfully wrote message "
                                    + nextMessage + " to " + hostName);
                        }
                        objectOutputStream = new ObjectOutputStream(outStream);
                    }
                    catch (NotSerializableException nse)
                    {
                        xcExceptionListener.handleTXThrowable(nse, xc);
//                        FijiArchipelago.err("TX " + hostName
//                                + " tried to send a non serializable object: " + nse);
                    }
                    catch (IOException ioe)
                    {
                        xcExceptionListener.handleTXThrowable(ioe, xc);
//                        FijiArchipelago.err("TX " + hostName + " failed: " + ioe);
//
//                        active.set(false);
//                        close();
                    }
                    catch (ConcurrentModificationException ccme)
                    {
                        xcExceptionListener.handleTXThrowable(ccme, xc);
//                        FijiArchipelago.err("TX: Concurrent modification exception: " + ccme);
                    }
                    catch (Exception e)
                    {
                        xcExceptionListener.handleTXThrowable(e, xc);
//                        FijiArchipelago.err("TX: Caught unexpected exception: " + e);
                    }
                }
            }
        }
    }
    
    public static final long DEFAULT_WAIT = 10000;
    public static final TimeUnit DEFAULT_UNIT = TimeUnit.MILLISECONDS;

    private final ArrayBlockingQueue<ClusterMessage> messageQ;
    private ObjectOutputStream objectOutputStream;
    private ObjectInputStream objectInputStream;
    private final Thread txThread, rxThread;
    private final AtomicBoolean active;
    private final long waitTime;
    private final TimeUnit tUnit;
    private final String hostName;
    private final TransceiverListener xcListener;
    private final TransceiverExceptionListener xcExceptionListener;
    private final OutputStream outStream;
    private final InputStream inStream;
    
    private final MessageXC xc = this;

    public MessageXC(final InputStream inStream,
                     final OutputStream outStream,
                     final TransceiverListener listener,
                     final TransceiverExceptionListener listenerE,
                     final String hostName) throws IOException
    {
        this(inStream, outStream, listener, listenerE, hostName, DEFAULT_WAIT, DEFAULT_UNIT);
    }

    public MessageXC(InputStream inStream,
                     OutputStream outStream,
                     final TransceiverListener listener,
                     final TransceiverExceptionListener listenerE,
                     String name,
                     long wait,
                     TimeUnit unit) throws IOException
    {
        hostName = name;
        messageQ = new ArrayBlockingQueue<ClusterMessage>(16, true);
        objectOutputStream = new ObjectOutputStream(outStream);
        objectInputStream =  new ObjectInputStream(inStream);
        this.inStream = inStream;
        this.outStream = outStream;
        active = new AtomicBoolean(true);
        waitTime = wait;
        tUnit = unit;
        xcListener = listener;
        xcExceptionListener = listenerE;

        txThread = new TXThread();
        rxThread = new RXThread();

        rxThread.start();
        txThread.start();
    }

    public void close()
    {
        if (active.get())
        {
            FijiArchipelago.debug("XC: Got close.");
            active.set(false);
            xcListener.streamClosed();
            txThread.interrupt();
            rxThread.interrupt();
        }
    }

    public boolean join()
    {
        if (txThread.isAlive() || rxThread.isAlive())
        {
            try
            {
                txThread.join();
                rxThread.join();
                return true;
            }
            catch (InterruptedException ie)
            {
                return false;
            }
        }
        else
        {
            return true;
        }
    }

    public synchronized boolean queueMessage(ClusterMessage message)
    {
        try
        {
            boolean verbose = message.type != MessageType.BEAT;
            if (verbose)
            {
                FijiArchipelago.debug("XC: Queuing message to " + hostName);
            }

            messageQ.put(message);

            if (verbose)
            {
                FijiArchipelago.debug("XC: Message to " + hostName + " queued successfully");
            }
            return true;
        }
        catch (InterruptedException ie)
        {
            return false;
        }
    }

    public boolean queueMessage(final MessageType type)
    {
        ClusterMessage cm = new ClusterMessage(type);
        return queueMessage(cm);
    }

    public boolean queueMessage(final MessageType type, final Serializable o)
    {
        ClusterMessage cm = new ClusterMessage(type);
        cm.o = o;
        return queueMessage(cm);
    }
    
    public String getHostName()
    {
        return hostName;
    }
}