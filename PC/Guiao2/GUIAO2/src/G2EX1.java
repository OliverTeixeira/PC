// o objetivo deste guiao é resolver o problema que foi falado no EX3 do primeiro guiao
/* 
e para resolver este problema, adicionamos Locks que impedem que duas threads acedam
a uma secçao de codigo ao mesmo tempo, neste caso, onde se incrementa o contador.
*/
/*na classe Bank, nos metodos balance, deposit e withdraw, adicionamos o synchronized */
public class G2EX1 {
    public static void main(String[] args) throws InterruptedException{
        
        final int N = Integer.parseInt(args[0]); 
        final int I = Integer.parseInt(args[1]); 
        Counter counter = new Counter();         
        
        
        Thread[] listaT = new Thread[N];
        
        for (int i = 0; i < N; i++){
            listaT[i] = new Incrementer(I, counter);
            listaT[i].start();
        }

        for (int n = 0; n < N; n++){
            listaT[n].join();                   
        }

        System.out.println(counter.value());
    }
}

class Counter {
    
    private int c;

    // Acrescentamos a keyword "synchronized" neste método previne que diferentes threads acedam às zonas de secção crítica, ou seja, garante exclusão mútua 
    // Assim vamos previnir os erros que ocorriam na versão do Guião anterior (Ex 2)
    synchronized public void increment(){
        c++;
    }

    // A keyword "synchronized" utiliza o "lock" íntrinseco ao objeto quando ele opera na secção crítica
    //e retorna o unlock quando sai da mesma.
    public int value(){
        return c;
    }
}

class Incrementer extends Thread {

    final int I;
    private Counter c;

    Incrementer(int I, Counter c) {
        this.I = I;
        this.c = c;
    }

    public void run(){
        for (int i = 0; i < I; i++){
            c.increment();
        }
    }
}

