package daniel;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class Main {

	class Account {
		private long number;

		public Account(long number) {
			this.number = number;
		}
		
		public long getNumber() {
			return number;
		}

		public void setNumber(long number) {
			this.number = number;
		}
	}
	
	private final String url = "jdbc:postgresql://localhost:5432/rdsp";
    private final String user = "rdsp";
    private final String password = "rdsppassword";
    
    Connection connection;
    ThreadLocal<WeakReference<Connection>> threadConnection = new ThreadLocal<>();
    List<Connection> connections = Collections.synchronizedList(new ArrayList<>());
    
	public Connection connect() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }
	
	public long getAccount() {
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
    	System.out.printf("Time  is %d\n", (long) difference);
        return count;
    }
	
	public Account getAccount(long number) {
        String SQL = "SELECT * FROM account WHERE account_number=" + number;
        Connection conn = null;
        WeakReference<Connection> wr = threadConnection.get();
        
        try {
			if(wr == null || wr.get() == null || wr.get().isClosed()) {
				conn = connect();
				connections.add(conn);
				wr = new WeakReference<Connection>(conn);
				threadConnection.set(wr);	
			}
			else {
				conn = wr.get();
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        try(Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(SQL)) {
            rs.next();
            number = rs.getLong(1);
    	}
    	catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
        
        return new Account(number);
    }
	
	public Long getAccount2(Long number) {
        String SQL = "SELECT * FROM account WHERE account_number=" + number;
        Connection conn = null;
        WeakReference<Connection> wr = threadConnection.get();
        
        try {
			if(wr == null || wr.get() == null || wr.get().isClosed()) {
				conn = connect();
				connections.add(conn);
				wr = new WeakReference<Connection>(conn);
				threadConnection.set(wr);	
			}
			else {
				conn =  wr.get();
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        try(Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(SQL)) {
            rs.next();
            number = rs.getLong(1);
    	}
    	catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
        
        return number;
    }
	
	//boolean notInSync(Account number) {
	boolean notInSync(Long number) {
    	return number == null || getAccount2(number) != 0;
    }
    
    static class Counter {
        static public AtomicLong value = new AtomicLong();
    }
    
    static Supplier<Long> generator() {
        return () -> Counter.value.addAndGet(1) > 100000 ? null : 26811001L;
    }
    
	void loadAccounts(int threads) {
    	List<Account> accounts = Collections.synchronizedList(new ArrayList<>());
    	//int threads = Runtime.getRuntime().availableProcessors();
    	Counter.value.set(0L);
    	long start_time = System.nanoTime();

    	try {
			//new ForkJoinPool(threads).submit(() -> Stream.generate(generator()).parallel().map(this::getAccount).filter(this::notInSync).anyMatch(number -> {
			new ForkJoinPool(threads).submit(() -> Stream.generate(generator()).parallel().filter(this::notInSync).anyMatch(number -> {
				if (number == null) {
					return true;
				}
				
				Account acc = getAccount(number);
				accounts.add(acc);
				return false;
			})).get();
		} catch (InterruptedException | ExecutionException e) {
		}
    	
    	long end_time = System.nanoTime();
    	double difference = (end_time - start_time) / 1e6;
    	System.out.printf("Time for %d is %d\n", threads, (long) difference);
    }
	
	void inCycle() {
		loadAccounts(1);
		connections.forEach(cnct -> {
			try {
				cnct.close();
			}
			catch (SQLException e) {
			}
		});
		connections.clear();
		
       	loadAccounts(2);
       	connections.forEach(cnct -> {
       		try {
				cnct.close();
			}
			catch (SQLException e) {
			}
		});
		connections.clear();
       	
    	loadAccounts(4);
    	connections.forEach(cnct -> {
    		try {
				cnct.close();
			}
			catch (SQLException e) {
			}
		});
		connections.clear();
    	
    	loadAccounts(8);
    	connections.forEach(cnct -> {
			try {
				cnct.close();
			}
			catch (SQLException e) {
			}
		});
		connections.clear();
	}
	
	public static void main(String[] args) {
		new Main().inCycle();//getAccount();
	}
}
