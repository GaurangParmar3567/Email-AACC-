// package com.example.mail.config;
// import javax.sql.DataSource;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.jdbc.core.JdbcTemplate;
// import org.springframework.jdbc.datasource.DriverManagerDataSource;

// @Configuration
// public class jdbcConfig {

//     @Bean(name = "iResolveDataSource")
//     public DataSource dataSource() {

//         DriverManagerDataSource ds = new DriverManagerDataSource();

//         ds.setDriverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver");

//         ds.setUrl(
//             "jdbc:sqlserver://localhost:1433;" +
//             "databaseName=SBI_IResolve;" +
//             "encrypt=false;" +
//             "trustServerCertificate=true;" +
//             "MultiSubnetFailover=true;" +
//             "ApplicationIntent=ReadWrite"
//         );

//         // ds.setUsername("Dems_Email");
//         // ds.setPassword("Dems_Email@123");

//         ds.setUsername("gaurang");
//         ds.setPassword("G@urav3567");

//         return ds;
//     }

//     @Bean(name = "iResolveJdbcTemplate")
//     public JdbcTemplate jdbcTemplate(DataSource iResolveDataSource) {
//         return new JdbcTemplate(iResolveDataSource);
//     }
// }
