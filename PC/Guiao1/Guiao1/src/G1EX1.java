public class G1EX1 {
    
    public static void main(String[] args) throws InterruptedException{
        
        final int N = Integer.parseInt(args[0]); // numero de threads
        final int I = Integer.parseInt(args[1]); // ate que numero cada thread vai imprimir
        
        // Criamos um array de threads
        Thread[] listaT = new Thread[N];
        
        // Este ciclo inicializa as threads
        for (int i = 0; i < N; i++){
            listaT[i] = new Printer(I);
            listaT[i].start();
        }

        // Este ciclo aguarda que as threads acabem e se juntem (fazemos isto para que o programa depois siga normalmente,
        //neste caso nao é muito relevante porque depois disso nao ha mais codigo)
        for (int n = 0; n < N; n++){
            listaT[n].join();                   // o join espera que as threads acabem
        }
    }
}

class Printer extends Thread {

    // ate que numero cada Thread vai imprimir
    final int I;

    Printer(int I) {
        this.I = I;
    }

    // metodo run (obrigatorio), quando a thread for ativada ira executar esta funcao
    @Override
    public void run(){
        for (int i = 0; i < I; i++){
            System.out.println(i);
        }
    }
}