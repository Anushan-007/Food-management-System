/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package citybites.main;

import citybites.data.DataStore;
import citybites.ui.WelcomeFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 *
 * @author User
 */
public class Main {

    public static void main(String[] args) {

        DataStore.loadSampleData();

        try {
            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName()
            );
        } catch (Exception exception) {
            System.out.println(
                    "Default look and feel will be used."
            );
        }

        SwingUtilities.invokeLater(() -> {
            WelcomeFrame welcomeFrame
                    = new WelcomeFrame();

            welcomeFrame.setVisible(true);
        });
        SwingUtilities.invokeLater(() -> {
            WelcomeFrame welcomeFrame
                    = new WelcomeFrame();

            welcomeFrame.setVisible(true);
        });
    }

}
