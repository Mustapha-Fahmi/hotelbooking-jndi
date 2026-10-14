package org.example;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@WebServlet("/hotels-prix")
public class HotelPrixServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("text/html;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        String city = req.getParameter("city");

        out.println("<html><body>");
        out.println("<h1>Hotels by city</h1>");

        if (city == null || city.isBlank()) {
            out.println("<p>Provide a city: <code>/hotels-prix?city=Marrakech</code></p>");
            out.println("</body></html>");
            return;
        }

        out.println("<h2>City: " + escape(city) + "</h2>");

        try {
            Context ctx = new InitialContext();
            DataSource ds = (DataSource) ctx.lookup("java:comp/env/jdbc/HotelDB");
            try (Connection con = ds.getConnection()) {
                initTable(con);
                String sql = "SELECT ID, NAME, CITY, PRIX FROM HOTEL "
                        + "WHERE UPPER(CITY) = UPPER(?) ORDER BY ID";

                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, city);
                    try (ResultSet rs = ps.executeQuery()) {
                        out.println("<table border='1' cellpadding='6'>");
                        out.println("<tr><th>ID</th><th>Name</th><th>City</th><th>Price</th></tr>");
                        boolean found = false;
                        while (rs.next()) {
                            found = true;
                            out.println("<tr><td>" + rs.getInt("ID") + "</td><td>"
                                    + escape(rs.getString("NAME")) + "</td><td>"
                                    + escape(rs.getString("CITY")) + "</td><td>"
                                    + rs.getDouble("PRIX") + "</td></tr>");
                        }
                        out.println("</table>");
                        if (!found) {
                            out.println("<p>No hotel found in this city.</p>");
                        }
                    }
                }
            }
        } catch (NamingException e) {
            out.println("<p style='color:red'>JNDI error: " + e + "</p>");
            e.printStackTrace();
        } catch (SQLException e) {
            out.println("<p style='color:red'>SQL error: " + e + "</p>");
            e.printStackTrace();
        }

        out.println("</body></html>");
    }

    private void initTable(Connection con) throws SQLException {
        try (Statement st = con.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS HOTEL ("
                    + "ID INT PRIMARY KEY, NAME VARCHAR(100), CITY VARCHAR(100))");

            // Add PRIX if the old servlet created the table first
            st.execute("ALTER TABLE HOTEL ADD COLUMN IF NOT EXISTS PRIX DOUBLE");

            // MERGE = insert or update by ID, so it works whoever ran first
            st.executeUpdate("MERGE INTO HOTEL (ID, NAME, CITY, PRIX) KEY(ID) VALUES (1, 'Atlas Palace', 'Marrakech', 850.0)");
            st.executeUpdate("MERGE INTO HOTEL (ID, NAME, CITY, PRIX) KEY(ID) VALUES (2, 'Sea View', 'Casablanca', 600.0)");
            st.executeUpdate("MERGE INTO HOTEL (ID, NAME, CITY, PRIX) KEY(ID) VALUES (3, 'Palmeraie Resort', 'Marrakech', 1200.0)");
        }
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
