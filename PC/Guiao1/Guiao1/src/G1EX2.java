public class G1EX2 {
    public static void main(String[] args) throws InterruptedException {
         
        // Proteção simples para argumentos
        int N = args.length > 0 ? Integer.parseInt(args[0]) : 10;
        int I = args.length > 1 ? Integer.parseInt(args[1]) : 100;
        
        // CORREÇÃO 1: Criar o objeto partilhado UMA VEZ
        Counter contadorPartilhado = new Counter();

        Thread[] listaT = new Thread[N];
        
        for (int i = 0; i < N; i++){
            // CORREÇÃO 2: Usar a classe Incrementer e passar o contador
            listaT[i] = new Incrementer(I, contadorPartilhado);
            listaT[i].start();
        }

        for (int n = 0; n < N; n++){
            listaT[n].join();
        }

        // CORREÇÃO 3: Imprimir o valor do objeto que criámos
        System.out.println(contadorPartilhado.value());
    }
}

class Counter {
    private int c = 0;

    public void increment(){
        c++;
    }
    public int value(){
        return c;
    }
}

class Incrementer extends Thread {
    final int I;
    final Counter c; // O contador onde vamos mexer

    Incrementer(int i, Counter c){
        this.I = i; // CORREÇÃO 4: Atribuir o parâmetro 'i' ao atributo 'I'
        this.c = c;
    }

    public void run(){
        for (int i = 0; i < I; i++){
            c.increment();
        }
    }
}

//EX3
/*Neste exercicio, variamos o valor das threads (N) e o numero de incrementos (I) com o objetivo de comparar o valor final
obtido no contador
Com valores pequenos (neste caso, com N = 10 e I = 100), o resultado foi 1000 (10x100) pois as threads sao tao rapidas que
acabam o trabalho antes de se cruzarem
Com valores grandes, (neste caso, com N = 10 e I = 100000), o resultado é sempre menor do que o esperado e sempre diferente.
PQ?
Quando duas threads tentam incrementar ao mesmo tempo, podem ambas ler o valor(ex: 50) antes de qualquer uma delas escrever.
Ambas somam 1 (51) e ambas escrevem 51 na memoria. Logo, fizemos dois incrementos mas o contador so subiu 1. O outro 
incremento foi perdido ou "Sobrescrito".
LOGO, sem mecanismos de sincronização, o acesso concorrente a estado partilhado leva a resultados inconsistentes e imprevisiveis.
*/