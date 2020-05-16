package org.ngs.bigx.middleware.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSplitPane;
import javax.swing.JTextField;

import org.ngs.bigx.middleware.core.BiGXMiddlewareCore;
import org.ngs.bigx.middleware.etc.LinuxInterfaceServer;

public class LoginJFrame extends JFrame{
	/**
	 * 
	 */
	private static final long serialVersionUID = -5374697355431380265L;
	private BiGXMiddlewareCore bigxcontext;
	public static final int BORDERMARGIN = 10;

	public LoginJFrame self = null;
	public static final String strLoginIdTextField = "testpatient@gmail.com";
	public static final String strLoginPasswordTextField = "testpatient";
	
	public LoginJFrame(BiGXMiddlewareCore bigxcontext)
	{
		this.bigxcontext = bigxcontext;
		this.self = this;
	}
	
	public void initLoginJFrame() throws IOException
	{
		JSplitPane MainContentSplitPane = new JSplitPane();
		MainContentSplitPane.setDividerSize(0);
		MainContentSplitPane.setEnabled(false);
		MainContentSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
		
		JPanel leftPanel = new JPanel();
		BufferedImage wPic = ImageIO.read(this.getClass().getResource("/resources/loginicon.png"));
		JLabel wIcon = new JLabel(new ImageIcon(wPic));
		leftPanel.add(wIcon);
		MainContentSplitPane.setLeftComponent(leftPanel);

		JPanel rightPanel = new JPanel(new BorderLayout());
		rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
		final JTextField tfLoginId = new JTextField(strLoginIdTextField);
		final JPasswordField tfLoginPassword = new JPasswordField(strLoginPasswordTextField);
		JButton btnLogin = new JButton("Login");
		JButton btnStartGame = new JButton("StartGame");
		JLabel lbLoginMessage01 = new JLabel(" ");
		JLabel lbLoginMessage02 = new JLabel(" ");
		
		tfLoginId.setAlignmentX(CENTER_ALIGNMENT);
		tfLoginPassword.setAlignmentX(CENTER_ALIGNMENT);
		btnLogin.setAlignmentX(CENTER_ALIGNMENT);
		btnStartGame.setAlignmentX(CENTER_ALIGNMENT);
		lbLoginMessage01.setAlignmentX(CENTER_ALIGNMENT);
		lbLoginMessage02.setAlignmentX(CENTER_ALIGNMENT);

        btnLogin.setPreferredSize(new Dimension(300, 50));
        btnLogin.setMaximumSize(new Dimension(300, 50)); // set max = pref

        btnStartGame.setPreferredSize(new Dimension(300, 50));
        btnStartGame.setMaximumSize(new Dimension(300, 50)); // set max = pref
		
		rightPanel.add(tfLoginId);
		rightPanel.add(tfLoginPassword);
		rightPanel.add(btnLogin);
		rightPanel.add(btnStartGame);
		rightPanel.add(lbLoginMessage01);
		rightPanel.add(lbLoginMessage02);
		
		tfLoginId.addMouseListener(new MouseListener() {
			
			@Override
			public void mouseReleased(MouseEvent e) { }
			
			@Override
			public void mousePressed(MouseEvent e) { }
			
			@Override
			public void mouseExited(MouseEvent e) { }
			
			@Override
			public void mouseEntered(MouseEvent e) { }
			
			@Override
			public void mouseClicked(MouseEvent e) {
				if(tfLoginId.getText().equals(strLoginIdTextField)) {
					tfLoginId.setText("");
				}
			}
		});
		tfLoginId.addFocusListener(new FocusListener() {
			
			@Override
			public void focusLost(FocusEvent e) {
				if(tfLoginId.getText().equals("")) {
					tfLoginId.setText(strLoginIdTextField);
				}
			}
			
			@Override
			public void focusGained(FocusEvent e) { }
		});
		
		tfLoginPassword.addMouseListener(new MouseListener() {
			
			@Override
			public void mouseReleased(MouseEvent e) { }
			
			@Override
			public void mousePressed(MouseEvent e) { }
			
			@Override
			public void mouseExited(MouseEvent e) { }
			
			@Override
			public void mouseEntered(MouseEvent e) { }
			
			@SuppressWarnings("deprecation")
			@Override
			public void mouseClicked(MouseEvent e) {
				if(tfLoginPassword.getText().equals(strLoginPasswordTextField)) {
					tfLoginPassword.setText("");
				}
			}
		});
		tfLoginPassword.addFocusListener(new FocusListener() {
			
			@SuppressWarnings("deprecation")
			@Override
			public void focusLost(FocusEvent e) {
				if(tfLoginPassword.getText().equals("")) {
					tfLoginPassword.setText(strLoginPasswordTextField);
				}
			}
			
			@Override
			public void focusGained(FocusEvent e) { }
		});
		
		btnLogin.addMouseListener(new MouseListener() {
			
			@Override
			public void mouseReleased(MouseEvent e) { }
			
			@Override
			public void mousePressed(MouseEvent e) { }
			
			@Override
			public void mouseExited(MouseEvent e) { }
			
			@Override
			public void mouseEntered(MouseEvent e) { }
			
			@SuppressWarnings("resource")
			@Override
			public void mouseClicked(MouseEvent e) {
				// Get ID/Password
				String id = tfLoginId.getText();
				@SuppressWarnings("deprecation")
				String password = tfLoginPassword.getText();
				int serverportnumber = LinuxInterfaceServer.serverportnumber;
				DatagramSocket socket;

				// Send a UDP pakcet to itselft for login
				try{
					String command = "REMOTE ACCOUNT LOGIN " + id + " " + password;
					byte[] data = command.getBytes("US-ASCII");
					DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName("localhost"), serverportnumber);
					socket = new DatagramSocket();
					socket.send(packet);
				} catch (IOException ee) {
					ee.printStackTrace();
					return;
				}
				
				// Assign ID/Password to context
				bigxcontext.setiXerciseAccountName(id);
				bigxcontext.setiXercisePassword(password);
				
				// Close the Login Window Right away
				self.dispose();
				
//				lbLoginMessage01.setText("Login Failed");
//				lbLoginMessage02.setText("Please check username and password");
//				
//				lbLoginMessage01.setForeground(Color.RED);
//				lbLoginMessage02.setForeground(Color.RED);
//
//				bigxcontext.setiXerciseAccountName("login");
//				bigxcontext.setiXercisePassword("password");
			}
		});
		
