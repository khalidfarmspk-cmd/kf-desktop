import java.sql.*;
public class Q2 {
  public static void main(String[] a) throws Exception {
    Connection c = DriverManager.getConnection("jdbc:mysql://localhost:3307/pointofsale", "root", "");
    Statement s = c.createStatement();
    ResultSet r = s.executeQuery("SELECT COUNT(*) FROM produk");
    if (r.next()) System.out.println("total produk: " + r.getInt(1));
    r.close();
    r = s.executeQuery("SELECT kode_produk, nama_produk FROM produk WHERE kode_produk LIKE '%76%' OR CAST(kode_produk AS CHAR) LIKE '%76%' LIMIT 20");
    while (r.next()) System.out.println(r.getString(1) + "\t" + r.getString(2));
    r.close(); s.close(); c.close();
  }
}