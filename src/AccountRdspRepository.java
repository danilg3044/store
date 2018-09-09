package com.ifdsgroup.rdsp.account.dao;


import com.ifdsgroup.rdsp.account.dto.model.enums.ElectionTypeCode;
import com.ifdsgroup.rdsp.common.error.RdspNoDataException;
import com.ifdsgroup.rdsp.common.utils.CommonConstants;
import com.ifdsgroup.rdsp.domain.Account;
import com.ifdsgroup.rdsp.domain.AccountElectionHistory;
import com.ifdsgroup.rdsp.domain.AccountStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * AccountRdspService will provide EXT JS screen required RDSP account details
 *
 *  @author      Usha S
 *  @version     1.0
 *  @since       15/May/2018
 */
@Repository
public class AccountRdspRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(AccountRdspRepository.class);

    static final String GET_ACCOUNT_BY_NUMBER = "SELECT * FROM account WHERE account_number = ?";
    static final String GET_ACCOUNTS_BY_NUMBERS = "SELECT * FROM account WHERE account_number in (:ids)";
    static final String GET_ACCOUNT_ELECTIONS =
        "SELECT * FROM account_election_history WHERE account_number = ? AND is_deleted is not true ";
    static final String UPDATE_ACCOUNT = "UPDATE account" +
        "   SET request_grant_ind=?, request_bond_ind=?, transfer_in_account_ind=?, esdc_reportable_ind=?, rdsp_inception_date=?," +
        "   dtc_election_status_code = ?, sdsp_status_code = ?, specified_year_status_code = ?" +
        "	WHERE account_number=?";
    static final String INSERT_ELECTION = "INSERT INTO account_election_history(" +
        "id, account_number, election_type_code, election_status_code, certification_date," +
        " transaction_date, reporting_status_code, period_start_date, period_end_date, effective_from_date, is_deleted)" +
        " VALUES (nextval('account_election_history_id_seq'), ?, ?, ?, ?, ?, ?, ?, ?, ?, FALSE)";
    static final String UPDATE_ELECTION = "UPDATE account_election_history " +
        "SET election_type_code = ?, election_status_code = ?, certification_date = ?," +
        " transaction_date = ?, reporting_status_code = ?, period_start_date = ?, period_end_date = ?," +
        " effective_from_date = ?, is_deleted = ? " +
        " WHERE id = ?";
    static final String GET_BENEFICIARY_BIRTH_DATE = "SELECT date_of_birth FROM entity WHERE entity_id = " +
        " ANY (SELECT entity_id FROM account_entity_role WHERE account_number = ? AND account_role_code = '79')";
    
    static final String UPDATE_DTC_ELIGIBILTY_HISTORY = "UPDATE account_election_history SET period_start_date= ?, effective_from_date = ? WHERE election_type_code = ?"
    		+ " AND account_number = ? AND period_end_date is null";

    @Value( "${keyholder.id.key.name}" )
    String idKeyName = "id";

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    AccountRdspRowMapper accountRdspRowMapper;

    @Autowired
    AccountRdspElectionRowMapper accountRdspElectionRowMapper;

    boolean notInSync(Long number) {
    	return number == null || getAccount2(Math.abs(number)) != null;
    }
    
    static class Counter {
        static public AtomicLong value = new AtomicLong();
    }
    
    static Supplier<Long> generator() {
        return () -> Counter.value.addAndGet(1) > 8000 ? null : 26811001L;
    }
    
    BufferedWriter writer = null;
    
    void loadAccounts(int threads) {
    	List<Long> accounts = Collections.synchronizedList(new ArrayList<>());
    	//int threads = Runtime.getRuntime().availableProcessors();
    	Counter.value.set(0L);
    	long start_time = System.nanoTime();

    	try {
			new ForkJoinPool(threads).submit(() -> Stream.generate(generator()).parallel().filter(this::notInSync).anyMatch(number -> {
				if (number == null) {
					return true;
				}
				
				//Account acc = getAccount(number);
				//accounts.add(acc.getAccountNumber());
				return false;
			})).get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
    	
    	long end_time = System.nanoTime();
    	double difference = (end_time - start_time) / 1e6;
    	
    	try {
			writer.write(String.format("Time for %d is [%d]\n", threads, (long) difference));
		} catch (IOException e) {
		}
    }
    
    ////////////////////////////////////////////////////////////////////////////////
    
    @Value("${spring.datasource.url}")
    private String url;

    @Value("${spring.datasource.username}")
    private String user;
    
    @Value("${spring.datasource.password}")
    private String password;
    
	public Connection connect() throws SQLException {
		//String url = jdbcTemplate.getDataSource().getConnection().getMetaData().getURL();
		//String user = jdbcTemplate.getDataSource().getConnection().getMetaData().getUserName();
		//String url = jdbcTemplate.getDataSource().getConnection().getMetaData().get();
        return DriverManager.getConnection(url, user, password);
    }
	
	public long getAccountRaw() {
        String SQL = "SELECT * FROM account WHERE account_number=26811001";
        long count = 0;
        long start_time = System.nanoTime();

        try (Connection conn = connect()) {
        	for(int i = 0; i <  100000; ++i) {
	        	try(Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(SQL)) {
		            rs.next();
		            count = rs.getLong(1);
	        	}
        	}
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
        
        long end_time = System.nanoTime();
    	double difference = (end_time - start_time) / 1e6;
    	System.out.printf("Time1  is %d\n", (long) difference);
        return count;
    }
	
	///////////////////////////////////////////////////////////////////////////////
	
    public Account getAccount(long accountNumber) {
    	Account account = null;
    	getAccountRaw();
    	try {
			writer = new BufferedWriter(new FileWriter("d:\\Work\\IFDS\\Log.txt"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	long start_time = System.nanoTime();
  
    	for(int i = 0; i <  2000; ++i) {
    		account = getAccount2(accountNumber);
    	}
    	
    	long end_time = System.nanoTime();
    	double difference = (end_time - start_time) / 1e6;
    	System.err.printf("Time2  is %d\n", (long) difference);
    	/*loadAccounts(1);
       	loadAccounts(2);
    	loadAccounts(4);
    	loadAccounts(8);*/

    	try {
			writer.close();
		} catch (IOException e) {
		}
        return account;
    }

    public Account getAccount2(long accountNumber) {
    	Account account = null;
        try {
            account = jdbcTemplate.queryForObject(GET_ACCOUNT_BY_NUMBER, new Object[]{accountNumber}, accountRdspRowMapper);
            if (account != null) {
                //getAccountElections(accountNumber, account);
            }
        } catch (EmptyResultDataAccessException e) {
            // Just log and return null
            //LOGGER.info("No account found for the number: " + accountNumber);
        }
        return account;
    }