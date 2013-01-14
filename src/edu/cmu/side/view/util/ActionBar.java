package edu.cmu.side.view.util;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;

import se.datadosen.component.RiverLayout;

public abstract class ActionBar extends JPanel{
	
	protected JButton add = new JButton();
	protected JButton cancel = new JButton();
	
	protected JProgressBar progressBar = new JProgressBar();
	protected JTextField name = new JTextField(5);
	protected JLabel nameLabel = new JLabel("Name:");
	protected JPanel settings = new JPanel(new RiverLayout());
	protected JComboBox combo;
	protected JPanel updaters = new JPanel(new RiverLayout());
	Font font = new Font(Font.SANS_SERIF, Font.BOLD, 20);

	public ActionBar(){
		setLayout(new GridLayout(1,3));
		add.setFont(font);
		setBorder(BorderFactory.createLineBorder(Color.gray));
		settings.setBackground(Color.white);
		updaters.setBackground(Color.white);
		settings.add("left", nameLabel);
		settings.add("left", name);
		progressBar.setPreferredSize(new Dimension(50,25));
		updaters.add("center hfill", progressBar);
		progressBar.setVisible(false);
		ImageIcon iconCancel = new ImageIcon("toolkits/icons/cancel.png");
		cancel.setText("");
		cancel.setIcon(iconCancel);
		cancel.setEnabled(false);
		cancel.setToolTipText("Cancel");
		JPanel left = new JPanel(new RiverLayout());
		JPanel middle = new JPanel(new RiverLayout());
		JPanel right = new JPanel(new RiverLayout());
		left.add("hfill", settings);
		middle.add("hfill", add);
		right.add("hfill", updaters);
		right.add("left", cancel);
		left.setBackground(Color.white);
		middle.setBackground(Color.white);
		right.setBackground(Color.white);
		add(left);
		add(middle);
		add(right);
	}

	public void refreshPanel(){
	}
}
