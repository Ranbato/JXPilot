package net.sf.jxpilot.game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.*;

/**
 * Haldles in game messages for JXPilot.
 * 
 * @author Taras Kostiak
 * @author Vlad Firoiu
 * 
 */
public class MessagePool {

    /**
     * Shows timeout between publishing message and removing it.<br>
     * TODO: Should be replaced with settings(transfered from XPilotPanel).
     */
    public static final long MESSAGE_REMOVE_TIMEOUT = 30 * 1000;

    /**
     * Shows timeout between publishing message(in white color) and graying it.<br>
     * TODO: Should be replaced with settings(transfered from XPilotPanel).
     */
    public static final long MESSAGE_GRAY_TIMEOUT = 15 * 1000;

    /**
     * Max messages to print.
     */
    public static final byte MAX_MESSAGES = 10;

    /**
     * Color for new messages.
     */
    public static final Color NEW_MESSAGE_COLOR = Color.WHITE;

    /**
     * Color of "grayed" messages.
     */
    public static final Color GRAYED_MESSAGE_COLOR = Color.GRAY;

    /**
     * Messages stored in this pool.
     */
    private LinkedList<TimedMessage> messages = null;

    /**
     * Creates new <code>MessagePool</code>.
     */
    public MessagePool() {
        messages = new LinkedList<TimedMessage>();
    }

    /**
     * Adds message to print on game screen.
     * 
     * @param message
     *            Message, to print.
     */
    public synchronized void publishMessage(String message) {
        messages.addFirst(new TimedMessage(message));
    }

    /**
     * This class stands for message with date of it's publish.
     * 
     * @author Taras Kostiak
     * 
     */
    private static class TimedMessage {

        /**
         * Stored message.
         */
        private String message = null;

        /**
         * Time of creating this message.
         */
        private long publishTime = -1;

        /**
         * Creates new <code>TimedMessage</code>, with publish time = current
         * system time.
         */
        public TimedMessage(String message) {
            this.message = message;
            publishTime = System.currentTimeMillis();
        }

        /**
         * @see #message
         */
        public String getMessage() {
            return message;
        }

        /**
         * @see #publishTime
         */
        public long getPublishTime() {
            return publishTime;
        }

    }

    /**
     * Renders current messages into given Graphics2D.
     * 
     * @param g2
     */
    public void render(Graphics2D g2) {
        final int baseX = 16;
        final int baseY = 40;
        final int yDistance = 16;

        long currentTime = System.currentTimeMillis();

        Iterator<TimedMessage> iterator = messages.iterator();
        
        for (int i = 0; iterator.hasNext();) {
        	TimedMessage mess = iterator.next();
            
        	long messageTime = currentTime-mess.getPublishTime();
        	
        	if(i>MAX_MESSAGES || messageTime>MESSAGE_REMOVE_TIMEOUT)
        	{
        		iterator.remove();
        	}
        	else
        	{
        		if(messageTime>MESSAGE_GRAY_TIMEOUT)	
        		{
        			g2.setColor(GRAYED_MESSAGE_COLOR);
        		}
        		else
        		{
        			g2.setColor(NEW_MESSAGE_COLOR);
        		}
        		g2.drawString(mess.getMessage(), baseX, baseY+i*yDistance);
        		i++;
        	}
        }
        	/*
        	if (!grayed
                    && (currentTime - mess.getPublishTime()) > MESSAGE_GRAY_TIMEOUT)
                g2.setColor(GRAYED_MESSAGE_COLOR);

            if (!noMore)
                noMore = ((messagesSize - i) > MAX_MESSAGES)
                        || (currentTime - mess.getPublishTime()) > MESSAGE_REMOVE_TIMEOUT;

            if (noMore) {
                if (messagesToRemove == null)
                    messagesToRemove = new LinkedList<TimedMessage>();

                messagesToRemove.add(mess);
            }
            else
                g2.drawString(messages.get(i).getMessage(), baseX, baseY
                        + (messagesSize - 1 - i) * yDistance);
        }

        if (messagesToRemove != null)
            messages.removeAll(messagesToRemove);
            */
        
    }
}