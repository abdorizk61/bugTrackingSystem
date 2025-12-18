/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JDialog.java to edit this template
 */
package ui.projectmanager;

import dao.BugDAO;
import dao.UserDAO;
import models.Bug;
import models.Role;
import models.User;
import services.EmailService;
import javax.swing.*;
import java.util.List;

/**
 * Assign Bug Form - Dialog for PM to assign/re-assign a bug to a developer
 * @author Team
 */
public class AssignBugForm extends javax.swing.JDialog {

    private final Bug currentBug;
    private final User projectManager;
    private boolean assignmentCompleted = false;

    // UI Components
    private javax.swing.JLabel lblBugTitle;
    private javax.swing.JLabel lblSelectDev;
    private javax.swing.JComboBox<User> comboDevelopers;
    private javax.swing.JButton btnAssign;
    private javax.swing.JButton btnCancel;

    /**
     * Creates new form AssignBugForm
     * @param parent The parent frame
     * @param modal Modal setting
     * @param bug The bug being assigned
     * @param pm The logged-in Project Manager
     */
    public AssignBugForm(java.awt.Frame parent, boolean modal, Bug bug, User pm) {
        super(parent, modal);
        this.currentBug = bug;
        this.projectManager = pm;
        
        initComponents();
        setupCustomUI();
        loadDevelopers();
        
        setLocationRelativeTo(parent);
    }

    private void setupCustomUI() {
        if (currentBug != null) {
            lblBugTitle.setText("Assigning Bug #" + currentBug.getId() + ": " + currentBug.getTitle());
        }
    }

    /**
     * Loads developers into the ComboBox
     */
    private void loadDevelopers() {
        UserDAO userDAO = new UserDAO();
        List<User> developers = userDAO.findByRole(Role.DEVELOPER);
        
        DefaultComboBoxModel<User> model = new DefaultComboBoxModel<>();
        for (User dev : developers) {
            model.addElement(dev);
        }
        comboDevelopers.setModel(model);

        // Custom renderer to show names nicely
        comboDevelopers.setRenderer(new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof User) {
                    User u = (User) value;
                    setText(u.getFullName() + " (" + u.getUsername() + ")");
                }
                return this;
            }
        });
    }

    /**
     * Initialize UI components (Layout & Elements)
     */
    private void initComponents() {
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Assign Bug to Developer");

        lblBugTitle = new javax.swing.JLabel();
        lblBugTitle.setFont(new java.awt.Font("Segoe UI", 1, 14)); 
        lblBugTitle.setText("Bug Title Here");

        lblSelectDev = new javax.swing.JLabel();
        lblSelectDev.setText("Select Developer:");

        comboDevelopers = new javax.swing.JComboBox<>();

        btnAssign = new javax.swing.JButton();
        btnAssign.setText("Assign & Notify");
        btnAssign.setBackground(new java.awt.Color(40, 167, 69)); // Green
        btnAssign.setForeground(java.awt.Color.WHITE);
        btnAssign.addActionListener(this::btnAssignActionPerformed);

        btnCancel = new javax.swing.JButton();
        btnCancel.setText("Cancel");
        btnCancel.addActionListener(e -> dispose());

        // Layout Setup (GroupLayout)
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(lblBugTitle, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblSelectDev)
                    .addComponent(comboDevelopers, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(btnAssign, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 20, Short.MAX_VALUE)
                        .addComponent(btnCancel, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(25, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(lblBugTitle)
                .addGap(18, 18, 18)
                .addComponent(lblSelectDev)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(comboDevelopers, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(25, 25, 25)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnAssign, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnCancel, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(25, Short.MAX_VALUE))
        );

        pack();
    }

    /**
     * Action when "Assign" button is clicked
     */
    private void btnAssignActionPerformed(java.awt.event.ActionEvent evt) {
        User selectedDev = (User) comboDevelopers.getSelectedItem();

        if (selectedDev == null) {
            JOptionPane.showMessageDialog(this, "Please select a developer first.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            // 1. Update Bug Data
            currentBug.setAssignee(selectedDev);
            // Optional: You might want to change status to ASSIGNED or OPEN here if needed
            // currentBug.setStatus(models.BugStatus.OPEN); 
            
            // 2. Save Changes to Database/File
            BugDAO bugDAO = new BugDAO();
            // Note: BugDAO should have an 'update' or 'save' method that handles existing bugs
            bugDAO.updateBug(currentBug); // Assuming updateBug exists, or use save()

            // ============================================================
            // START: Notification System Integration (PM to Developer)
            // ============================================================
            try {
                EmailService emailService = new EmailService();
                
                String senderId = String.valueOf(projectManager.getId());
                String recipientId = String.valueOf(selectedDev.getId());
                
                String subject = "Bug Assignment Update: #" + currentBug.getId();
                String body = "Dear " + selectedDev.getFullName() + ",\n\n" +
                              "The Project Manager (" + projectManager.getFullName() + ") has assigned a bug to you.\n" +
                              "Bug Title: " + currentBug.getTitle() + "\n" +
                              "Priority: " + currentBug.getPriority() + "\n\n" +
                              "Please check your dashboard.";

                // Send the notification
                emailService.sendEmail(recipientId, senderId, subject, body);
                
                System.out.println(">> PM Notification sent to Dev: " + selectedDev.getUsername());
                
            } catch (Exception ex) {
                System.err.println(">> Warning: Notification failed: " + ex.getMessage());
            }
            // ============================================================
            // END: Notification System Integration
            // ============================================================

            JOptionPane.showMessageDialog(this, 
                    "Bug assigned to " + selectedDev.getUsername() + " successfully!", 
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            
            this.assignmentCompleted = true;
            this.dispose();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error assigning bug: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    public boolean isAssignmentCompleted() {
        return assignmentCompleted;
    }
}
