package com.thevoiceasia.phonebox.chat;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smackx.muc.SubjectUpdatedListener;

import com.thevoiceasia.phonebox.gui.Client;
/**
 * Simple class which sets up a JPanel in border layout with a JScrollPane
 * 
 * Has an addMessage(String) method to add messages to the panel (for a simple
 * XMPP chat program but obviously this part is protocol agnostic)
 * @author waynemerricks
 *
 */
public class ChatMessagePanel extends JPanel implements PacketListener, SubjectUpdatedListener{

	private JTextPane messages = new JTextPane();
	private Style chatStyle;
	private String myNickName;
	private JLabel topic = new JLabel();
	private I18NStrings xStrings;
	
	private static final Logger LOGGER = Logger.getLogger(Client.class.getName());//Logger
	private static final Level LOG_LEVEL = Level.INFO;
	private static final Color GREEN = new Color(109, 154, 11);
	private static final long serialVersionUID = 1L;

	/**
	 * Sets up the Message Panel, uses language/country to get the right I18N strings
	 * Nickname is used to highlight your own messages in a different colour
	 * @param language
	 * @param country
	 * @param myNickName
	 */
	public ChatMessagePanel(String language, String country, String myNickName){
		
		super();
		this.myNickName = myNickName;
		this.setLayout(new BorderLayout());
		setupLogging();
		
		//Setup TextArea
		messages.setEditable(false);
		xStrings = new I18NStrings(language, country);
		chatStyle = messages.addStyle("chatStyle", null); //$NON-NLS-1$
		
		//Setup Topic
		//Increase font size
		Font t = topic.getFont();
		t = t.deriveFont(Font.BOLD);
		t = t.deriveFont(24F);
		topic.setFont(t);
		topic.setHorizontalTextPosition(JLabel.CENTER);
		topic.setHorizontalAlignment(JLabel.CENTER);
		topic.setText(xStrings.getString("ChatManager.topicLabel")); //$NON-NLS-1$
		
		JPanel north = new JPanel(new BorderLayout());
		north.add(topic, BorderLayout.SOUTH);
		north.add(new JSeparator(JSeparator.HORIZONTAL));
		
		this.add(north, BorderLayout.NORTH);
		
		JScrollPane messageScroll = new JScrollPane(messages);
		messageScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		this.add(messageScroll, BorderLayout.CENTER);
		
	}
	
	/**
	 * Sets the chat window text colour for the next string to be inserted
	 * @param c
	 */
	private void setTextColour(Color c){
		
		StyleConstants.setForeground(chatStyle, c);
		
	}
	
	/**
	 * Clears all text in the chat window
	 */
	public void clear(){
		
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				messages.setText(null);
			}
		});
		
	}
	
	/**
	 * Set the Logger object
	 */
	private void setupLogging(){
		
		LOGGER.setLevel(LOG_LEVEL);
		
		try{
			LOGGER.addHandler(new FileHandler("chatLog.log")); //$NON-NLS-1$
		}catch(IOException e){
			
			e.printStackTrace();
			showWarning(e, xStrings.getString("ChatManager.logCreateError")); //$NON-NLS-1$
			
		}
		
	}
	
	/**
	 * Logs a warning message and displays friendly message to user
	 * @param e
	 * @param friendlyErrorMessage
	 */
	private void showWarning(Exception e, String friendlyErrorMessage){
		
		System.err.println(xStrings.getString("ChatManager.errorPrefix") + friendlyErrorMessage); //$NON-NLS-1$
		e.printStackTrace();
		JOptionPane.showMessageDialog(null, friendlyErrorMessage, xStrings.getString("ChatManager.errorBoxTitle"), JOptionPane.WARNING_MESSAGE); //$NON-NLS-1$
		LOGGER.warning(friendlyErrorMessage);
		
	}
	
	@Override
	public void processPacket(Packet XMPPPacket) {
		
		LOGGER.info(xStrings.getString("ChatManager.receivedMessage") + XMPPPacket); //$NON-NLS-1$
		
		if(XMPPPacket instanceof Message){
			
			Message message = (Message)XMPPPacket;
			
			String friendlyFrom = message.getFrom();
			if(friendlyFrom.contains("/")) //$NON-NLS-1$
				friendlyFrom = friendlyFrom.split("/")[1]; //$NON-NLS-1$
			
			String b = message.getBody();
			
			if(friendlyFrom.equals(xStrings.getString("ChatManager.SYSTEM"))){  //$NON-NLS-1$
				
				//Control Messages, need to clean up message body and act accordingly
				//phonebox@conference.elastix/waynemerricks !ChatManager.chatParticipantLeft!
				if(b.contains("/")) //$NON-NLS-1$
					b = b.split("/")[1]; //$NON-NLS-1$
				
			}
			
			final String from = friendlyFrom;
			final String body = b + "\n"; //$NON-NLS-1$
			
			SwingUtilities.invokeLater(new Runnable(){
				public void run(){
					
					try{
						
						if(from.equals(myNickName))
							setTextColour(Color.RED);
						else if(from.equals("SYSTEM")) //$NON-NLS-1$
							setTextColour(GREEN);
						else
							setTextColour(Color.BLUE);
						
						StyledDocument doc = messages.getStyledDocument();
						doc.insertString(doc.getLength(), from + ": ", chatStyle); //$NON-NLS-1$
						
						//Add Message in normal black
						if(!from.equals("SYSTEM")) //$NON-NLS-1$
							setTextColour(Color.BLACK);
						doc.insertString(doc.getLength(), body, chatStyle);
						
					}catch(BadLocationException e){
						LOGGER.severe(xStrings.getString("ChatManager.errorInsertingMessage")); //$NON-NLS-1$
						e.printStackTrace();
					}
					
				}
			});
			
		}
		
	}

	@Override
	public void subjectUpdated(String subject, String from) {
		
		LOGGER.info(xStrings.getString("ChatManager.logSettingTopic") + subject); //$NON-NLS-1$
		
		/*
		 * Can't set subject to "" as XMPP server ignores the change.  This causes a bug when the subject is 
		 * set to the same subject as you get an XMPP error.  
		 * 
		 * To work around this, topic is set to a greeter message when we don't want to see it.
		 * 
		 * We then check if the topic is the greeter message and if so, set the topic label to ""
		 */
		
		if(subject != null && subject.equals(xStrings.getString("ChatManager.emptyTopic"))) //$NON-NLS-1$
			subject = null;
			
		final String t = subject;
		
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				
				topic.setText(t);
				
			}
		});
		
	}
	
}
