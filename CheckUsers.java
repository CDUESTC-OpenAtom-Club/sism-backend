import java.sql.*;

public class CheckUsers {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://175.24.139.148:8386/sism_db";
        String user = "sism_user";
        String password = "sism_pass_2024";
        
        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            String sql = "SELECT username, password_hash, real_name, is_active, " +
                        "LENGTH(password_hash) as hash_length, " +
                        "SUBSTRING(password_hash, 1, 10) as hash_prefix " +
                        "FROM app_user u " +
                        "JOIN org o ON u.org_id = o.org_id " +
                        "WHERE o.org_type = 'FUNCTIONAL_DEPT' OR o.org_type = 'FUNCTION_DEPT' " +
                        "ORDER BY username";
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                
                System.out.println("Functional Department Users:");
                System.out.println("=".repeat(80));
                
                while (rs.next()) {
                    System.out.printf("Username: %s%n", rs.getString("username"));
                    System.out.printf("Real Name: %s%n", rs.getString("real_name"));
                    System.out.printf("Is Active: %s%n", rs.getBoolean("is_active"));
                    System.out.printf("Password Hash Length: %d%n", rs.getInt("hash_length"));
                    System.out.printf("Password Hash Prefix: %s%n", rs.getString("hash_prefix"));
                    System.out.println("-".repeat(80));
                }
            }
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
