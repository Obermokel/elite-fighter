/*
 * Author:  Boris Guenther
 * Date:    27.12.2018
 * Time:    15:16:11
 */
package borg.ed.cz.gui;

import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import borg.ed.galaxy.journal.JournalUpdateListener;
import borg.ed.galaxy.journal.Status;
import borg.ed.galaxy.journal.StatusUpdateListener;
import borg.ed.galaxy.journal.events.AbstractJournalEvent;

/**
 * CombatControlFrame
 *
 * @author <a href="mailto:boris.guenther@redteclab.com">Boris Guenther</a>
 */
public class CombatControlFrame extends JFrame implements WindowListener, KeyListener, JournalUpdateListener, StatusUpdateListener {

    private static final long serialVersionUID = 4349890395389987402L;

    private final JLabel lblStatus;

    public CombatControlFrame() {
        super("EDCZ");

        this.lblStatus = new JLabel();
        this.lblStatus.setVerticalAlignment(SwingConstants.TOP);

        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.addWindowListener(this);
        this.setLayout(new BorderLayout());
        this.add(this.lblStatus, BorderLayout.CENTER);
        this.setSize(400, 200);
        this.addKeyListener(this);
    }

    @Override
    public void onNewJournalEntry(AbstractJournalEvent event) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onNewStatus(Status status) {
        // TODO Auto-generated method stub

    }

    @Override
    public void windowOpened(WindowEvent e) {
        // Do nothing
    }

    @Override
    public void windowClosing(WindowEvent e) {
        // Do nothing
    }

    @Override
    public void windowClosed(WindowEvent e) {
        System.exit(0);
    }

    @Override
    public void windowIconified(WindowEvent e) {
        // Do nothing
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
        // Do nothing
    }

    @Override
    public void windowActivated(WindowEvent e) {
        // Do nothing
    }

    @Override
    public void windowDeactivated(WindowEvent e) {
        // Do nothing
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // Do nothing
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            System.exit(-1);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // Do nothing
    }

}
