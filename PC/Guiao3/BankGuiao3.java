import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class BankGuiao3 {

    private static class Account {
        private int balance;
        private Lock lock = new ReentrantLock();

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

    private Map<Integer, Account> map = new HashMap<Integer, Account>();
    private int nextId = 0;
    //antes, no guiao2, utilizavamos o synchronized. Agora neste guiao 3, vamos utilizaz Locks de biblioteca, em que nao tem de estar associados a um metodo
    //ou a um objeto. Assim, podemos ter mais controlo sobre o bloqueio, e podemos bloquear apenas o que queremos, e nao o metodo inteiro
    private Lock lock = new ReentrantLock();

    // create account and return account id
    public int createAccount(int balance) {
        Account c = new Account(balance);
        int id;

        lock.lock();

        try{ //utilizamos o lock do banco para garantir que apenas uma thread pode criar uma conta ao mesmo tempo, evitando que duas threads criem contas
            //ao mesmo tempo com o mesmo id.
            id = nextId;
            nextId += 1;
            map.put(id, c);
        } finally {
            lock.unlock(); //garantimos que o lock é sempre desbloqueado, mesmo que haja uma exceção, para evitar deadlocks
        }
        return id;
    }

    // close account and return balance, or 0 if no such account
    public int closeAccount(int id) {
        Account c;;
        lock.lock();
        try{
            c = map.remove(id);
            //se a conta nao existir, nao tem necessidade de bloquear a conta (pq n existe), por isso podemos desbloquear o lock do banco e retornar 0
            if(c == null)
                return 0;
            c.lock.lock(); //se a conta existir, bloqueamos ela para garantir que nenhuma outra thread possa aceder a ela enquanto estamos a fechar a conta e 
                          // e libertamos a parte que modifica para que outras threads possam aceder a outras contas enquanto estamos a fechar esta conta
        } finally{
            lock.unlock();
        }
        try{
            return c.balance();//assim podemos consultar uma conta especifica sem bloquear o banco inteiro, permitindo que outras
                               //threads possam aceder a outras contas enquanto estamos a consultar essa conta
        }  finally{
                c.lock.unlock();
            }
    }



    // account balance; 0 if no such account
    public int balance(int id) {
        Account c;
        lock.lock();

        try{
            c = map.get(id);
            if(c == null) //se a conta nao existir, nao tem necessidade de bloquear a conta (pq n existe), por isso podemos desbloquear o lock do banco e retornar 0
                return 0;
            c.lock.lock();
        } finally {
            lock.unlock();
        }
        
        try{
            return c.balance();
        } finally {
            c.lock.unlock();
        }
    }

    // deposit; fails if no such account
    public boolean deposit(int id, int value) {
        Account c;
        lock.lock();

        try{
            c = map.get(id);
            if(c == null)
                return false;
                c.lock.lock();
        } finally{
            lock.unlock();
        }

        try{
            return c.deposit(value);
        } finally{
            c.lock.unlock();
        }
    }

    // withdraw; fails if no such account or insufficient balance
    public boolean withdraw(int id, int value) {
        Account c;
        lock.lock();
        try{
            c = map.get(id);
            if(c == null)
                return false;
            c.lock.lock();
        } finally {
            lock.unlock();
        }
        try {
            return c.withdraw(value);
        } finally {
            c.lock.unlock();
        }
    }



    // transfer value between accounts;
    // fails if either account does not exist or insufficient balance
    //mais dificil...
    public boolean transfer(int from, int to, int value) {
        Account cfrom, cto;
        lock.lock();
        try{
            cfrom = map.get(from);
            cto = map.get(to);
            if(cfrom == null || cto == null)
                return false;
                //nesse caso, ao inves de fazer cfrom.lock.lock e cto.lock.lock, como sao duas contas, temos de bloquear as duas individualmente para que 
                //nao tenha deadlocks. Vamos fazer por ordem crescente de numeros de id
            if(from < to){
                cfrom.lock.lock();
                cto.lock.lock();
            } else {
                cto.lock.lock();
                cfrom.lock.lock();
            }   
        } finally {
                lock.unlock();
            }
        try{
            try{
                if(!cfrom.withdraw(value)){
                    return false;
                }
            } finally {
                cfrom.lock.unlock(); //depois de tentar o levantamento da conta de origem, desbloqueamos a conta
            }
            return cto.deposit(value);
        } finally {
            cto.lock.unlock(); //depois de tentar o deposito na conta de destino ou seja, no fim da transferencia, desbloqueamos a conta destino
        }
    }



    // sum of balances in set of accounts; 0 if some does not exist
    public int totalBalance(int[] ids) {
        //para nao alterar a lista original, criamos um clone para trabalhar dentro desta chamada de metodo
        ids = ids.clone();
        Arrays.sort(ids);

        int total = 0;

        //lista auxiliar para guardar as contas temporariamente
        Account[] arrAcc = new Account[ids.length];

        lock.lock();
        try{
            //guardamos todas as listas no array para q possamos mexer nelas independente
            for(int i = 0; i < ids.length; i++){
                arrAcc[i] = map.get(ids[i]);
                if(arrAcc[i] == null) return 0;
            }
            //vamos agora dar lock para cada uma das contas que vamos mexer
            for(Account c : arrAcc){
                c.lock.lock();
            }
        } finally{
            lock.unlock();
        }

        int i = 0;
        //vamos agr consultar o saldo de cada conta e a medida em q consultamos o saldo, vamos desbloqueando a conta p q outras threads possam ja acede-la
        try{
            for(Account c : arrAcc){
                total += c.balance();
                c.lock.unlock();
            }
            return total;
        } finally {
            //caso haja uma exceção, garantimos que todas as contas que ainda nao foram desbloqueadas, sejam desbloqueadas
            for(int j = i; j < arrAcc.length; j++){
                arrAcc[j].lock.unlock();
            }
        }
    }
}
