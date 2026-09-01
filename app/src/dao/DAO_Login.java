package dao;

import Main.Menu_Utama;
import Main.user;
import view.Form_Login_old;
import config.Koneksi;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import model.Model_login;
import org.mindrot.jbcrypt.BCrypt;
import service.Service_Login;

public class DAO_Login implements Service_Login {

    private static final String GENERIC_AUTH_FAIL = "Incorrect username or password";
    private Connection conn;

    public DAO_Login() throws ClassNotFoundException {
        conn = Koneksi.getConnection();
    }

    @Override
    public void prosesLogin(Model_login mod_login) {
        PreparedStatement st = null;
        ResultSet rs = null;
        String sql = "SELECT * FROM users WHERE username_user = ?";

        try {
            st = conn.prepareStatement(sql);
            st.setString(1, mod_login.getUsername());
            rs = st.executeQuery();

            if (!rs.next()) {
                failLogin();
                return;
            }

            String stored = rs.getString("password_user");
            String entered = mod_login.getPass_user();
            boolean passwordOk = false;
            boolean needsUpgrade = false;

            if (stored != null && stored.startsWith("$2")) {
                passwordOk = BCrypt.checkpw(entered, stored);
            } else {
                // TEMPORARY: plaintext passwords from before auth migration.
                // Compare as plaintext once, then re-hash. Remove this branch
                // once every user has logged in at least once.
                if (stored != null && stored.equals(entered)) {
                    passwordOk = true;
                    needsUpgrade = true;
                }
            }

            if (!passwordOk) {
                failLogin();
                return;
            }

            if (!"AKTIF".equalsIgnoreCase(rs.getString("status_user"))) {
                failLogin();
                return;
            }

            if (needsUpgrade) {
                upgradePasswordHash(rs.getString("user_Id"), entered);
            }

            String Nama = rs.getString("nama_user");
            String Level2 = rs.getString("level_user");
            user.setId(rs.getString("user_Id"));
            user.setNama(Nama);
            user.setUsername(rs.getString("username_user"));
            user.setJenisUser(Level2);

            Menu_Utama menu = new Menu_Utama(Nama, Level2);
            menu.setVisible(true);
            menu.revalidate();

            Form_Login_old lg = new Form_Login_old();
            lg.tutup = true;

        } catch (SQLException ex) {
            Logger.getLogger(DAO_Login.class.getName()).log(Level.SEVERE, null, ex);
            failLogin();
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(DAO_Login.class.getName()).log(Level.SEVERE, null, ex);
            failLogin();
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException ex) {
                    Logger.getLogger(DAO_Login.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (st != null) {
                try {
                    st.close();
                } catch (SQLException ex) {
                    Logger.getLogger(DAO_Login.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    private void failLogin() {
        JOptionPane.showMessageDialog(null, GENERIC_AUTH_FAIL, "Message", JOptionPane.INFORMATION_MESSAGE);
        try {
            Form_Login_old lg = new Form_Login_old();
            lg.tutup = false;
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(DAO_Login.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void upgradePasswordHash(String userId, String plainPassword) {
        String hash = BCrypt.hashpw(plainPassword, BCrypt.gensalt(10));
        try (PreparedStatement ups = conn.prepareStatement(
                "UPDATE users SET password_user = ? WHERE user_Id = ?")) {
            ups.setString(1, hash);
            ups.setString(2, userId);
            ups.executeUpdate();
        } catch (SQLException ex) {
            Logger.getLogger(DAO_Login.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
