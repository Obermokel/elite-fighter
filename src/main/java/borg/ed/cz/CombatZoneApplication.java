/*
 * Author:  Boris Guenther
 * Date:    27.12.2018
 * Time:    14:34:25
 */
package borg.ed.cz;

import java.awt.AWTException;
import java.awt.GraphicsEnvironment;
import java.awt.Robot;
import java.io.IOException;

import javax.swing.UIManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import borg.ed.cz.gui.CombatControlFrame;
import borg.ed.galaxy.GalaxyApplication;
import borg.ed.galaxy.journal.JournalReaderThread;
import borg.ed.galaxy.journal.StatusReaderThread;

/**
 * CombatZoneApplication
 *
 * @author <a href="mailto:boris.guenther@redteclab.com">Boris Guenther</a>
 */
@Configuration
@Import(GalaxyApplication.class)
@ComponentScan(basePackages = { "borg.ed.cz.tasks" })
public class CombatZoneApplication {

	static final Logger logger = LoggerFactory.getLogger(CombatZoneApplication.class);

	public static final ApplicationContext APPCTX = new AnnotationConfigApplicationContext(CombatZoneApplication.class);

	public static void main(String[] args) throws IOException {
		try {
			UIManager.setLookAndFeel("com.jtattoo.plaf.noire.NoireLookAndFeel");
		} catch (Exception e) {
			logger.error("Failed to set look & feel", e);
		}

		// Create the main combat zone thread
		CombatZoneThread combatZoneThread = APPCTX.getBean(CombatZoneThread.class);

		// Open the GUI
		CombatControlFrame combatControlFrame = APPCTX.getBean(CombatControlFrame.class);
		combatControlFrame.setVisible(true);

		// Listen to scanned ships
		combatZoneThread.addListener(combatControlFrame);

		// Start all event-generating threads
		JournalReaderThread journalReaderThread = APPCTX.getBean(JournalReaderThread.class);
		journalReaderThread.addListener(combatZoneThread);
		journalReaderThread.addListener(combatControlFrame);
		journalReaderThread.start();
		StatusReaderThread statusReaderThread = APPCTX.getBean(StatusReaderThread.class);
		statusReaderThread.addListener(combatZoneThread);
		statusReaderThread.addListener(combatControlFrame);
		statusReaderThread.start();

		// Wait for GUI and Journal
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			return;
		}

		// Start the main combat zone thread
		combatZoneThread.start();
	}

	@Bean
	public CombatControlFrame combatControlFrame() {
		return new CombatControlFrame();
	}

	@Bean
	public CombatZoneThread combatZoneThread() {
		return new CombatZoneThread();
	}

	@Bean
	public Robot robot() {
		try {
			return new Robot(GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice());
		} catch (AWTException e) {
			throw new RuntimeException("Failed to obtain a robot", e);
		}
	}

}
