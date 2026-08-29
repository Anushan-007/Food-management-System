package citybites.main;

import citybites.config.DatabaseInitializer;
import citybites.ui.WelcomeFrame;
import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {

    public static void main(String[] args) {

        // Apply FlatLaf modern look and feel before any UI is created
        try {
            FlatLightLaf.setup();
        } catch (Exception e) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
        }

        // Initialise the database: create tables and seed default data
        try {
            DatabaseInitializer.initialize();
        } catch (RuntimeException e) {
            // Show a dialog and exit if the DB cannot be reached
            javax.swing.JOptionPane.showMessageDialog(
                    null,
                    "Cannot connect to the database.\n\n"
                    + e.getMessage()
                    + "\n\nPlease ensure MySQL is running and db.properties is configured.",
                    "Database Error",
                    javax.swing.JOptionPane.ERROR_MESSAGE
            );
            System.exit(1);
        }

        // Launch the UI on the Event Dispatch Thread (single WelcomeFrame)
        SwingUtilities.invokeLater(() -> new WelcomeFrame().setVisible(true));
    }
}
