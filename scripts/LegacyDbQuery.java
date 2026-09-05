import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;

public class LegacyDbQuery {

  public static void main(String[] args) {
    String dbPath = args.length > 0 && !args[0].isBlank()
        ? args[0]
        : "/Users/deepeshgodara/Documents/petstore1.3.1_02/docker/data/petstoredb";

    String query = args.length > 1 && !args[1].isBlank()
        ? args[1]
        : "SELECT POID, POUSERID, POVALUE, POLOCALE FROM PURCHASEORDER";

    String url = "jdbc:hsqldb:file:" + dbPath + ";readonly=true;shutdown=true;hsqldb.lock_file=false";

    try (Connection conn = DriverManager.getConnection(url, "sa", "");
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(query)) {

      ResultSetMetaData meta = rs.getMetaData();
      int cols = meta.getColumnCount();

      // Print headers
      for (int i = 1; i <= cols; i++) {
        System.out.printf("%-20s ", meta.getColumnLabel(i));
      }
      System.out.println();
      for (int i = 1; i <= cols; i++) {
        System.out.print("-------------------- ");
      }
      System.out.println();

      int count = 0;
      while (rs.next()) {
        count++;
        for (int i = 1; i <= cols; i++) {
          Object val = rs.getObject(i);
          String str = val != null ? val.toString().trim() : "NULL";
          if (str.length() > 20) {
            str = str.substring(0, 17) + "...";
          }
          System.out.printf("%-20s ", str);
        }
        System.out.println();
      }
      System.out.println("\nTotal rows: " + count);

    } catch (Exception e) {
      System.err.println("SQL Error: " + e.getMessage());
      System.exit(1);
    }
  }
}
