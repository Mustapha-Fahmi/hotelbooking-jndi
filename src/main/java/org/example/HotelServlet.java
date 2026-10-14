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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@WebServlet("/hotels")
public class HotelServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("text/html;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        out.println("<html><body>");
        out.println("<h1>Available hotels</h1>");

        try {
            Context ctx = new InitialContext();
            DataSource ds = (DataSource) ctx.lookup("java:comp/env/jdbc/HotelDB");
            try (Connection con = ds.getConnection()) {
                initTable(con);

                try (Statement st = con.createStatement();
                     ResultSet rs = st.executeQuery("SELECT ID, NAME, CITY FROM HOTEL ORDER BY ID")) {

                    out.println("<table border='1' cellpadding='6'>");
                    out.println("<tr><th>ID</th><th>Name</th><th>City</th></tr>");
                    while (rs.next()) {
                        out.println("<tr><td>" + rs.getInt("ID") + "</td><td>"
                                + rs.getString("NAME") + "</td><td>"
                                + rs.getString("CITY") + "</td></tr>");
                    }
                    out.println("</table>");
                }
            }
        } catch (NamingException e) {
            // JNDI problem: the name was not found
            out.println("<p style='color:red'>JNDI error: " + e + "</p>");
            e.printStackTrace();
        } catch (SQLException e) {
            // JDBC / pool problem
            out.println("<p style='color:red'>SQL error: " + e + "</p>");
            e.printStackTrace();
        }

        out.println("</body></html>");
    }

    private void initTable(Connection con) throws SQLException {
        try (Statement st = con.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS HOTEL ("
                    + "ID INT PRIMARY KEY, "
                    + "NAME VARCHAR(100), "
                    + "CITY VARCHAR(100))");

            try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM HOTEL")) {
                rs.next();
                if (rs.getInt(1) == 0) {
                    st.executeUpdate("INSERT INTO HOTEL VALUES (1, 'Atlas Palace', 'Marrakech')");
                    st.executeUpdate("INSERT INTO HOTEL VALUES (2, 'Sea View', 'Casablanca')");
                }
            }
        }
    }
}