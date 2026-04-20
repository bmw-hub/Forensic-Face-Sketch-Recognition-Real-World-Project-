package thirdeye.v2;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class Login_screenController implements Initializable {

    @FXML
    private Text ip_add;
    @FXML
    private Text mac_add;
    @FXML
    private Text net_add;
    @FXML
    private TextField email;
    @FXML
    private TextField password;
    @FXML
    private TextField otp;
    @FXML
    private Text error;
    @FXML
    private Button send;
    @FXML
    private Button verify;
    @FXML
    private Text hide;
    @FXML
    private Text loginerror;
    @FXML
    private Text hide1;
    @FXML
    private Text loginmsg;
    @FXML
    private AnchorPane login_page;
    
    // Database connection
    private Connection conn = null;
    private PreparedStatement preparedStatement = null;
    private ResultSet resultSet = null;
    
    // Email credentials - MOVE THESE TO A CONFIG FILE FOR SECURITY
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";
    private static final String EMAIL_FROM = "srdisposals@gmail.com";
    private static final String EMAIL_PASSWORD = "uxolxpfrxjyrbkqd"; // USE ENVIRONMENT VARIABLE IN PRODUCTION
    
    public Login_screenController() {
        conn = connectdb.ConnectDB();
    }

    /**
     * Thread to fetch and display IP and MAC address
     */
    class ip_mac extends Thread {
        @Override 
        public void run() {
            try {
                InetAddress ip = InetAddress.getLocalHost();
                ip_add.setText(ip.getHostAddress());
                
                NetworkInterface network = NetworkInterface.getByInetAddress(ip);
                net_add.setText("[ " + network.toString() + " ]");
                                        
                byte[] mac = network.getHardwareAddress();
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < mac.length; i++) {
                    sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));        
                }             
                mac_add.setText(sb.toString()); 
                    
            } catch (UnknownHostException | SocketException e) {
                Logger.getLogger(Login_screenController.class.getName()).log(Level.SEVERE, "Error fetching IP/MAC", e);
            }
        }
    }
    
    /**
     * Generate a random 4-digit OTP
     */
    public void generateOTP() {
        Random rd = new Random();
        // Generate 4-digit OTP (1000-9999)
        int otpValue = 1000 + rd.nextInt(9000);
        String generatedOtp = String.valueOf(otpValue);
        hide.setText(generatedOtp);
        System.out.println("DEBUG: Generated OTP is: " + generatedOtp);
    }
    
    /**
     * Send OTP via email using Gmail SMTP
     */
    public void sendOTP() {
        // Configure mail server properties
        Properties props = new Properties();
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");
        props.put("mail.smtp.ssl.trust", SMTP_HOST);
        
        try {
            // Create session with authentication
            Session session = Session.getInstance(props, new javax.mail.Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(EMAIL_FROM, EMAIL_PASSWORD);
                }
            });
            
            // Enable debug mode for troubleshooting
            session.setDebug(false); // Set to true for debugging
            
            // Create email message
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(EMAIL_FROM));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(email.getText().trim()));
            message.setSubject("OTP For your ThirdEye Account");
            
            // Email body
            String emailBody = "Dear User,\n\n" +
                             "Your OTP for ThirdEye login is: " + hide.getText() + "\n\n" +
                             "This OTP is valid for this session only.\n" +
                             "Do not share this OTP with anyone.\n\n" +
                             "Regards,\n" +
                             "ThirdEye Team";
            message.setText(emailBody);
            
            // Send the email
            Transport.send(message);
            error.setText("OTP has been sent to your email");
            System.out.println("OTP email sent successfully to: " + email.getText());
            
        } catch (MessagingException e) {
            error.setText("Failed to send OTP. Check your connection.");
            System.err.println("Error sending email: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Validate user credentials against database
     * @return "Success" if login successful, "Error" otherwise
     */
    private String login() { 
        String status = "Error";
        String e_mail = email.getText().trim();
        String pass = password.getText().trim();
        
        // Validate input
        if (e_mail.isEmpty() || pass.isEmpty()) {
            error.setText("Email and Password cannot be empty");
            return status;
        }
        
        // Validate email format
        if (!e_mail.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            error.setText("Invalid email format");
            return status;
        }
        
        try {
            // Connect to database
            if (conn == null || conn.isClosed()) {
                conn = DriverManager.getConnection("jdbc:sqlite:login.sqlite");
                System.out.println("Connection to SQLite established.");
            }
            
            // Prepare SQL query
            String sql = "SELECT * FROM login_data WHERE email = ? AND password = ?";
            preparedStatement = conn.prepareStatement(sql);
            preparedStatement.setString(1, e_mail);
            preparedStatement.setString(2, pass);
            
            // Execute query
            resultSet = preparedStatement.executeQuery();
            
            if (resultSet.next()) {
                System.out.println("Login Successful for: " + e_mail);
                loginerror.setText("successful");
                status = "Success";
            } else {
                System.out.println("Login Failed: Incorrect credentials");
                error.setText("Invalid email or password");
            }
            
        } catch (SQLException ex) {
            System.err.println("Database error: " + ex.getMessage());
            error.setText("Database connection error");
            ex.printStackTrace();
        } finally {
            // Close resources
            try {
                if (resultSet != null) resultSet.close();
                if (preparedStatement != null) preparedStatement.close();
            } catch (SQLException ex) {
                System.err.println("Error closing resources: " + ex.getMessage());
            }
        }
        
        return status;
    }
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Start thread to fetch IP and MAC address
        new ip_mac().start();
        
        // Initialize UI state
        otp.setVisible(false);
        verify.setVisible(false);
    }
    
    /**
     * Handle button actions for Send OTP and Verify OTP
     */
    @FXML
    private void handleButtonAction(ActionEvent event) {
        // Clear previous error messages
        error.setText("");
        
        if (event.getSource() == send) {
            // Validate login credentials
            String loginStatus = login();
            
            if (loginStatus.equals("Success") && loginerror.getText().equals(loginmsg.getText())) {
                // Generate and send OTP
                generateOTP();
                sendOTP();
                
                // Update UI
                send.setVisible(false);
                otp.setVisible(true);
                verify.setVisible(true);
                email.setEditable(false);
                password.setEditable(false);
                
            } else {
                error.setText("Please enter correct email and password");
            }
            
        } else if (event.getSource() == verify) {
            // Verify OTP
            String enteredOTP = otp.getText().trim();
            String generatedOTP = hide.getText();
            
            if (enteredOTP.isEmpty()) {
                error.setText("Please enter the OTP");
                return;
            }
            
            if (generatedOTP.equals(enteredOTP)) {
                // OTP verified successfully - open dashboard
                try {
                    FXMLLoader fxmlLoader = new FXMLLoader();
                    fxmlLoader.setLocation(getClass().getResource("menu.fxml"));
                    Scene scene = new Scene(fxmlLoader.load());
                    Stage stage = new Stage();
                    stage.setTitle("ThirdEye - Dashboard");
                    stage.setScene(scene);
                    stage.setResizable(false);
                    stage.show();
                    
                    // Close login window
                    ((Node)(event.getSource())).getScene().getWindow().hide();
                    
                    System.out.println("Login successful - Dashboard opened");
                    
                } catch (IOException e) {
                    error.setText("Failed to open dashboard");
                    Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Failed to create dashboard window", e);
                }
            } else {
                error.setText("Incorrect OTP. Please try again.");
                otp.clear();
            }
        }
    }
    
    /**
     * Clean up resources when controller is destroyed
     */
    public void cleanup() {
        try {
            if (resultSet != null) resultSet.close();
            if (preparedStatement != null) preparedStatement.close();
            if (conn != null) conn.close();
        } catch (SQLException e) {
            System.err.println("Error during cleanup: " + e.getMessage());
        }
    }
}