		btnStartGame.addMouseListener(new MouseListener() {
			
			@Override
			public void mouseReleased(MouseEvent e) { }
			
			@Override
			public void mousePressed(MouseEvent e) { }
			
			@Override
			public void mouseExited(MouseEvent e) { }
			
			@Override
			public void mouseEntered(MouseEvent e) { }
			
			@SuppressWarnings("resource")
			@Override
			public void mouseClicked(MouseEvent e) {
				// Get ID/Password
				String id = tfLoginId.getText();
				@SuppressWarnings("deprecation")
				String password = tfLoginPassword.getText();
				int serverportnumber = LinuxInterfaceServer.serverportnumber;
				DatagramSocket socket;

				// Send a UDP pakcet to itselft for login
				try{
					String command = "REMOTE START GAME minecraft";
					byte[] data = command.getBytes("US-ASCII");
					DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName("localhost"), serverportnumber);
					socket = new DatagramSocket();
					socket.send(packet);
				} catch (IOException ee) {
					ee.printStackTrace();
					return;
				}
				
				// Assign ID/Password to context
				bigxcontext.setiXerciseAccountName(id);
				bigxcontext.setiXercisePassword(password);
				
				// Close the Login Window Right away
				self.dispose();
				
//				lbLoginMessage01.setText("Login Failed");
//				lbLoginMessage02.setText("Please check username and password");
//				
//				lbLoginMessage01.setForeground(Color.RED);
//				lbLoginMessage02.setForeground(Color.RED);
//
//				bigxcontext.setiXerciseAccountName("login");
//				bigxcontext.setiXercisePassword("password");
			}
		});
		
		MainContentSplitPane.setRightComponent(rightPanel);

		this.getContentPane().add(MainContentSplitPane, BorderLayout.CENTER);
	}
	
	public void setLoginCredential(String accountName, String password)
	{
		this.bigxcontext.setiXerciseAccountName(accountName);
		this.bigxcontext.setiXercisePassword(password);
	}
}
