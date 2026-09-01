import java.sql.*;
public class Q {
  public static void main(String[] a) throws Exception {
    Connection c = DriverManager.getConnection("jdbc:mysql://localhost:3307/pointofsale", "root", "");
    Statement s = c.createStatement();
    ResultSet r = s.executeQuery("SELECT kode_produk, nama_produk FROM produk WHERE kode_produk = '000076' OR kode_produk = '76' OR kode_produk LIKE '%76'");
    boolean any = false;
    while (r.next()) { any = true; System.out.println(r.getString(1) + "\t" + r.getString(2)); }
    if (!any) System.out.println("(no rows)");
    r.close(); s.close(); c.close();
  }
}