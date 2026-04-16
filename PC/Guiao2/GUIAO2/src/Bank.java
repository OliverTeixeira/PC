public class Bank {

    private static class Account {
        private int balance;
        Account(int balance) { this.balance = balance; }
        int balance() { return balance; }
        boolean deposit(int value) {
            balance += value;
            return true;
        }
        boolean withdraw(int value) {
            if (value > balance)
                return false;
            balance -= value;
            return true;
        }
    }

    // Bank slots and vector of accounts
    private int slots;
    private Account[] av; 

    public Bank(int n) {
        slots=n;
        av=new Account[slots];
        for (int i=0; i<slots; i++) av[i]=new Account(0);
    }

    // Account balance
    public synchronized int balance(int id) {
        if (id < 0 || id >= slots)
            return 0;
        return av[id].balance();
    }

    // Deposit
    public synchronized boolean deposit(int id, int value) {
        if (id < 0 || id >= slots)
            return false;
        return av[id].deposit(value);
    }

    // Withdraw; fails if no such account or insufficient balance
    public synchronized boolean withdraw(int id, int value) {
        if (id < 0 || id >= slots)
            return false;
        return av[id].withdraw(value);
    }



    public boolean transfer(int idFrom, int idTo, int amount){   
        if (!validateId(idFrom) || !validateId(idTo))               
            return false;                                       
        Account aFrom = av[idFrom];
        Account aTo = av[idTo];                                  
                                                                
        if (idFrom < idTo) {
            synchronized (aFrom) {                                   
                synchronized (aTo) {                                 
                    if (!aFrom.withdraw(amount))                     
                        return false;                                
                    return aTo.deposit(amount);                      
                }                                                   
            }   
        } else {
            synchronized (aTo) {                                   
                synchronized (aFrom) {                                 
                    if (!aFrom.withdraw(amount))                     
                        return false;                                
                    return aTo.deposit(amount);                      
                }                                                   
            }   
        } 
    }          

    public int totalBalance(){
        int sum = 0;
        for (Account c: av){
            sum += c.balance();
        }
        return sum;
    }
     public boolean validateId (int id){
        return id > 0 && id <= slots;
    }


}


