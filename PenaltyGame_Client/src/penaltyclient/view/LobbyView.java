/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package penaltyclient.view;

import javax.swing.*;
import java.awt.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import penaltyclient.controller.LobbyController;
/**
 *
 * @author This PC
 */
public class LobbyView extends JFrame {
    private JLabel lblTitle, lblUserInfo;
    private JButton btnLogout;
    private JTable tblPlayers;
    private DefaultTableModel tableModel;
    private LobbyController lobbyController;


    public LobbyView(String username) {
        setTitle("Lobby");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        
        // header
        JPanel headerPanel = new JPanel(new BorderLayout());
        lblTitle  = new JLabel("Penalty Game Lobby", SwingConstants.LEFT);
        lblUserInfo = new JLabel("Welcome: " + username);
        btnLogout = new JButton("Logout");

        JPanel userPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        userPanel.add(lblUserInfo);
        userPanel.add(btnLogout);
        headerPanel.add(lblTitle, BorderLayout.WEST);
        headerPanel.add(userPanel, BorderLayout.EAST);
        
        add(headerPanel, BorderLayout.NORTH);

        // table player

        String[] cols = {"Name", "Status", "Score", "Action"};
        tableModel = new DefaultTableModel(cols, 0);
        tblPlayers = new JTable(tableModel);

        tblPlayers.getColumn("Action").setCellRenderer(new ButtonRenderer());
        tblPlayers.getColumn("Action").setCellEditor(new ButtonEditor(new JCheckBox(), lobbyController));

        btnLogout.addActionListener(e -> this.lobbyController.handleLogout());

        add(new JScrollPane(tblPlayers), BorderLayout.CENTER);
    }



    public void addPlayer(String name, String status, int score) {
        tableModel.addRow(new Object[]{name, status, score, "Invite"});
    }


    class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
        }
        @Override
        public Component getTableCellRendererComponent(
                JTable table, 
                Object value,
                boolean isSelected, 
                boolean hasFocus,
                int row, 
                int column) {
            setText("Invite");
            return this;
        }
    }

    class ButtonEditor extends DefaultCellEditor {
        private JButton button;
        private int row;
        private LobbyController controller;

        public ButtonEditor(JCheckBox checkBox, LobbyController controller) {
            super(checkBox);
            this.controller = controller;
            button = new JButton("Invite");
            button.setOpaque(true);
            button.addActionListener(e -> fireEditingStopped());
        }

        @Override
        public Component getTableCellEditorComponent(
                JTable table, 
                Object value,
                boolean isSelected, 
                int row, 
                int column) {
            this.row = row;
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            String playerName = (String) tableModel.getValueAt(row, 0);
            controller.handleInvite(playerName);
            return "Invited";
        }
    }
    

   
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );
        pack();
    }
    // </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables

}